package com.krampus.legendaryinventory.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WeightPacket(int weight, int capacity) implements CustomPacketPayload {

    public static final Type<WeightPacket> TYPE = new Type<>(LINet.id("weight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeightPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, WeightPacket::weight,
        ByteBufCodecs.VAR_INT, WeightPacket::capacity,
        WeightPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
