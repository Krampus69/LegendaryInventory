package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.client.ClientMirror;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScrollAckPacket {

    private final int containerId;
    private final int row;
    private final boolean expanded;

    public ScrollAckPacket(int containerId, int row, boolean expanded) {
        this.containerId = containerId;
        this.row = row;
        this.expanded = expanded;
    }

    public ScrollAckPacket(FriendlyByteBuf buf) {
        this.containerId = buf.readVarInt();
        this.row = buf.readVarInt();
        this.expanded = buf.readBoolean();
    }

    public static void send(ServerPlayer player, AbstractContainerMenu menu) {
        ScrollContext context = ScrollRegistry.get(menu);
        int row = context == null ? 0 : context.getScrollRow();
        boolean expanded = context != null && context.isExpanded();
        LINet.toPlayer(player, new ScrollAckPacket(menu.containerId, row, expanded));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(row);
        buf.writeBoolean(expanded);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientMirror.acknowledgeScroll(containerId, row, expanded)));
        ctx.get().setPacketHandled(true);
    }
}
