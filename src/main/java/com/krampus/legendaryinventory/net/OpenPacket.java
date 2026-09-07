package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.menu.LIMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenPacket() implements CustomPacketPayload {

    public static final Type<OpenPacket> TYPE = new Type<>(LINet.id("open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPacket> STREAM_CODEC =
        StreamCodec.unit(new OpenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || !player.isCreative()) {
            return;
        }
        ExtendedInventory extended = LIAttachments.extended(player);
        extended.clearChanges();
        LINet.toPlayer(player, MirrorPacket.full(extended));
        player.openMenu(new SimpleMenuProvider(
            (id, inv, p) -> new LIMenu(id, inv),
            Component.translatable("container.legendaryinventory.extended")
        ));
    }
}
