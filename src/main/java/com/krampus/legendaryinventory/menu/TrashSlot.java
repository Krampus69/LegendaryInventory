package com.krampus.legendaryinventory.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrashSlot extends Slot {

    private static final SimpleContainer DUMMY = new SimpleContainer(1);

    public TrashSlot(int x, int y) {
        super(DUMMY, 0, x, y);
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void set(ItemStack stack) {
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean hasItem() {
        return false;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize();
    }

    @Override
    public void setChanged() {
    }
}
