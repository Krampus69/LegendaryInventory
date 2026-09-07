package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ClientMirror {

    private ClientMirror() {}

    public static void accept(boolean full, int[] indices, ItemStack[] stacks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        apply(LIAttachments.extended(mc.player), full, indices, stacks);
    }

    private static void apply(ExtendedInventory ext, boolean full, int[] indices, ItemStack[] stacks) {
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
                }
            }
        }
        ext.clearChanges();
    }
}
