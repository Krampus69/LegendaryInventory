package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.config.LIConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public final class ContainerContents {

    private static final String[] BUILTIN_PATHS = {"Items", "BlockEntityTag.Items"};
    private static final int MAX_DEPTH = 1;

    private ContainerContents() {}

    public static boolean enabled() {
        return LIConfig.COMMON.containerContentsCount.get();
    }

    public static int weightOf(ItemStack container) {
        return enabled() ? weightOf(container, 0) : 0;
    }

    private static int weightOf(ItemStack container, int depth) {
        if (container.isEmpty() || depth > MAX_DEPTH) {
            return 0;
        }
        IItemHandler handler = container.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler != null) {
            int total = 0;
            for (int i = 0; i < handler.getSlots(); i++) {
                total += stackWeight(handler.getStackInSlot(i), depth);
            }
            return total;
        }
        CompoundTag tag = container.getTag();
        if (tag == null) {
            return 0;
        }
        ListTag items = findItems(tag);
        if (items == null) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < items.size(); i++) {
            total += stackWeight(ItemStack.of(items.getCompound(i)), depth);
        }
        return total;
    }

    private static int stackWeight(ItemStack stack, int depth) {
        if (stack.isEmpty()) {
            return 0;
        }
        int weight = WeightTable.perItem(stack) * stack.getCount();
        if (depth < MAX_DEPTH) {
            weight += weightOf(stack, depth + 1);
        }
        return weight;
    }

    private static ListTag findItems(CompoundTag tag) {
        for (String path : BUILTIN_PATHS) {
            ListTag list = listAt(tag, path);
            if (list != null) {
                return list;
            }
        }
        List<? extends String> extra = LIConfig.COMMON.containerContentsPaths.get();
        for (String path : extra) {
            ListTag list = listAt(tag, path);
            if (list != null) {
                return list;
            }
        }
        return null;
    }

    private static ListTag listAt(CompoundTag tag, String path) {
        CompoundTag current = tag;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.contains(parts[i], Tag.TAG_COMPOUND)) {
                return null;
            }
            current = current.getCompound(parts[i]);
        }
        String last = parts[parts.length - 1];
        if (!current.contains(last, Tag.TAG_LIST)) {
            return null;
        }
        ListTag list = current.getList(last, Tag.TAG_COMPOUND);
        return list.isEmpty() ? null : list;
    }
}
