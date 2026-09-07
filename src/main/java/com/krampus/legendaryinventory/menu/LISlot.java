package com.krampus.legendaryinventory.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public class LISlot extends Slot {

    private static final SimpleContainer DUMMY = new SimpleContainer(0);

    private final ScrollContext context;
    private final int window;
    private boolean renderHidden = false;

    public LISlot(ScrollContext context, int window, int x, int y) {
        super(DUMMY, window, x, y);
        this.context = context;
        this.window = window;
    }

    public ScrollContext context() {
        return context;
    }

    public int window() {
        return window;
    }

    public int backingIndex() {
        return context.backingIndexFor(window);
    }

    private IItemHandlerModifiable handler() {
        return context.getBacking();
    }

    @Override
    public ItemStack getItem() {
        int i = backingIndex();
        return i < 0 ? ItemStack.EMPTY : handler().getStackInSlot(i);
    }

    @Override
    public void set(ItemStack stack) {
        int i = context.writeIndexFor(window);
        if (i >= 0) {
            handler().setStackInSlot(i, stack);
        } else if (!stack.isEmpty()) {
            rescue(stack);
        }
        setChanged();
    }

    private void rescue(ItemStack stack) {
        ItemStack left = ItemHandlerHelper.insertItem(handler(), stack, false);
        if (!left.isEmpty()) {
            Player owner = context.getOwner();
            if (!owner.level().isClientSide()) {
                owner.drop(left, false);
            }
        }
    }

    @Override
    public ItemStack remove(int amount) {
        int i = context.writeIndexFor(window);
        return i < 0 ? ItemStack.EMPTY : handler().extractItem(i, amount, false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        int i = backingIndex();
        return !stack.isEmpty() && i >= 0 && handler().isItemValid(i, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        int i = backingIndex();
        return i >= 0 && !handler().extractItem(i, 1, true).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        int i = backingIndex();
        return i < 0 ? 64 : handler().getSlotLimit(i);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    public void setRenderHidden(boolean hidden) {
        this.renderHidden = hidden;
    }

    @Override
    public boolean isActive() {
        return backingIndex() >= 0 && !renderHidden;
    }

    @Override
    public void setChanged() {
        context.onSlotContentsChanged();
    }
}
