package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.client.ClientIntroCue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class IntroCuePacket {

    public IntroCuePacket() {
    }

    public IntroCuePacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> ClientIntroCue::arm));
        ctx.get().setPacketHandled(true);
    }
}
