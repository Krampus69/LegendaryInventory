package com.krampus.legendaryinventory.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;

public final class RecipeSurfacer {

    private static final int MAIN_START = Inventory.getSelectionSize();
    private static final int MAIN_END = Inventory.INVENTORY_SIZE;

    private RecipeSurfacer() {}

    public static void surface(Player player, ExtendedInventory extended, Recipe<?> recipe, boolean maxTransfer) {
        Inventory inventory = player.getInventory();
        List<Ingredient> ingredients = new ArrayList<>();
        NonNullList<Ingredient> all = recipe.getIngredients();
        for (Ingredient ingredient : all) {
            if (!ingredient.isEmpty()) {
                ingredients.add(ingredient);
            }
        }
        for (Ingredient ingredient : ingredients) {
            int needed = 0;
            for (Ingredient other : ingredients) {
                if (other == ingredient) {
                    needed++;
                }
            }
            int stacksWanted = maxTransfer ? needed : 1;
            int have = countMatching(inventory, ingredient);
            int stacksMoved = 0;
            for (int i = 0; i < extended.getSlots() && (have < needed || stacksMoved < stacksWanted); i++) {
                ItemStack stack = extended.getStackInSlot(i);
                if (stack.isEmpty() || !ingredient.test(stack)) {
                    continue;
                }
                int target = freeMainSlot(inventory);
                if (target < 0) {
                    target = swappableMainSlot(inventory, ingredients);
                    if (target < 0) {
                        break;
                    }
                    ItemStack displaced = inventory.getItem(target);
                    inventory.setItem(target, stack);
                    extended.setStackInSlot(i, displaced);
                } else {
                    inventory.setItem(target, stack);
                    extended.setStackInSlot(i, ItemStack.EMPTY);
                }
                have += stack.getCount();
                stacksMoved++;
            }
        }
        inventory.setChanged();
    }

    private static int countMatching(Inventory inventory, Ingredient ingredient) {
        int count = 0;
        for (int i = 0; i < MAIN_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int freeMainSlot(Inventory inventory) {
        for (int i = MAIN_START; i < MAIN_END; i++) {
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static int swappableMainSlot(Inventory inventory, List<Ingredient> ingredients) {
        for (int i = MAIN_START; i < MAIN_END; i++) {
            ItemStack stack = inventory.getItem(i);
            boolean useful = false;
            for (Ingredient ingredient : ingredients) {
                if (ingredient.test(stack)) {
                    useful = true;
                    break;
                }
            }
            if (!useful) {
                return i;
            }
        }
        return -1;
    }
}
