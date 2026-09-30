package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PickExtendedPacket {

    private final int index;

    public PickExtendedPacket(int index) {
        this.index = index;
    }

    public PickExtendedPacket(FriendlyByteBuf buf) {
        this.index = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(index);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && !player.isSpectator()) {
            pick(player, index);
        }
        ctx.get().setPacketHandled(true);
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
