package com.krampus.legendaryinventory.inventory;

import com.krampus.legendaryinventory.api.IExtendedInventory;
import com.krampus.legendaryinventory.weight.WeightTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.BitSet;

public class ExtendedInventory extends ItemStackHandler implements IExtendedInventory {

    public static final int ROWS = 50;
    public static final int COLS = 9;
    public static final int SIZE = ROWS * COLS;

    private final BitSet changed = new BitSet(SIZE);

    private int cachedWeight = -1;
    private int cachedGeneration = -1;

    public ExtendedInventory() {
        super(SIZE);
    }

    @Override
    protected void onContentsChanged(int slot) {
        cachedWeight = -1;
        if (slot >= 0 && slot < SIZE) {
            changed.set(slot);
        }
    }

    public int totalWeight() {
        if (!WeightTable.isReady()) {
            return 0;
        }
        int gen = WeightTable.generation();
        if (cachedWeight < 0 || cachedGeneration != gen) {
            int sum = 0;
            for (int i = 0; i < getSlots(); i++) {
                sum += WeightTable.of(getStackInSlot(i));
            }
            cachedWeight = sum;
            cachedGeneration = gen;
        }
        return cachedWeight;
    }

    public boolean hasChanges() {
        return !changed.isEmpty();
    }

    public BitSet takeChanges() {
        BitSet copy = (BitSet) changed.clone();
        changed.clear();
        return copy;
    }

    public void markChanged(int slot) {
        onContentsChanged(slot);
    }

    public void clearChanges() {
        changed.clear();
    }

    public int usedSlots() {
        int used = 0;
        for (int i = 0; i < getSlots(); i++) {
            if (!getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    public void clearAll() {
        for (int i = 0; i < getSlots(); i++) {
            setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setSize(SIZE);
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < SIZE) {
                stacks.set(slot, ItemStack.of(entry));
            }
        }
        changed.clear();
        cachedWeight = -1;
        onLoad();
    }
}
