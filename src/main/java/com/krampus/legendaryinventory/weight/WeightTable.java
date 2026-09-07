package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.api.WeightHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WeightTable {

    private static int[] weights = new int[0];
    private static int generation = 0;

    private WeightTable() {}

    public static void rebuild() {
        WeightRules.Values rules = WeightRules.current();
        int size = BuiltInRegistries.ITEM.size();
        int[] table = new int[size];
        int overridden = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            int id = BuiltInRegistries.ITEM.getId(item);
            if (id < 0 || id >= size) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            Integer override = rules.overrides().get(key);
            if (override != null) {
                table[id] = override;
                overridden++;
                continue;
            }
            Integer preset = WeightHooks.getDefault(key);
            table[id] = preset != null ? preset : fallback(item, rules);
        }
        weights = table;
        generation++;
        LegendaryInventory.LOGGER.info("Built weight table for {} items, {} from weights.json", size, overridden);
    }

    private static int fallback(Item item, WeightRules.Values rules) {
        int max = item.getDefaultMaxStackSize();
        if (max >= 64) return rules.stackable();
        if (max > 1) return rules.semiStackable();
        return rules.unstackable();
    }

    public static int perItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int id = BuiltInRegistries.ITEM.getId(stack.getItem());
        int weight = id < 0 || id >= weights.length ? 1 : weights[id];
        return WeightHooks.hasProviders() ? WeightHooks.applyProviders(stack, weight) : weight;
    }

    public static int of(ItemStack stack) {
        return perItem(stack) * stack.getCount();
    }

    public static int generation() {
        return generation;
    }

    public static boolean isReady() {
        return weights.length > 0;
    }
}
