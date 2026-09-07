package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.HiddenCollect;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CollectPacket() implements CustomPacketPayload {

    public static final Type<CollectPacket> TYPE = new Type<>(LINet.id("collect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CollectPacket> STREAM_CODEC =
        StreamCodec.unit(new CollectPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CollectPacket packet, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player && HiddenCollect.collect(player.containerMenu)) {
            player.containerMenu.broadcastChanges();
        }
    }
}
