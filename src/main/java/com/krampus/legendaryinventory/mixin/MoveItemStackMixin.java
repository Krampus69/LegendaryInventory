package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class MoveItemStackMixin {

    @Unique
    private boolean legendaryinventory$mergedFirst;

    @Inject(
        method = "moveItemStackTo",
        at = @At("HEAD")
    )
    private void legendaryinventory$mergeIntoBacking(
            ItemStack stack, int startIndex, int endIndex, boolean reverse,
            CallbackInfoReturnable<Boolean> cir) {

        legendaryinventory$mergedFirst = false;
        if (stack.isEmpty() || !stack.isStackable()) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null || !legendaryinventory$rangeCoversWindow(menu, startIndex, endIndex)) {
            return;
        }

        IItemHandler backing = context.getBacking();
        for (int i = 0; i < backing.getSlots() && !stack.isEmpty(); i++) {
            ItemStack present = backing.getStackInSlot(i);
            if (present == stack || present.isEmpty() || !ItemStack.isSameItemSameComponents(present, stack)) {
                continue;
            }
            int before = stack.getCount();
            int after = backing.insertItem(i, stack.copy(), false).getCount();
            if (after != before) {
                stack.setCount(after);
                legendaryinventory$mergedFirst = true;
            }
        }
    }

    @Inject(
        method = "moveItemStackTo",
        at = @At("RETURN"),
        cancellable = true
    )
    private void legendaryinventory$spillIntoBacking(
            ItemStack stack, int startIndex, int endIndex, boolean reverse,
            CallbackInfoReturnable<Boolean> cir) {

        if (legendaryinventory$mergedFirst) {
            legendaryinventory$mergedFirst = false;
            cir.setReturnValue(true);
        }
        if (stack.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return;
        }
        if (!legendaryinventory$rangeCoversWindow(menu, startIndex, endIndex)) {
            return;
        }

        int before = stack.getCount();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
            context.getBacking(), stack.copy(), false);
        int after = remainder.getCount();
        if (after == before) {
            return;
        }

        stack.setCount(after);
        cir.setReturnValue(true);
    }

    @Unique
    private static boolean legendaryinventory$rangeCoversWindow(
            AbstractContainerMenu menu, int startIndex, int endIndex) {
        int from = Math.max(0, Math.min(startIndex, endIndex));
        int to = Math.min(menu.slots.size(), Math.max(startIndex, endIndex));
        for (int i = from; i < to; i++) {
            Slot slot = menu.slots.get(i);
            if (slot instanceof LISlot) {
                return true;
            }
        }
        return false;
    }
}
