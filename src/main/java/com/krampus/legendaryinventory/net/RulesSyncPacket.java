package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.weight.WeightRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record RulesSyncPacket(WeightRules.Values values) implements CustomPacketPayload {

    public static final Type<RulesSyncPacket> TYPE = new Type<>(LINet.id("rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RulesSyncPacket> STREAM_CODEC =
        StreamCodec.of(RulesSyncPacket::write, RulesSyncPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static RulesSyncPacket read(RegistryFriendlyByteBuf buf) {
        int stackable = buf.readVarInt();
        int semi = buf.readVarInt();
        int unstackable = buf.readVarInt();
        double slowStart = buf.readDouble();
        double heavy = buf.readDouble();
        double sprintBlock = buf.readDouble();
        double stall = buf.readDouble();
        int count = buf.readVarInt();
        Map<ResourceLocation, Integer> overrides = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            Item item = BuiltInRegistries.ITEM.byId(buf.readVarInt());
            int weight = buf.readVarInt();
            overrides.put(BuiltInRegistries.ITEM.getKey(item), weight);
        }
        return new RulesSyncPacket(
            new WeightRules.Values(stackable, semi, unstackable, slowStart, heavy, sprintBlock, stall, overrides));
    }

    private static void write(RegistryFriendlyByteBuf buf, RulesSyncPacket packet) {
        WeightRules.Values values = packet.values;
        buf.writeVarInt(values.stackable());
        buf.writeVarInt(values.semiStackable());
        buf.writeVarInt(values.unstackable());
        buf.writeDouble(values.slowStart());
        buf.writeDouble(values.heavy());
        buf.writeDouble(values.sprintBlock());
        buf.writeDouble(values.stall());
        Map<Integer, Integer> present = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : values.overrides().entrySet()) {
            if (!BuiltInRegistries.ITEM.containsKey(entry.getKey())) {
                continue;
            }
            present.put(BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(entry.getKey())), entry.getValue());
        }
        buf.writeVarInt(present.size());
        for (Map.Entry<Integer, Integer> entry : present.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public static void handle(RulesSyncPacket packet, IPayloadContext ctx) {
        WeightRules.apply(packet.values());
    }
}
