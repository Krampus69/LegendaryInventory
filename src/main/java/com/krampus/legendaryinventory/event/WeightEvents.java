package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.MirrorPacket;
import com.krampus.legendaryinventory.net.WeightPacket;
import com.krampus.legendaryinventory.weight.CapacityBonus;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.LIAttributes;
import com.krampus.legendaryinventory.weight.WeightDirty;
import com.krampus.legendaryinventory.weight.WeightRules;
import com.krampus.legendaryinventory.weight.WeightTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.BitSet;
import java.util.Map;
import java.util.WeakHashMap;

public final class WeightEvents {

    public static final ResourceLocation OVERLOAD_ID =
        ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "overload");

    private static final int DELTA_LIMIT = 32;
    private static final double[] AIR_FACTOR = {0.0D, 1.80D, 1.40D, 1.10D, 0.0D};

    private static final Map<Player, Integer> LAST_SENT = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_TIER = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_CAPACITY = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_GENERATION = new WeakHashMap<>();
    private static final Map<Player, ItemStack[]> LAST_MAIN = new WeakHashMap<>();

    private WeightEvents() {}

    @EventBusSubscriber(modid = LegendaryInventory.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onAttributeModification(EntityAttributeModificationEvent event) {
            event.add(EntityType.PLAYER, LIAttributes.CARRY_CAPACITY);
        }
    }

    @EventBusSubscriber(modid = LegendaryInventory.MODID)
    public static final class GameBus {

        @SubscribeEvent
        public static void onTagsUpdated(TagsUpdatedEvent event) {
            WeightTable.rebuild();
        }

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) {
                WeightRules.sendTo(sp);
                watchInventory(sp);
            }
        }

        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) {
                watchInventory(sp);
            }
        }

        @SubscribeEvent
        public static void onContainerOpen(PlayerContainerEvent.Open event) {
            Player player = event.getEntity();
            if (!player.level().isClientSide()) {
                WeightDirty.watch(event.getContainer(), player);
            }
        }

        @SubscribeEvent
        public static void onContainerClose(PlayerContainerEvent.Close event) {
            if (!event.getEntity().level().isClientSide()) {
                WeightDirty.mark(event.getEntity());
            }
        }

        @SubscribeEvent
        public static void onToss(ItemTossEvent event) {
            if (!event.getPlayer().level().isClientSide()) {
                WeightDirty.mark(event.getPlayer());
            }
        }

        private static void watchInventory(ServerPlayer player) {
            WeightDirty.watch(player.inventoryMenu, player);
            WeightDirty.mark(player);
        }

        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            if (player.isCreative() || player.isSpectator()) {
                clear(player);
                MountEvents.clearPenalty(player);
                LAST_TIER.remove(player);
                LAST_CAPACITY.remove(player);
                return;
            }
            if (!WeightTable.isReady()) {
                return;
            }

            boolean dirty = WeightDirty.consume(player);
            ExtendedInventory ext = LIAttachments.extended(player);
            if (ext.hasChanges()) {
                dirty = true;
                if (ScrollRegistry.has(player.containerMenu)) {
                    BitSet changes = ext.takeChanges();
                    LINet.toPlayer(player, changes.cardinality() > DELTA_LIMIT
                        ? MirrorPacket.full(ext)
                        : MirrorPacket.delta(ext, changes));
                }
            }
            syncHiddenMain(player);

            int generation = WeightTable.generation();
            Integer lastGeneration = LAST_GENERATION.get(player);
            if (lastGeneration == null || lastGeneration != generation) {
                LAST_GENERATION.put(player, generation);
                syncBaseCapacity(player);
                dirty = true;
            }

            int capacity = CarryLoad.capacity(player);
            Integer lastCapacity = LAST_CAPACITY.get(player);
            boolean capacityChanged = lastCapacity == null || lastCapacity != capacity;
            if (capacityChanged) {
                LAST_CAPACITY.put(player, capacity);
                dirty = true;
            }

            if (dirty) {
                IntroCue.check(player, ext);
                int carried = CarryLoad.carriedWeight(player);
                int tier = CarryLoad.tierFor(carried, capacity);
                LAST_TIER.put(player, tier);
                apply(player, CarryLoad.penaltyFor(carried, capacity));

                Integer last = LAST_SENT.get(player);
                if (last == null || last != carried || capacityChanged) {
                    LAST_SENT.put(player, carried);
                    LINet.toPlayer(player, new WeightPacket(carried, capacity));
                }
            }

            applyOverload(player, LAST_TIER.getOrDefault(player, 0),
                LAST_SENT.getOrDefault(player, 0), capacity);
            MountEvents.tick(player, LAST_SENT.getOrDefault(player, 0), capacity);
        }

        @SubscribeEvent
        public static void onJump(LivingEvent.LivingJumpEvent event) {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }
            if (player.level().isClientSide()) {
                return;
            }
            if (!CarryLoad.stalled(player)) {
                return;
            }
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onPickup(ItemEntityPickupEvent.Pre event) {
            Player player = event.getPlayer();
            if (player.level().isClientSide()) {
                return;
            }
            if (player.isCreative() || player.isSpectator() || !WeightTable.isReady()) {
                return;
            }
            int carried = CarryLoad.carriedWeight(player);
            int capacity = CarryLoad.capacity(player);
            boolean blocked = LIConfig.COMMON.blockPickupWhenFull.get()
                ? CarryLoad.ratioFor(carried, capacity) >= 1.0D
                : CarryLoad.stalled(carried, capacity);
            if (blocked) {
                event.setCanPickup(TriState.FALSE);
            }
        }
    }

    private static void syncHiddenMain(ServerPlayer player) {
        ScrollContext context = ScrollRegistry.get(player.containerMenu);
        if (context == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack[] last = LAST_MAIN.computeIfAbsent(player, p -> new ItemStack[CombinedInventoryHandler.MAIN_COUNT]);
        BitSet changed = new BitSet(CombinedInventoryHandler.MAIN_COUNT);
        for (int i = 0; i < CombinedInventoryHandler.MAIN_COUNT; i++) {
            ItemStack current = inventory.getItem(CombinedInventoryHandler.MAIN_OFFSET + i);
            if (last[i] != null && ItemStack.matches(last[i], current)) {
                continue;
            }
            last[i] = current.copy();
            if (!context.isBackingVisible(i)) {
                changed.set(i);
            }
        }
        if (!changed.isEmpty()) {
            LINet.toPlayer(player, MirrorPacket.mainDelta(inventory, changed));
        }
    }

    private static void syncBaseCapacity(Player player) {
        AttributeInstance attr = player.getAttribute(LIAttributes.CARRY_CAPACITY);
        if (attr == null) {
            return;
        }
        double base = LIConfig.COMMON.baseCapacity.get();
        if (attr.getBaseValue() != base) {
            attr.setBaseValue(base);
        }
        CapacityBonus.apply(player);
    }

    public static void applyOverload(Player player, int tier, int carried, int capacity) {
        if (tier <= 0) {
            return;
        }
        if (CarryLoad.sprintBlocked(carried, capacity) && player.isSprinting()) {
            player.setSprinting(false);
        }
        clampAir(player, tier);
    }

    private static void clampAir(Player player, int tier) {
        if (player.onGround() || player.getAbilities().flying || player.isFallFlying()) {
            return;
        }
        if (player.isInWater() || player.isInLava() || player.isPassenger() || player.onClimbable()) {
            return;
        }

        Vec3 v = player.getDeltaMovement();
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        if (horizontal < 1.0E-4D) {
            return;
        }

        double max = player.getAttributeValue(Attributes.MOVEMENT_SPEED) * AIR_FACTOR[tier];
        if (horizontal > max) {
            double scale = max / horizontal;
            player.setDeltaMovement(v.x * scale, v.y, v.z * scale);
        }
    }

    private static void apply(Player player, double amount) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier existing = speed.getModifier(OVERLOAD_ID);
        if (existing != null) {
            if (existing.amount() == amount) {
                return;
            }
            speed.removeModifier(OVERLOAD_ID);
        }
        if (amount != 0.0D) {
            speed.addTransientModifier(new AttributeModifier(
                OVERLOAD_ID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void clear(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(OVERLOAD_ID) != null) {
            speed.removeModifier(OVERLOAD_ID);
        }
    }
}
