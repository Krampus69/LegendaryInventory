package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.LIAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearExtendedPacket() implements CustomPacketPayload {

    public static final Type<ClearExtendedPacket> TYPE = new Type<>(LINet.id("clear_extended"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearExtendedPacket> STREAM_CODEC =
        StreamCodec.unit(new ClearExtendedPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearExtendedPacket packet, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player && player.isCreative()) {
            LIAttachments.extended(player).clearAll();
        }
    }
}
