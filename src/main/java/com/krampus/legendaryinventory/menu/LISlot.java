package com.krampus.legendaryinventory.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public class LISlot extends Slot {

    private static final SimpleContainer DUMMY = new SimpleContainer(0);
    public static final int MAIN_FIRST = 9;
    public static final int EXTRA_FIRST = 100;

    private final ScrollContext context;
    private final int baseWindow;
    private final boolean extra;
    private boolean renderHidden = false;

    public LISlot(ScrollContext context, int window, int x, int y) {
        this(context, window, false, x, y);
    }

    public LISlot(ScrollContext context, int baseWindow, boolean extra, int x, int y) {
        this(DUMMY, baseWindow, context, baseWindow, extra, x, y);
    }

    public LISlot(Container container, int slotIndex, ScrollContext context, int baseWindow, boolean extra, int x, int y) {
        super(container, slotIndex, x, y);
        this.context = context;
        this.baseWindow = baseWindow;
        this.extra = extra;
    }

    public ScrollContext context() {
        return context;
    }

    public boolean isExtra() {
        return extra;
    }

    public int window() {
        return context.windowOf(baseWindow, extra);
    }

    public int backingIndex() {
        return context.backingIndexFor(window());
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
        boolean remote = context.isRemoteWrite();
        int i = remote ? context.remoteWriteIndexFor(baseWindow, extra) : context.writeIndexFor(window());
        if (i >= 0) {
            handler().setStackInSlot(i, stack);
        } else if (!stack.isEmpty() && !remote) {
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
        int i = context.writeIndexFor(window());
        return i < 0 ? ItemStack.EMPTY : handler().extractItem(i, amount, false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        int i = backingIndex();
        if (i < 0) {
            return false;
        }
        return stack.isEmpty() || handler().isItemValid(i, stack);
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
        int i = context.writeIndexFor(window());
        if (i >= 0) {
            context.getBacking().markChanged(i);
        }
        context.onSlotContentsChanged();
    }
}
