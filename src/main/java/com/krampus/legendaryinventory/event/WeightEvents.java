package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.MirrorPacket;
import com.krampus.legendaryinventory.net.WeightPacket;
import com.krampus.legendaryinventory.weight.CapacityBonus;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightDirty;
import com.krampus.legendaryinventory.weight.LIAttributes;
import com.krampus.legendaryinventory.weight.WeightRules;
import com.krampus.legendaryinventory.weight.WeightTable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.BitSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class WeightEvents {

    public static final UUID OVERLOAD_UUID = UUID.fromString("6f2b1c40-9d3e-4a71-8f55-0c1a7b93e2d1");

    private static final int DELTA_LIMIT = 32;
    private static final double[] AIR_FACTOR = {0.0D, 1.80D, 1.40D, 1.10D, 0.0D};

    private static final Map<Player, Integer> LAST_SENT = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_TIER = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_CAPACITY = new WeakHashMap<>();
    private static final Map<Player, Integer> LAST_GENERATION = new WeakHashMap<>();

    private WeightEvents() {}

    @Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onAttributeModification(EntityAttributeModificationEvent event) {
            event.add(EntityType.PLAYER, LIAttributes.CARRY_CAPACITY.get());
        }
    }

    @Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
    public static final class ForgeBus {

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
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            if (!(event.player instanceof ServerPlayer player)) {
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
            ExtendedInventory ext = LICaps.get(player);
            if (ext.hasChanges()) {
                dirty = true;
                if (ScrollRegistry.has(player.containerMenu)) {
                    BitSet changes = ext.takeChanges();
                    LINet.toPlayer(player, changes.cardinality() > DELTA_LIMIT
                        ? MirrorPacket.full(ext)
                        : MirrorPacket.delta(ext, changes));
                }
            }

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
        public static void onPickup(EntityItemPickupEvent event) {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            Player player = event.getEntity();
            if (player.isCreative() || player.isSpectator() || !WeightTable.isReady()) {
                return;
            }
            int carried = CarryLoad.carriedWeight(player);
            int capacity = CarryLoad.capacity(player);
            boolean blocked = LIConfig.COMMON.blockPickupWhenFull.get()
                ? CarryLoad.ratioFor(carried, capacity) >= 1.0D
                : CarryLoad.stalled(carried, capacity);
            if (blocked) {
                event.setCanceled(true);
            }
        }
    }

    private static void syncBaseCapacity(Player player) {
        AttributeInstance attr = player.getAttribute(LIAttributes.CARRY_CAPACITY.get());
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
        AttributeModifier existing = speed.getModifier(OVERLOAD_UUID);
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return;
            }
            speed.removeModifier(OVERLOAD_UUID);
        }
        if (amount != 0.0D) {
            speed.addTransientModifier(new AttributeModifier(
                OVERLOAD_UUID,
                "legendaryinventory.overload",
                amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static void clear(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(OVERLOAD_UUID) != null) {
            speed.removeModifier(OVERLOAD_UUID);
        }
    }
}
