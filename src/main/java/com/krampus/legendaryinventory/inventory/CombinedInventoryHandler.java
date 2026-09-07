package com.krampus.legendaryinventory.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class CombinedInventoryHandler implements IItemHandlerModifiable {

    public static final int MAIN_OFFSET = 9;
    public static final int MAIN_COUNT = 27;

    private final Inventory inventory;
    private final ExtendedInventory extended;

    public CombinedInventoryHandler(Inventory inventory, ExtendedInventory extended) {
        this.inventory = inventory;
        this.extended = extended;
    }

    @Override
    public int getSlots() {
        return MAIN_COUNT + extended.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            return ItemStack.EMPTY;
        }
        return slot < MAIN_COUNT
            ? inventory.getItem(MAIN_OFFSET + slot)
            : extended.getStackInSlot(slot - MAIN_COUNT);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= getSlots()) {
            return;
        }
        if (slot < MAIN_COUNT) {
            inventory.setItem(MAIN_OFFSET + slot, stack);
            inventory.setChanged();
        } else {
            extended.setStackInSlot(slot - MAIN_COUNT, stack);
        }
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= getSlots()) {
            return stack;
        }
        if (slot >= MAIN_COUNT) {
            return extended.insertItem(slot - MAIN_COUNT, stack, simulate);
        }

        ItemStack existing = inventory.getItem(MAIN_OFFSET + slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());

        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }

        boolean overflow = stack.getCount() > limit;
        int moved = overflow ? limit : stack.getCount();

        if (!simulate) {
            if (existing.isEmpty()) {
                inventory.setItem(MAIN_OFFSET + slot, stack.copyWithCount(moved));
            } else {
                existing.grow(moved);
            }
            inventory.setChanged();
        }

        return overflow ? stack.copyWithCount(stack.getCount() - moved) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= getSlots()) {
            return ItemStack.EMPTY;
        }
        if (slot >= MAIN_COUNT) {
            return extended.extractItem(slot - MAIN_COUNT, amount, simulate);
        }

        ItemStack existing = inventory.getItem(MAIN_OFFSET + slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int taken = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= taken) {
            if (simulate) {
                return existing.copy();
            }
            inventory.setItem(MAIN_OFFSET + slot, ItemStack.EMPTY);
            inventory.setChanged();
            return existing;
        }

        if (!simulate) {
            inventory.setItem(MAIN_OFFSET + slot, existing.copyWithCount(existing.getCount() - taken));
            inventory.setChanged();
        }
        return existing.copyWithCount(taken);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot < MAIN_COUNT ? inventory.getMaxStackSize() : extended.getSlotLimit(slot - MAIN_COUNT);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot < MAIN_COUNT || extended.isItemValid(slot - MAIN_COUNT, stack);
    }
}
