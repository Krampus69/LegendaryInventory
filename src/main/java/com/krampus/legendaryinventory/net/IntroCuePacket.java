package com.krampus.legendaryinventory.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IntroCuePacket() implements CustomPacketPayload {

    public static final Type<IntroCuePacket> TYPE = new Type<>(LINet.id("intro_cue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IntroCuePacket> STREAM_CODEC =
        StreamCodec.unit(new IntroCuePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
