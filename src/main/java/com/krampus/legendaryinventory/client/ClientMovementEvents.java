package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.event.WeightEvents;
import com.krampus.legendaryinventory.weight.CarryLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class ClientMovementEvents {

    private static final int HINT_INTERVAL = 20;

    private static int hintCooldown = 0;
    private static boolean jumpBlocked = false;

    private ClientMovementEvents() {}

    private static boolean wantsToMove(Player player) {
        if (!(player instanceof LocalPlayer local) || local.input == null) {
            return false;
        }
        return local.input.forwardImpulse != 0.0F
            || local.input.leftImpulse != 0.0F
            || local.input.jumping
            || jumpBlocked;
    }

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        jumpBlocked = false;
        Player player = event.getEntity();
        if (!isLocalSurvivalPlayer(player) || !event.getInput().jumping) {
            return;
        }
        if (player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger()) {
            return;
        }
        int carried = ClientWeightState.get();
        if (carried < 0 || !CarryLoad.stalled(carried, CarryLoad.capacity(player))) {
            return;
        }
        event.getInput().jumping = false;
        jumpBlocked = true;
    }

    private static boolean isLocalSurvivalPlayer(Player player) {
        if (!player.level().isClientSide() || Minecraft.getInstance().player != player) {
            return false;
        }
        return !player.isCreative() && !player.isSpectator();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!isLocalSurvivalPlayer(player)) {
            return;
        }

        int carried = ClientWeightState.get();
        if (carried < 0) {
            return;
        }

        int capacity = CarryLoad.capacity(player);
        WeightEvents.applyOverload(player, CarryLoad.tierFor(carried, capacity), carried, capacity);

        if (hintCooldown > 0) {
            hintCooldown--;
        }
        if (CarryLoad.stalled(carried, capacity) && wantsToMove(player) && hintCooldown == 0) {
            player.displayClientMessage(Component.translatable("gui.legendaryinventory.weight.overencumbered"), true);
            hintCooldown = HINT_INTERVAL;
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isLocalSurvivalPlayer(player)) {
            return;
        }

        int carried = ClientWeightState.get();
        if (carried < 0) {
            return;
        }
        if (!CarryLoad.stalled(carried, CarryLoad.capacity(player))) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0.0D, motion.z);
    }
}
