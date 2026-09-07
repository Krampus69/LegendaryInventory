package com.krampus.legendaryinventory.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WeightHooks {

    private static final List<IWeightProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, Integer> DEFAULTS = new ConcurrentHashMap<>();
    private static final Set<Class<? extends Slot>> SLOT_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<Class<? extends AbstractContainerMenu>> EXCLUDED_MENUS = ConcurrentHashMap.newKeySet();

    private WeightHooks() {}

    public static void addProvider(IWeightProvider provider) {
        PROVIDERS.add(provider);
    }

    public static int applyProviders(ItemStack stack, int weight) {
        for (IWeightProvider provider : PROVIDERS) {
            int result = provider.getWeight(stack, weight);
            if (result >= 0) {
                weight = result;
            }
        }
        return weight;
    }

    public static boolean hasProviders() {
        return !PROVIDERS.isEmpty();
    }

    public static void setDefault(ResourceLocation item, int weight) {
        DEFAULTS.put(item, Math.max(0, weight));
    }

    public static Integer getDefault(ResourceLocation item) {
        return DEFAULTS.get(item);
    }

    public static void allowSlotClass(Class<? extends Slot> type) {
        SLOT_CLASSES.add(type);
    }

    public static boolean isSlotClassAllowed(Class<?> type) {
        return type == Slot.class || SLOT_CLASSES.contains(type);
    }

    public static void excludeMenu(Class<? extends AbstractContainerMenu> type) {
        EXCLUDED_MENUS.add(type);
    }

    public static boolean isMenuExcluded(AbstractContainerMenu menu) {
        for (Class<? extends AbstractContainerMenu> type : EXCLUDED_MENUS) {
            if (type.isInstance(menu)) {
                return true;
            }
        }
        return false;
    }
}
