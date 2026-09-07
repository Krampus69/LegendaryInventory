package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.BitSet;

public record FilterPacket(@Nullable int[] indices) implements CustomPacketPayload {

    public static final Type<FilterPacket> TYPE = new Type<>(LINet.id("filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterPacket> STREAM_CODEC =
        StreamCodec.of(FilterPacket::write, FilterPacket::read);

    private static final int MAX = ExtendedInventory.SIZE + CombinedInventoryHandler.MAIN_COUNT;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static FilterPacket read(RegistryFriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return new FilterPacket(null);
        }
        int count = Math.max(0, Math.min(buf.readVarInt(), MAX));
        int[] read = new int[count];
        BitSet seen = new BitSet(MAX);
        int kept = 0;
        for (int i = 0; i < count; i++) {
            int index = Math.max(0, Math.min(buf.readVarInt(), MAX - 1));
            if (seen.get(index)) {
                continue;
            }
            seen.set(index);
            read[kept++] = index;
        }
        return new FilterPacket(Arrays.copyOf(read, kept));
    }

    private static void write(RegistryFriendlyByteBuf buf, FilterPacket packet) {
        buf.writeBoolean(packet.indices != null);
        if (packet.indices == null) {
            return;
        }
        buf.writeVarInt(packet.indices.length);
        for (int i : packet.indices) {
            buf.writeVarInt(i);
        }
    }

    public static void handle(FilterPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(player.containerMenu);
        if (context != null) {
            context.setFilter(packet.indices());
            player.containerMenu.broadcastFullState();
        }
    }
}
