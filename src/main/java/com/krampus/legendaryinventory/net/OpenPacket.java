package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.menu.LIMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenPacket {

    public OpenPacket() {}

    public OpenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || !player.isCreative()) {
            ctx.get().setPacketHandled(true);
            return;
        }
        var extended = LICaps.get(player);
        extended.clearChanges();
        LINet.toPlayer(player, MirrorPacket.full(extended));
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (id, inv, p) -> new LIMenu(id, inv),
            Component.translatable("container.legendaryinventory.extended")
        ));
        ctx.get().setPacketHandled(true);
    }
}
