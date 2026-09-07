package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightTags;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class MountEvents {

    private static final ResourceLocation MOUNT_OVERLOAD_ID =
        ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "mount_overload");
    private static final Map<Player, LivingEntity> RIDDEN = new WeakHashMap<>();

    private MountEvents() {}

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.isMounting()) {
            clearPenalty(player);
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (!isAnimal(event.getEntityBeingMounted()) && !LIConfig.COMMON.mountBlockVehicles.get()) {
            return;
        }
        int carried = CarryLoad.carriedWeight(player);
        int capacity = CarryLoad.capacity(player);
        if (capacity <= 0) {
            return;
        }
        if (carried / (double) capacity >= LIConfig.COMMON.mountBlockRatio.get()) {
            player.displayClientMessage(Component.translatable("gui.legendaryinventory.mount.blocked"), true);
            event.setCanceled(true);
        }
    }

    public static void tick(Player player, int carried, int capacity) {
        LivingEntity previous = RIDDEN.get(player);
        LivingEntity current = player.getVehicle() instanceof LivingEntity living && isAnimal(living) ? living : null;
        if (previous != null && previous != current) {
            removeModifier(previous);
            RIDDEN.remove(player);
        }
        if (current == null) {
            return;
        }
        RIDDEN.put(player, current);
        double amount = LIConfig.COMMON.mountAnimalPenalty.get() ? CarryLoad.penaltyFor(carried, capacity) : 0.0D;
        AttributeInstance speed = current.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier existing = speed.getModifier(MOUNT_OVERLOAD_ID);
        if (amount == 0.0D) {
            if (existing != null) {
                speed.removeModifier(MOUNT_OVERLOAD_ID);
            }
            return;
        }
        if (existing != null) {
            if (existing.amount() == amount) {
                return;
            }
            speed.removeModifier(MOUNT_OVERLOAD_ID);
        }
        speed.addTransientModifier(new AttributeModifier(
            MOUNT_OVERLOAD_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void clearPenalty(Player player) {
        LivingEntity previous = RIDDEN.remove(player);
        if (previous != null) {
            removeModifier(previous);
        }
    }

    private static void removeModifier(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(MOUNT_OVERLOAD_ID) != null) {
            speed.removeModifier(MOUNT_OVERLOAD_ID);
        }
    }

    public static boolean isAnimal(Entity entity) {
        if (entity.getType().is(WeightTags.FREE_MOUNTS)) {
            return false;
        }
        if (entity.getType().is(WeightTags.BLOCKED_MOUNTS)) {
            return true;
        }
        return entity instanceof LivingEntity;
    }
}
