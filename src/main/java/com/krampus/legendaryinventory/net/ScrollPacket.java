package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScrollPacket {

    private final int row;

    public ScrollPacket(int row) {
        this.row = row;
    }

    public ScrollPacket(FriendlyByteBuf buf) {
        this.row = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(row);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ScrollContext context = ScrollRegistry.get(player.containerMenu);
            if (context != null) {
                context.setScrollRow(row);
                player.containerMenu.broadcastChanges();
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
