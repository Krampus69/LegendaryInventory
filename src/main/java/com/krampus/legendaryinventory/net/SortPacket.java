package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.InventorySorter;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.inventory.SlotGroupHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.WeakHashMap;

public record SortPacket(boolean byWeight) implements CustomPacketPayload {

    public static final Type<SortPacket> TYPE = new Type<>(LINet.id("sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, SortPacket::byWeight, SortPacket::new);

    private static final int COOLDOWN_TICKS = 5;
    private static final Map<ServerPlayer, Integer> LAST_SORT = new WeakHashMap<>();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SortPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(player.containerMenu);
        if (context == null || !accept(player)) {
            return;
        }
        InventorySorter.sort(context.getBacking(), packet.byWeight());
        for (SlotGroupHandler group : SlotGroupHandler.containerGroups(player.containerMenu, player.getInventory())) {
            InventorySorter.sort(group, packet.byWeight());
        }
        player.getInventory().setChanged();
        context.setFilter(null);
        player.containerMenu.broadcastFullState();
        ExtendedInventory extended = LIAttachments.extended(player);
        extended.clearChanges();
        LINet.toPlayer(player, MirrorPacket.full(extended));
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
