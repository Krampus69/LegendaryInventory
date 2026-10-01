package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExpandPacket(boolean expanded) implements CustomPacketPayload {

    public static final Type<ExpandPacket> TYPE = new Type<>(LINet.id("expand"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExpandPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, ExpandPacket::expanded, ExpandPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExpandPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(player.inventoryMenu);
        if (context != null) {
            context.setExpanded(packet.expanded());
        }
        ScrollAckPacket.send(player, player.inventoryMenu);
        if (context != null) {
            player.inventoryMenu.broadcastChanges();
        }
    }
}
