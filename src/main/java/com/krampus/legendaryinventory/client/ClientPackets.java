package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.net.IntroCuePacket;
import com.krampus.legendaryinventory.net.MirrorPacket;
import com.krampus.legendaryinventory.net.WeightPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPackets {

    private ClientPackets() {}

    public static void handleWeight(WeightPacket packet, IPayloadContext ctx) {
        ClientWeightState.set(packet.weight(), packet.capacity());
    }

    public static void handleMirror(MirrorPacket packet, IPayloadContext ctx) {
        ClientMirror.accept(packet.full(), packet.indices(), packet.stacks());
    }

    public static void handleIntroCue(IntroCuePacket packet, IPayloadContext ctx) {
        ClientIntroCue.arm();
    }
}
