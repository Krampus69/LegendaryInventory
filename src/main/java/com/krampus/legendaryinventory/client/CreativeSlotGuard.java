package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.net.CreativeSetPacket;
import com.krampus.legendaryinventory.net.LINet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CreativeSlotGuard {

    private static int userActionDepth = 0;

    private CreativeSlotGuard() {}

    public static void enterUserAction() {
        userActionDepth++;
    }

    public static void exitUserAction() {
        userActionDepth = Math.max(0, userActionDepth - 1);
    }

    public static boolean intercept(ItemStack stack, int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || slot < 0) {
            return false;
        }
        InventoryMenu menu = mc.player.inventoryMenu;
        if (slot >= menu.slots.size()) {
            return false;
        }
        Slot target = menu.getSlot(slot);
        if (!(target instanceof LISlot li)) {
            return false;
        }
        if (userActionDepth > 0) {
            int backing = li.context().writeIndexFor(li.window());
            if (backing >= 0) {
                LINet.toServer(new CreativeSetPacket(backing, stack.copy()));
            }
        }
        return true;
    }
}
