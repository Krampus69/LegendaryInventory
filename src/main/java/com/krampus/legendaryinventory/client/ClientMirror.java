package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ClientMirror {

    private ClientMirror() {}

    public static void accept(boolean full, int[] indices, ItemStack[] stacks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        apply(mc.player.getInventory(), LIAttachments.extended(mc.player), full, indices, stacks);
    }

    private static void apply(Inventory inventory, ExtendedInventory ext, boolean full, int[] indices, ItemStack[] stacks) {
        if (full) {
            int n = Math.min(stacks.length, ext.getSlots());
            for (int i = 0; i < n; i++) {
                ext.setStackInSlot(i, stacks[i]);
            }
        } else if (indices != null) {
            for (int i = 0; i < indices.length; i++) {
                int slot = indices[i];
                if (slot >= 0 && slot < ext.getSlots()) {
                    ext.setStackInSlot(slot, stacks[i]);
                } else if (slot >= ExtendedInventory.SIZE
                    && slot < ExtendedInventory.SIZE + CombinedInventoryHandler.MAIN_COUNT) {
                    inventory.setItem(CombinedInventoryHandler.MAIN_OFFSET + slot - ExtendedInventory.SIZE, stacks[i]);
                }
            }
        }
        ext.clearChanges();
    }
}
