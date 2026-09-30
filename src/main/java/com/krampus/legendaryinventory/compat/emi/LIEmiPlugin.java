package com.krampus.legendaryinventory.compat.emi;

import com.krampus.legendaryinventory.LegendaryInventory;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.List;
import java.util.Map;

@EmiEntrypoint
public class LIEmiPlugin implements EmiPlugin {

    private static final String FILLER_CLASS = "dev.emi.emi.registry.EmiRecipeFiller";

    @Override
    public void register(EmiRegistry registry) {
        ExtendedCraftingHandler<InventoryMenu> inventory = new ExtendedCraftingHandler<>(2, 2);
        ExtendedCraftingHandler<CraftingMenu> crafting = new ExtendedCraftingHandler<>(3, 3);
        registry.addRecipeHandler(null, inventory);
        registry.addRecipeHandler(MenuType.CRAFTING, crafting);
        moveToFront(null, inventory);
        moveToFront(MenuType.CRAFTING, crafting);
    }

    @SuppressWarnings("unchecked")
    private static void moveToFront(MenuType<?> type, EmiRecipeHandler<?> handler) {
        try {
            Object map = Class.forName(FILLER_CLASS).getField("handlers").get(null);
            List<EmiRecipeHandler<?>> list = ((Map<MenuType<?>, List<EmiRecipeHandler<?>>>) map).get(type);
            if (list != null && list.remove(handler)) {
                list.add(0, handler);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegendaryInventory.LOGGER.warn("Could not prioritise the EMI recipe handler, craftables will only see visible slots", e);
        }
    }
}
