package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.InventorySorter;
import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.inventory.SlotGroupHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class SortPacket {

    private static final int COOLDOWN_TICKS = 5;

    private static final Map<ServerPlayer, Integer> LAST_SORT = new WeakHashMap<>();

    private final boolean byWeight;

    public SortPacket() {
        this(false);
    }

    public SortPacket(boolean byWeight) {
        this.byWeight = byWeight;
    }

    public SortPacket(FriendlyByteBuf buf) {
        this.byWeight = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.byWeight);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ScrollContext context = ScrollRegistry.get(player.containerMenu);
            if (context != null && accept(player)) {
                InventorySorter.sort(context.getBacking(), this.byWeight);
                for (SlotGroupHandler group : SlotGroupHandler.containerGroups(player.containerMenu, player.getInventory())) {
                    InventorySorter.sort(group, this.byWeight);
                }
                player.getInventory().setChanged();
                context.setFilter(null);
                player.containerMenu.broadcastFullState();
                ExtendedInventory extended = LICaps.get(player);
                extended.clearChanges();
                LINet.toPlayer(player, MirrorPacket.full(extended));
            }
        }
        ctx.get().setPacketHandled(true);
    }

    private static boolean accept(ServerPlayer player) {
        if (!LIConfig.COMMON.sortEnabled.get()) {
            return false;
        }
        int now = player.server.getTickCount();
        Integer last = LAST_SORT.get(player);
        if (last != null && now >= last && now - last < COOLDOWN_TICKS) {
            return false;
        }
        LAST_SORT.put(player, now);
        return true;
    }
}
