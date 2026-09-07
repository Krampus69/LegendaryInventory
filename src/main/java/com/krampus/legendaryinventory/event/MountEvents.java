package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class MountEvents {

    private static final UUID MOUNT_OVERLOAD_UUID = UUID.fromString("b3f0a4e2-6c1d-4f8a-9d27-5e0c9a1b7f34");
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
        AttributeModifier existing = speed.getModifier(MOUNT_OVERLOAD_UUID);
        if (amount == 0.0D) {
            if (existing != null) {
                speed.removeModifier(MOUNT_OVERLOAD_UUID);
            }
            return;
        }
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return;
            }
            speed.removeModifier(MOUNT_OVERLOAD_UUID);
        }
        speed.addTransientModifier(new AttributeModifier(
            MOUNT_OVERLOAD_UUID, "legendaryinventory.mount_overload", amount,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    public static void clearPenalty(Player player) {
        LivingEntity previous = RIDDEN.remove(player);
        if (previous != null) {
            removeModifier(previous);
        }
    }

    private static void removeModifier(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(MOUNT_OVERLOAD_UUID) != null) {
            speed.removeModifier(MOUNT_OVERLOAD_UUID);
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
