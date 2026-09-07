package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.weight.WeightRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RulesSyncPacket {

    private final WeightRules.Values values;

    public RulesSyncPacket(WeightRules.Values values) {
        this.values = values;
    }

    public RulesSyncPacket(FriendlyByteBuf buf) {
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
        this.values = new WeightRules.Values(stackable, semi, unstackable, slowStart, heavy, sprintBlock, stall, overrides);
    }

    public void encode(FriendlyByteBuf buf) {
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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> WeightRules.apply(values));
        ctx.get().setPacketHandled(true);
    }
}
