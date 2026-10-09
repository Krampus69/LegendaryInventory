package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreativeSetPacket {

    private final int backing;
    private final ItemStack stack;

    public CreativeSetPacket(int backing, ItemStack stack) {
        this.backing = backing;
        this.stack = stack;
    }

    public CreativeSetPacket(FriendlyByteBuf buf) {
        this.backing = buf.readVarInt();
        this.stack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(backing);
        buf.writeItem(stack);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.isCreative()) {
            apply(player);
        }
        ctx.get().setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        ScrollContext context = ScrollRegistry.get(player.inventoryMenu);
        if (context == null) {
            return;
        }
        CombinedInventoryHandler handler = context.getBacking();
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
