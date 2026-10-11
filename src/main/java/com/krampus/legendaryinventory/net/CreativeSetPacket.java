package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CreativeSetPacket(int backing, ItemStack stack) implements CustomPacketPayload {

    public static final Type<CreativeSetPacket> TYPE = new Type<>(LINet.id("creative_set"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeSetPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CreativeSetPacket::backing,
        ItemStack.OPTIONAL_STREAM_CODEC, CreativeSetPacket::stack,
        CreativeSetPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeSetPacket packet, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || !player.isCreative()) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(player.inventoryMenu);
        if (context == null) {
            return;
        }
        CombinedInventoryHandler handler = context.getBacking();
        int backing = packet.backing();
        ItemStack stack = packet.stack();
        if (backing < 0 || backing >= handler.getSlots()) {
            return;
        }
        if (!stack.isEmpty() && (stack.getCount() > stack.getMaxStackSize() || !stack.isItemEnabled(player.level().enabledFeatures()))) {
            return;
        }
        handler.setStackInSlot(backing, stack);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }
}
