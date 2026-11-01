package com.krampus.legendaryinventory.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class HotbarMove {

    public static final int HOTBAR_START = 36;
    public static final int HOTBAR_END = 45;

    private HotbarMove() {}

    public static boolean move(AbstractContainerMenu menu, ItemStack stack) {
        int end = Math.min(HOTBAR_END, menu.slots.size());
        List<Slot> targets = new ArrayList<>(end - HOTBAR_START);
        for (int i = HOTBAR_START; i < end; i++) {
            targets.add(menu.slots.get(i));
        }
        return moveInto(targets, stack);
    }

    public static boolean toRows(AbstractContainerMenu menu, ItemStack stack) {
        List<Slot> targets = new ArrayList<>(ScrollContext.EXTRA_WINDOW + ScrollContext.BASE_WINDOW);
        List<Slot> main = new ArrayList<>(ScrollContext.BASE_WINDOW);
        for (Slot slot : menu.slots) {
            if (slot instanceof LISlot li) {
                (li.isExtra() ? targets : main).add(slot);
            }
        }
        targets.addAll(main);
        return moveInto(targets, stack);
    }

    private static boolean moveInto(List<Slot> targets, ItemStack stack) {
        boolean moved = false;
        for (int i = 0; i < targets.size() && !stack.isEmpty(); i++) {
            Slot slot = targets.get(i);
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
        for (int i = 0; i < targets.size() && !stack.isEmpty(); i++) {
            Slot slot = targets.get(i);
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
