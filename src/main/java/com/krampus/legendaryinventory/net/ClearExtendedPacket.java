package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.LICaps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClearExtendedPacket {

    public ClearExtendedPacket() {}

    public ClearExtendedPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.isCreative()) {
            LICaps.get(player).clearAll();
        }
        ctx.get().setPacketHandled(true);
    }
}
