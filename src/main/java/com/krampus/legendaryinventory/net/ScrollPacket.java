package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScrollPacket {

    private final int containerId;
    private final int row;

    public ScrollPacket(AbstractContainerMenu menu, int row) {
        this.containerId = menu.containerId;
        this.row = row;
    }

    public ScrollPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.row = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(row);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            AbstractContainerMenu menu = player.containerMenu.containerId == containerId
                ? player.containerMenu
                : player.inventoryMenu.containerId == containerId ? player.inventoryMenu : null;
            ScrollContext context = menu == null ? null : ScrollRegistry.get(menu);
            if (LIConfig.COMMON.logMenuAugment.get()) {
                LegendaryInventory.LOGGER.info("[server] ScrollPacket id={} row={} current={} id={} applied={}",
                    containerId, row, player.containerMenu.getClass().getSimpleName(), player.containerMenu.containerId, context != null);
            }
            if (context != null) {
                context.setScrollRow(row);
            }
            ScrollAckPacket.send(player, menu == null ? player.containerMenu : menu);
            if (context != null) {
                menu.broadcastChanges();
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
