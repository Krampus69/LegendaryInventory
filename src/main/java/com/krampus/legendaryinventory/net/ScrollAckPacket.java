package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record ScrollAckPacket(int containerId, int row, boolean expanded) implements CustomPacketPayload {

    public static final Type<ScrollAckPacket> TYPE = new Type<>(LINet.id("scroll_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollAckPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ScrollAckPacket::containerId,
        ByteBufCodecs.VAR_INT, ScrollAckPacket::row,
        ByteBufCodecs.BOOL, ScrollAckPacket::expanded,
        ScrollAckPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, AbstractContainerMenu menu) {
        ScrollContext context = ScrollRegistry.get(menu);
        int row = context == null ? 0 : context.getScrollRow();
        boolean expanded = context != null && context.isExpanded();
        LINet.toPlayer(player, new ScrollAckPacket(menu.containerId, row, expanded));
    }
}
