package com.krampus.legendaryinventory.menu;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CoveredSlot extends Slot {

    private final Slot delegate;
    private final ScrollContext context;

    public CoveredSlot(Slot delegate, ScrollContext context) {
        super(delegate.container, delegate.getSlotIndex(), delegate.x, delegate.y);
        this.delegate = delegate;
        this.context = context;
        this.index = delegate.index;
    }

    public Slot delegate() {
        return delegate;
    }

    @Override
    public boolean isActive() {
        return !context.isExpanded() && delegate.isActive();
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
        delegate.onQuickCraft(oldStack, newStack);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        delegate.onTake(player, stack);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return delegate.mayPlace(stack);
    }

    @Override
    public ItemStack getItem() {
        return delegate.getItem();
    }

    @Override
    public boolean hasItem() {
        return delegate.hasItem();
    }

    @Override
    public void setByPlayer(ItemStack stack) {
        delegate.setByPlayer(stack);
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        delegate.setByPlayer(newStack, oldStack);
    }

    @Override
    public void set(ItemStack stack) {
        delegate.set(stack);
    }

    @Override
    public void setChanged() {
        delegate.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return delegate.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return delegate.getMaxStackSize(stack);
    }

    @Override
    @Nullable
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return delegate.getNoItemIcon();
    }

    @Override
    public ItemStack remove(int amount) {
        return delegate.remove(amount);
    }

    @Override
    public boolean mayPickup(Player player) {
        return delegate.mayPickup(player);
    }

    @Override
    public Optional<ItemStack> tryRemove(int count, int decrement, Player player) {
        return delegate.tryRemove(count, decrement, player);
    }

    @Override
    public ItemStack safeTake(int count, int decrement, Player player) {
        return delegate.safeTake(count, decrement, player);
    }

    @Override
    public ItemStack safeInsert(ItemStack stack) {
        return delegate.safeInsert(stack);
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        return delegate.safeInsert(stack, increment);
    }

    @Override
    public boolean allowModification(Player player) {
        return delegate.allowModification(player);
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return delegate.isSameInventory(other instanceof CoveredSlot covered ? covered.delegate : other);
    }

    @Override
    public int getSlotIndex() {
        return delegate.getSlotIndex();
    }
}
