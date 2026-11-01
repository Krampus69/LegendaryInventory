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
    private final boolean inventory;
    private final boolean container;

    public SortPacket() {
        this(false);
    }

    public SortPacket(boolean byWeight) {
        this(byWeight, true, true);
    }

    public SortPacket(boolean byWeight, boolean inventory, boolean container) {
        this.byWeight = byWeight;
        this.inventory = inventory;
        this.container = container;
    }

    public SortPacket(FriendlyByteBuf buf) {
        this.byWeight = buf.readBoolean();
        this.inventory = buf.readBoolean();
        this.container = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.byWeight);
        buf.writeBoolean(this.inventory);
        buf.writeBoolean(this.container);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            ScrollContext context = ScrollRegistry.get(player.containerMenu);
            if (context != null && (this.inventory || this.container) && accept(player)) {
                if (this.inventory) {
                    InventorySorter.sort(context.getBacking(), this.byWeight);
                    player.getInventory().setChanged();
                    context.setFilter(null);
                }
                if (this.container) {
                    for (SlotGroupHandler group : SlotGroupHandler.containerGroups(player.containerMenu, player.getInventory())) {
                        InventorySorter.sort(group, this.byWeight);
                    }
                }
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
