package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.net.IntroCuePacket;
import com.krampus.legendaryinventory.net.LINet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class IntroCue {

    private static final String KEY = LegendaryInventory.MODID + ":row_cue_shown";

    private IntroCue() {}

    public static void check(ServerPlayer player, ExtendedInventory extended) {
        if (!LIConfig.COMMON.firstRowCue.get()) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(KEY)) {
            return;
        }
        if (extended.usedSlots() == 0) {
            return;
        }
        persisted.putBoolean(KEY, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        LINet.toPlayer(player, new IntroCuePacket());
    }
}
