package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.CarryLoad;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.event.WaystoneTeleportEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WaystonesCompat {

    public static final String MOD_ID = "waystones";

    private WaystonesCompat() {}

    public static void init() {
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, WaystonesCompat::onTeleport);
    }

    private static void onTeleport(WaystoneTeleportEvent.Pre event) {
        if (!LIConfig.COMMON.waystonesBlockWhenStalled.get()) {
            return;
        }
        if (!(event.getContext().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!CarryLoad.stalled(player)) {
            return;
        }
        player.displayClientMessage(Component.translatable("gui.legendaryinventory.compat.waystones.blocked"), true);
        event.setCanceled(true);
    }
}
