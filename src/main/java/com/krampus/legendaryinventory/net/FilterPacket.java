package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class FilterPacket {

    private static final int MAX = ExtendedInventory.SIZE + CombinedInventoryHandler.MAIN_COUNT;

    private final int[] indices;

    public FilterPacket(@Nullable int[] indices) {
        this.indices = indices;
    }

    public FilterPacket(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            this.indices = null;
            return;
        }
        int count = Math.max(0, Math.min(buf.readVarInt(), MAX));
        this.indices = new int[count];
        for (int i = 0; i < count; i++) {
            this.indices[i] = Math.max(0, Math.min(buf.readVarInt(), MAX - 1));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(indices != null);
        if (indices == null) {
            return;
        }
        buf.writeVarInt(indices.length);
        for (int i : indices) {
            buf.writeVarInt(i);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ScrollContext context = ScrollRegistry.get(player.containerMenu);
            if (context != null) {
                context.setFilter(indices);
                player.containerMenu.broadcastFullState();
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
