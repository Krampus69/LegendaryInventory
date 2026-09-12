package com.krampus.legendaryinventory.compat.jei;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.RecipeTransferPacket;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftingTransferHandler<C extends RecipeBookMenu<?, ?>> implements IRecipeTransferHandler<C, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<C, RecipeHolder<CraftingRecipe>> fallback;
    private final Class<? extends C> containerClass;
    @Nullable
    private final MenuType<C> menuType;
    private final int gridWidth;
    private final int gridHeight;

    public CraftingTransferHandler(IRecipeTransferHandlerHelper helper,
                                   IRecipeTransferHandler<C, RecipeHolder<CraftingRecipe>> fallback,
                                   Class<? extends C> containerClass,
                                   @Nullable MenuType<C> menuType,
                                   int gridWidth, int gridHeight) {
        this.helper = helper;
        this.fallback = fallback;
        this.containerClass = containerClass;
        this.menuType = menuType;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return containerClass;
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return Optional.ofNullable(menuType);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(C menu, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView slots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return fallback.transferRecipe(menu, recipe, slots, player, maxTransfer, doTransfer);
        }
        if (!recipe.value().canCraftInDimensions(gridWidth, gridHeight)) {
            return helper.createUserErrorWithTooltip(
                Component.translatable("jei.tooltip.error.recipe.transfer.too.large.player.inventory"));
        }

        List<ItemStack> available = collectAvailable(player.getInventory(), context.getBacking());
        List<IRecipeSlotView> missing = new ArrayList<>();
        for (IRecipeSlotView view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            if (view.isEmpty()) {
                continue;
            }
            if (!claim(view, available)) {
                missing.add(view);
            }
        }
        if (!missing.isEmpty()) {
            return helper.createUserErrorForMissingSlots(
                Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missing);
        }
        if (doTransfer) {
            LINet.toServer(new RecipeTransferPacket(recipe.id(), maxTransfer));
        }
        return null;
    }

    private static List<ItemStack> collectAvailable(Inventory inventory, CombinedInventoryHandler backing) {
        List<ItemStack> available = new ArrayList<>();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        for (int i = 0; i < backing.getSlots(); i++) {
            ItemStack stack = backing.getStackInSlot(i);
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }
        return available;
    }

    private static boolean claim(IRecipeSlotView view, List<ItemStack> available) {
        List<ItemStack> options = view.getItemStacks().toList();
        for (ItemStack option : options) {
            for (ItemStack stack : available) {
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, option)) {
                    stack.shrink(1);
                    return true;
                }
            }
        }
        for (ItemStack option : options) {
            for (ItemStack stack : available) {
                if (!stack.isEmpty() && stack.is(option.getItem())) {
                    stack.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }
}
