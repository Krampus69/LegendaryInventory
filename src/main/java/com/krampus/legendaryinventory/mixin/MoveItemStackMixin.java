package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class MoveItemStackMixin {

    @Inject(
        method = "moveItemStackTo",
        at = @At("RETURN"),
        cancellable = true
    )
    private void legendaryinventory$spillIntoBacking(
            ItemStack stack, int startIndex, int endIndex, boolean reverse,
            CallbackInfoReturnable<Boolean> cir) {

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
