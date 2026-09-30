package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PickExtendedPacket(int index) implements CustomPacketPayload {

    public static final Type<PickExtendedPacket> TYPE = new Type<>(LINet.id("pick_extended"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PickExtendedPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, PickExtendedPacket::index, PickExtendedPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PickExtendedPacket packet, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player && !player.isSpectator()) {
            pick(player, packet.index());
        }
    }

    private static void pick(ServerPlayer player, int index) {
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null || index < 0 || index >= ext.getSlots()) {
            return;
        }
        ItemStack picked = ext.getStackInSlot(index);
        if (picked.isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        int hotbar = inventory.getSuitableHotbarSlot();
        ItemStack displaced = inventory.getItem(hotbar);
        ext.setStackInSlot(index, displaced);
        inventory.setItem(hotbar, picked);
        inventory.selected = hotbar;
        player.connection.send(new ClientboundSetCarriedItemPacket(hotbar));
        player.inventoryMenu.broadcastChanges();
    }
}
