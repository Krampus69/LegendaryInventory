package com.krampus.legendaryinventory.compat.jei;

import com.krampus.legendaryinventory.LegendaryInventory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;

@JeiPlugin
public class LIJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();

        registration.addRecipeTransferHandler(new CraftingTransferHandler<>(
            helper,
            helper.createUnregisteredRecipeTransferHandler(helper.createBasicRecipeTransferInfo(
                CraftingMenu.class, MenuType.CRAFTING, RecipeTypes.CRAFTING, 1, 9, 10, 36)),
            CraftingMenu.class, MenuType.CRAFTING, 3, 3
        ), RecipeTypes.CRAFTING);

        registration.addRecipeTransferHandler(new CraftingTransferHandler<>(
            helper,
            helper.createUnregisteredRecipeTransferHandler(helper.createBasicRecipeTransferInfo(
                InventoryMenu.class, null, RecipeTypes.CRAFTING, 1, 4, 5, 36)),
            InventoryMenu.class, null, 2, 2
        ), RecipeTypes.CRAFTING);
    }
}
