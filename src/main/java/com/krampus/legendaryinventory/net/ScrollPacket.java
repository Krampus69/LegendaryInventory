package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScrollPacket(int containerId, int row) implements CustomPacketPayload {

    public static final Type<ScrollPacket> TYPE = new Type<>(LINet.id("scroll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ScrollPacket::containerId,
        ByteBufCodecs.VAR_INT, ScrollPacket::row,
        ScrollPacket::new
    );

    public ScrollPacket(AbstractContainerMenu menu, int row) {
        this(menu.containerId, row);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScrollPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu.containerId == packet.containerId()
            ? player.containerMenu
            : player.inventoryMenu.containerId == packet.containerId() ? player.inventoryMenu : null;
        ScrollContext context = menu == null ? null : ScrollRegistry.get(menu);
        if (context != null) {
            context.setScrollRow(packet.row());
        }
        ScrollAckPacket.send(player, menu == null ? player.containerMenu : menu);
        if (context != null) {
            menu.broadcastChanges();
        }
    }
}
