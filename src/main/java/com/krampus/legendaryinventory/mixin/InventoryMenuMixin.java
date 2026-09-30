package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.menu.HotbarMove;
import com.krampus.legendaryinventory.menu.LISlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(
        method = "quickMoveStack",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legendaryinventory$quickMoveExtra(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(index);
        if (!(slot instanceof LISlot li) || !li.isExtra()) {
            return;
        }
        if (!slot.hasItem()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (!HotbarMove.move(menu, stack)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        cir.setReturnValue(copy);
    }
}
