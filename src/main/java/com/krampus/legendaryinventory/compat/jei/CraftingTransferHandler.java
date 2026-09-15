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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftingTransferHandler<C extends RecipeBookMenu<?, ?>> implements IRecipeTransferHandler<C, RecipeHolder<CraftingRecipe>> {

    private static WeakReference<Player> cacheOwner = new WeakReference<>(null);
    private static int cacheTick = -1;
    private static ItemStack[] cacheStacks = new ItemStack[0];
    private static int[] cacheCounts = new int[0];

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

        int[] counts = availableCounts(player, context.getBacking());
        ItemStack[] stacks = cacheStacks;
        List<IRecipeSlotView> missing = new ArrayList<>();
        for (IRecipeSlotView view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            if (view.isEmpty()) {
                continue;
            }
            if (!claim(view, stacks, counts)) {
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

    private static int[] availableCounts(Player player, CombinedInventoryHandler backing) {
        if (cacheOwner.get() != player || cacheTick != player.tickCount) {
            rebuildCache(player, backing);
        }
        return cacheCounts.clone();
    }

    private static void rebuildCache(Player player, CombinedInventoryHandler backing) {
        Inventory inventory = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        for (int i = 0; i < backing.getSlots(); i++) {
            ItemStack stack = backing.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        cacheStacks = stacks.toArray(new ItemStack[0]);
        cacheCounts = new int[cacheStacks.length];
        for (int i = 0; i < cacheStacks.length; i++) {
            cacheCounts[i] = cacheStacks[i].getCount();
        }
        cacheOwner = new WeakReference<>(player);
        cacheTick = player.tickCount;
    }

    private static boolean claim(IRecipeSlotView view, ItemStack[] stacks, int[] counts) {
        List<ItemStack> options = view.getItemStacks().toList();
        for (ItemStack option : options) {
            for (int i = 0; i < stacks.length; i++) {
                if (counts[i] > 0 && ItemStack.isSameItemSameComponents(stacks[i], option)) {
                    counts[i]--;
                    return true;
                }
            }
        }
        for (ItemStack option : options) {
            for (int i = 0; i < stacks.length; i++) {
                if (counts[i] > 0 && stacks[i].is(option.getItem())) {
                    counts[i]--;
                    return true;
                }
            }
        }
        return false;
    }
}
