package com.krampus.legendaryinventory.api;

import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.weight.CapacityBonus;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.LIAttributes;
import com.krampus.legendaryinventory.weight.WeightTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class LegendaryInventoryApi {

    public static final String MOD_ID = "legendaryinventory";

    private LegendaryInventoryApi() {}

    @Nullable
    public static IExtendedInventory getExtendedInventory(Player player) {
        return LIAttachments.extended(player);
    }

    public static int getItemWeight(ItemStack stack) {
        return WeightTable.perItem(stack);
    }

    public static int getStackWeight(ItemStack stack) {
        return WeightTable.of(stack);
    }

    public static boolean isWeightTableReady() {
        return WeightTable.isReady();
    }

    public static int getCarriedWeight(Player player) {
        return CarryLoad.carriedWeight(player);
    }

    public static int getCapacity(Player player) {
        return CarryLoad.capacity(player);
    }

    public static Attribute getCapacityAttribute() {
        return LIAttributes.CARRY_CAPACITY.get();
    }

    public static int getCapacityBonus(Player player) {
        return CapacityBonus.get(player);
    }

    public static void setCapacityBonus(Player player, int bonus) {
        CapacityBonus.set(player, bonus);
    }

    public static boolean addCapacityBonus(Player player, int amount) {
        return CapacityBonus.add(player, amount);
    }

    public static void registerWeightProvider(IWeightProvider provider) {
        WeightHooks.addProvider(provider);
    }

    public static void setDefaultWeight(ResourceLocation item, int weight) {
        WeightHooks.setDefault(item, weight);
    }

    public static void allowAugmentedSlotClass(Class<? extends Slot> type) {
        WeightHooks.allowSlotClass(type);
    }

    public static void excludeMenuFromAugment(Class<? extends AbstractContainerMenu> type) {
        WeightHooks.excludeMenu(type);
    }

    public static boolean isAugmented(AbstractContainerMenu menu) {
        return ScrollRegistry.has(menu);
    }
}
