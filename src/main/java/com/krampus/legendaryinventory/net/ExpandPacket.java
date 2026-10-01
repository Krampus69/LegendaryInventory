package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExpandPacket {

    private final boolean expanded;

    public ExpandPacket(boolean expanded) {
        this.expanded = expanded;
    }

    public ExpandPacket(FriendlyByteBuf buf) {
        this.expanded = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(expanded);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ScrollContext context = ScrollRegistry.get(player.inventoryMenu);
            if (context != null) {
                context.setExpanded(expanded);
            }
            ScrollAckPacket.send(player, player.inventoryMenu);
            if (context != null) {
                player.inventoryMenu.broadcastChanges();
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
