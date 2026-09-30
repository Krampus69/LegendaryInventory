package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Inventory.class, priority = 1500)
public abstract class InventoryMixin {

    @Shadow @Final public Player player;

    @Unique
    private int legendaryinventory$moved;

    @Unique
    private ExtendedInventory legendaryinventory$serverExtended() {
        if (player == null || player.level().isClientSide()) {
            return null;
        }
        return ExtendedAccess.of(player);
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void legendaryinventory$mergeBefore(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        legendaryinventory$moved = 0;
        if (slot != -1 || !ExtendedAccess.accepts(stack)) {
            return;
        }
        ExtendedInventory ext = legendaryinventory$serverExtended();
        if (ext == null) {
            return;
        }
        int keep = Math.min(stack.getCount(), ExtendedAccess.partialRoomIn((Inventory) (Object) this, stack));
        int surplus = stack.getCount() - keep;
        if (surplus <= 0) {
            return;
        }
        ItemStack portion = stack.copyWithCount(surplus);
        int moved = ExtendedAccess.mergeIntoExisting(ext, portion);
        if (moved > 0) {
            stack.shrink(moved);
            legendaryinventory$moved += moved;
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void legendaryinventory$spillAfter(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ExtendedInventory ext = legendaryinventory$serverExtended();
        if (ext == null) {
            return;
        }
        if (!stack.isEmpty() && ExtendedAccess.accepts(stack)) {
            int moved = ExtendedAccess.insert(ext, stack);
            if (moved > 0) {
                legendaryinventory$moved += moved;
            }
        }
        if (stack.isEmpty() && legendaryinventory$moved > 0) {
            cir.setReturnValue(true);
        }
        legendaryinventory$moved = 0;
    }

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void legendaryinventory$contains(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext != null && ExtendedAccess.contains(ext, stack)) {
            cir.setReturnValue(true);
        }
    }
}
