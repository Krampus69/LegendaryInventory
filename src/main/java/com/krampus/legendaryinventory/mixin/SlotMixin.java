package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.menu.CoveredSlots;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "isActive", at = @At("RETURN"), cancellable = true)
    private void legendaryinventory$hideCovered(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && CoveredSlots.isHidden((Slot) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
