package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class HiddenCollect {

    private HiddenCollect() {}

    public static boolean collect(AbstractContainerMenu menu) {
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return false;
        }
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || carried.getCount() >= carried.getMaxStackSize()) {
            return false;
        }
        CombinedInventoryHandler backing = context.getBacking();
        int before = carried.getCount();
        for (int pass = 0; pass < 2 && carried.getCount() < carried.getMaxStackSize(); pass++) {
            for (int i = 0; i < backing.getSlots() && carried.getCount() < carried.getMaxStackSize(); i++) {
                if (context.isBackingVisible(i)) {
                    continue;
                }
                ItemStack stack = backing.getStackInSlot(i);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, carried)) {
                    continue;
                }
                if (pass == 0 && stack.getCount() >= stack.getMaxStackSize()) {
                    continue;
                }
                int room = carried.getMaxStackSize() - carried.getCount();
                ItemStack taken = backing.extractItem(i, Math.min(room, stack.getCount()), false);
                carried.grow(taken.getCount());
            }
        }
        if (carried.getCount() == before) {
            return false;
        }
        menu.setCarried(carried);
        return true;
    }
}
