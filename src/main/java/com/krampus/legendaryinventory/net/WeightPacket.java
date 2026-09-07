package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.client.ClientWeightState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WeightPacket {

    private final int weight;
    private final int capacity;

    public WeightPacket(int weight, int capacity) {
        this.weight = weight;
        this.capacity = capacity;
    }

    public WeightPacket(FriendlyByteBuf buf) {
        this.weight = buf.readVarInt();
        this.capacity = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(weight);
        buf.writeVarInt(capacity);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientWeightState.set(weight, capacity)));
        ctx.get().setPacketHandled(true);
    }
}
