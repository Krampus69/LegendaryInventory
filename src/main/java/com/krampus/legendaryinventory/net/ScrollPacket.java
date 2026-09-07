package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScrollPacket(int row) implements CustomPacketPayload {

    public static final Type<ScrollPacket> TYPE = new Type<>(LINet.id("scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, ScrollPacket::row, ScrollPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScrollPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(player.containerMenu);
        if (context != null) {
            context.setScrollRow(packet.row());
            player.containerMenu.broadcastChanges();
        }
    }
}
