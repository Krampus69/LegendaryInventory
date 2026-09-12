package com.krampus.legendaryinventory.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public final class RecipePlacer {

    private RecipePlacer() {}

    public static void place(ServerPlayer player, RecipeBookMenu<?> menu, ExtendedInventory extended,
                             CraftingRecipe recipe, boolean placeAll) {
        int gridWidth = menu.getGridWidth();
        int gridHeight = menu.getGridHeight();
        int resultIndex = menu.getResultSlotIndex();
        int gridStart = resultIndex + 1;
        int gridCount = gridWidth * gridHeight;

        clearGrid(player, menu, gridStart, gridCount);

        List<Ingredient> ingredients = new ArrayList<>();
        List<Integer> targets = new ArrayList<>();
        layout(recipe, gridWidth, gridStart, ingredients, targets);

        int crafts = 1;
        if (placeAll) {
            crafts = Integer.MAX_VALUE;
            for (int i = 0; i < ingredients.size(); i++) {
                int available = countMatching(player.getInventory(), extended, ingredients.get(i), ingredients);
                crafts = Math.min(crafts, available);
            }
            crafts = Math.max(1, Math.min(crafts, 64));
        }

        for (int i = 0; i < ingredients.size(); i++) {
            Slot slot = menu.getSlot(targets.get(i));
            ItemStack placed = ItemStack.EMPTY;
            int wanted = crafts;
            while (wanted > 0) {
                ItemStack taken = takeMatching(player.getInventory(), extended, ingredients.get(i), placed, wanted);
                if (taken.isEmpty()) {
                    break;
                }
                if (placed.isEmpty()) {
                    placed = taken;
                } else {
                    placed.grow(taken.getCount());
                }
                wanted -= taken.getCount();
                if (placed.getCount() >= placed.getMaxStackSize()) {
                    break;
                }
            }
            if (!placed.isEmpty()) {
                slot.set(placed);
            }
        }
        player.getInventory().setChanged();
    }

    private static void clearGrid(ServerPlayer player, RecipeBookMenu<?> menu, int gridStart, int gridCount) {
        for (int i = gridStart; i < gridStart + gridCount; i++) {
            Slot slot = menu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            slot.set(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    private static void layout(CraftingRecipe recipe, int gridWidth, int gridStart,
                               List<Ingredient> ingredients, List<Integer> targets) {
        NonNullList<Ingredient> all = recipe.getIngredients();
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            for (int i = 0; i < all.size(); i++) {
                Ingredient ingredient = all.get(i);
                if (ingredient.isEmpty()) {
                    continue;
                }
                int x = i % width;
                int y = i / width;
                ingredients.add(ingredient);
                targets.add(gridStart + y * gridWidth + x);
            }
            return;
        }
        int at = 0;
        for (Ingredient ingredient : all) {
            if (ingredient.isEmpty()) {
                continue;
            }
            ingredients.add(ingredient);
            targets.add(gridStart + at++);
        }
    }

    private static int countMatching(Inventory inventory, ExtendedInventory extended, Ingredient ingredient,
                                     List<Ingredient> all) {
        int total = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        for (int i = 0; i < extended.getSlots(); i++) {
            ItemStack stack = extended.getStackInSlot(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        int sharing = 0;
        for (Ingredient other : all) {
            if (other == ingredient || sameOptions(other, ingredient)) {
                sharing++;
            }
        }
        return total / Math.max(1, sharing);
    }

    private static boolean sameOptions(Ingredient a, Ingredient b) {
        ItemStack[] left = a.getItems();
        ItemStack[] right = b.getItems();
        if (left.length != right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            if (!ItemStack.isSameItemSameTags(left[i], right[i])) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack takeMatching(Inventory inventory, ExtendedInventory extended, Ingredient ingredient,
                                          ItemStack like, int max) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack, ingredient, like)) {
                int n = Math.min(max, stack.getCount());
                ItemStack taken = stack.copyWithCount(n);
                stack.shrink(n);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return taken;
            }
        }
        for (int i = 0; i < extended.getSlots(); i++) {
            ItemStack stack = extended.getStackInSlot(i);
            if (matches(stack, ingredient, like)) {
                int n = Math.min(max, stack.getCount());
                return extended.extractItem(i, n, false);
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean matches(ItemStack stack, Ingredient ingredient, ItemStack like) {
        if (stack.isEmpty() || !ingredient.test(stack)) {
            return false;
        }
        return like.isEmpty() || ItemStack.isSameItemSameTags(stack, like);
    }
}
