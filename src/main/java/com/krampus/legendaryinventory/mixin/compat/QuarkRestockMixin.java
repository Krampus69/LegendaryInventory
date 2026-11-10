package com.krampus.legendaryinventory.mixin.compat;

import com.krampus.legendaryinventory.compat.QuarkCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.violetmoon.quark.base.handler.InventoryTransferHandler;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.base.handler.InventoryTransferHandler$Restock", remap = false)
public abstract class QuarkRestockMixin {

    @Inject(method = "transfer", at = @At("RETURN"), require = 0)
    private void legendaryinventory$restockRows(InventoryTransferHandler.TransferPredicate predicate, CallbackInfo ci) {
        if (!QuarkCompat.transferActive()) {
            return;
        }
        try {
            InventoryTransferHandler.Transfer self = (InventoryTransferHandler.Transfer) (Object) this;
            if (!self.itemHandlers.isEmpty()) {
                QuarkCompat.restockRows(self.player, self.itemHandlers.get(0).getLeft(), predicate);
            }
        } catch (LinkageError e) {
            QuarkCompat.disableTransfer(e);
        }
    }
}
