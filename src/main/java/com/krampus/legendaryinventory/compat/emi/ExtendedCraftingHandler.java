package com.krampus.legendaryinventory.compat.emi;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.RecipeTransferPacket;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;

public class ExtendedCraftingHandler<T extends RecipeBookMenu<?>> implements StandardRecipeHandler<T> {

    private static final int VANILLA_SLOTS = 36;

    private final int gridWidth;
    private final int gridHeight;

    public ExtendedCraftingHandler(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    private int gridSize() {
        return gridWidth * gridHeight;
    }

    @Override
    public List<Slot> getInputSources(T menu) {
        List<Slot> slots = new ArrayList<>(getCraftingSlots(menu));
        int start = gridSize() + 1;
        for (int i = start; i < start + VANILLA_SLOTS && i < menu.slots.size(); i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(T menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 1; i <= gridSize() && i < menu.slots.size(); i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }

    @Override
    public Slot getOutputSlot(T menu) {
        return menu.getSlot(0);
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        T menu = screen.getMenu();
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return StandardRecipeHandler.super.getInventory(screen);
        }
        List<EmiStack> stacks = new ArrayList<>();
        for (Slot slot : getCraftingSlots(menu)) {
            add(stacks, slot.getItem());
        }
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            add(stacks, inventory.getItem(i));
        }
        CombinedInventoryHandler backing = context.getBacking();
        for (int i = 0; i < backing.getSlots(); i++) {
            add(stacks, backing.getStackInSlot(i));
        }
        return new EmiPlayerInventory(stacks);
    }

    private static void add(List<EmiStack> stacks, ItemStack stack) {
        if (!stack.isEmpty()) {
            stacks.add(EmiStack.of(stack));
        }
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return backing(recipe) != null;
    }

    private CraftingRecipe backing(EmiRecipe recipe) {
        Recipe<?> backing = recipe.getBackingRecipe();
        if (backing instanceof CraftingRecipe crafting && crafting.canCraftInDimensions(gridWidth, gridHeight)) {
            return crafting;
        }
        return null;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        CraftingRecipe backing = backing(recipe);
        AbstractContainerMenu menu = context.getScreenHandler();
        if (backing == null || ScrollRegistry.get(menu) == null) {
            return StandardRecipeHandler.super.craft(recipe, context);
        }
        LINet.toServer(new RecipeTransferPacket(backing.getId(), context.getAmount() > 1));
        return true;
    }
}
