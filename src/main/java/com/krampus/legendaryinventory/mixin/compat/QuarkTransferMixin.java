package com.krampus.legendaryinventory.mixin.compat;

import com.krampus.legendaryinventory.compat.QuarkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.violetmoon.quark.base.handler.InventoryTransferHandler;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.base.handler.InventoryTransferHandler$Transfer", remap = false)
public abstract class QuarkTransferMixin {

    @Inject(method = "transfer", at = @At("RETURN"), require = 0)
    private void legendaryinventory$depositRows(InventoryTransferHandler.TransferPredicate predicate, CallbackInfo ci) {
        if (!QuarkCompat.transferActive()) {
            return;
        }
        try {
            InventoryTransferHandler.Transfer self = (InventoryTransferHandler.Transfer) (Object) this;
            QuarkCompat.depositRows(self.player, stack -> self.insert(stack, predicate));
        } catch (LinkageError e) {
            QuarkCompat.disableTransfer(e);
        }
    }
}
