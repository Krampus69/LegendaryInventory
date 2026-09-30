package com.krampus.legendaryinventory.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class HotbarMove {

    public static final int HOTBAR_START = 36;
    public static final int HOTBAR_END = 45;

    private HotbarMove() {}

    public static boolean move(AbstractContainerMenu menu, ItemStack stack) {
        boolean moved = false;
        int end = Math.min(HOTBAR_END, menu.slots.size());
        for (int i = HOTBAR_START; i < end && !stack.isEmpty(); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack present = slot.getItem();
            if (present.isEmpty() || !ItemStack.isSameItemSameTags(present, stack)) {
                continue;
            }
            int room = Math.min(slot.getMaxStackSize(present), present.getMaxStackSize()) - present.getCount();
            if (room <= 0) {
                continue;
            }
            int take = Math.min(room, stack.getCount());
            present.grow(take);
            stack.shrink(take);
            slot.setChanged();
            moved = true;
        }
        for (int i = HOTBAR_START; i < end && !stack.isEmpty(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() || !slot.mayPlace(stack)) {
                continue;
            }
            int take = Math.min(slot.getMaxStackSize(stack), stack.getCount());
            slot.setByPlayer(stack.split(take));
            moved = true;
        }
        return moved;
    }
}
