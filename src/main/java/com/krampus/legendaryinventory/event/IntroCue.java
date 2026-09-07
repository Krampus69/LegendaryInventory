package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.inventory.WeightProfile;
import com.krampus.legendaryinventory.net.IntroCuePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IntroCue {

    private IntroCue() {}

    public static void check(ServerPlayer player, ExtendedInventory extended) {
        if (!LIConfig.COMMON.firstRowCue.get()) {
            return;
        }
        WeightProfile profile = LIAttachments.profile(player);
        if (profile.rowCueShown()) {
            return;
        }
        if (extended.usedSlots() == 0) {
            return;
        }
        LIAttachments.setProfile(player, profile.withRowCueShown(true));
        PacketDistributor.sendToPlayer(player, new IntroCuePacket());
    }
}
