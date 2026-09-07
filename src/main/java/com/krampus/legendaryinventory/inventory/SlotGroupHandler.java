package com.krampus.legendaryinventory.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SlotGroupHandler implements IItemHandlerModifiable {

    private final List<Slot> slots;

    private SlotGroupHandler(List<Slot> slots) {
        this.slots = slots;
    }

    public static List<SlotGroupHandler> containerGroups(AbstractContainerMenu menu, Inventory playerInventory) {
        Map<Container, List<Slot>> groups = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.getClass() != Slot.class) {
                continue;
            }
            Container container = slot.container;
            if (container == playerInventory || container instanceof CraftingContainer) {
                continue;
            }
            groups.computeIfAbsent(container, c -> new ArrayList<>()).add(slot);
        }

        List<SlotGroupHandler> out = new ArrayList<>();
        for (List<Slot> group : groups.values()) {
            if (group.size() > 1) {
                out.add(new SlotGroupHandler(group));
            }
        }
        return out;
    }

    @Override
    public int getSlots() {
        return slots.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots.get(slot).getItem();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        slots.get(slot).set(stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slots.get(slot).getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slots.get(slot).mayPlace(stack);
    }
}
