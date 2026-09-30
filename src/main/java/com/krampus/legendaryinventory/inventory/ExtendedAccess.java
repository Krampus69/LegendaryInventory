package com.krampus.legendaryinventory.inventory;

import com.krampus.legendaryinventory.item.LIItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Set;
import java.util.function.Predicate;

public final class ExtendedAccess {

    private ExtendedAccess() {}

    public static ExtendedInventory of(Player player) {
        if (player == null) {
            return null;
        }
        return player.getCapability(LICaps.EXTENDED).orElse(null);
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(LIItems.SACK.get());
    }

    public static int mergeIntoExisting(ExtendedInventory ext, ItemStack stack) {
        int moved = 0;
        for (int i = 0; i < ext.getSlots() && !stack.isEmpty(); i++) {
            ItemStack present = ext.getStackInSlot(i);
            if (present.isEmpty() || !ItemStack.isSameItemSameTags(present, stack)) {
                continue;
            }
            int limit = Math.min(ext.getSlotLimit(i), present.getMaxStackSize());
            int room = limit - present.getCount();
            if (room <= 0) {
                continue;
            }
            int take = Math.min(room, stack.getCount());
            present.grow(take);
            stack.shrink(take);
            ext.markChanged(i);
            moved += take;
        }
        return moved;
    }

    public static int insert(ExtendedInventory ext, ItemStack stack) {
        int before = stack.getCount();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(ext, stack.copy(), false);
        stack.setCount(remainder.getCount());
        return before - stack.getCount();
    }

    public static int partialRoomIn(net.minecraft.world.entity.player.Inventory inventory, ItemStack stack) {
        int room = 0;
        for (ItemStack present : inventory.items) {
            if (present.isEmpty() || !ItemStack.isSameItemSameTags(present, stack)) {
                continue;
            }
            int limit = Math.min(inventory.getMaxStackSize(), present.getMaxStackSize());
            room += Math.max(0, limit - present.getCount());
        }
        return room;
    }

    public static boolean contains(ExtendedInventory ext, ItemStack stack) {
        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack present = ext.getStackInSlot(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameTags(present, stack)) {
                return true;
            }
        }
        return false;
    }

    public static int count(ExtendedInventory ext, Item item) {
        int total = 0;
        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack present = ext.getStackInSlot(i);
            if (!present.isEmpty() && present.is(item)) {
                total += present.getCount();
            }
        }
        return total;
    }

    public static boolean hasAnyOf(ExtendedInventory ext, Set<Item> items) {
        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack present = ext.getStackInSlot(i);
            if (!present.isEmpty() && items.contains(present.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnyMatching(ExtendedInventory ext, Predicate<ItemStack> predicate) {
        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack present = ext.getStackInSlot(i);
            if (!present.isEmpty() && predicate.test(present)) {
                return true;
            }
        }
        return false;
    }

    public static int findMatching(ExtendedInventory ext, ItemStack stack) {
        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack present = ext.getStackInSlot(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameTags(present, stack)) {
                return i;
            }
        }
        return -1;
    }
}
