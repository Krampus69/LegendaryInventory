package com.krampus.legendaryinventory.mixin.client;

import com.krampus.legendaryinventory.client.InventoryExpansion;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(
        method = "renderLabels",
        at = @At("HEAD"),
        cancellable = true
    )
    private void legendaryinventory$hideLabels(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        if (InventoryExpansion.isExpanded((InventoryScreen) (Object) this)) {
            ci.cancel();
        }
    }
}
