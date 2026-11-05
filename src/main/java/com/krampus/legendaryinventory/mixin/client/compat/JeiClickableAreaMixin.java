package com.krampus.legendaryinventory.mixin.client.compat;

import com.krampus.legendaryinventory.client.InventoryExpansion;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Pseudo
@Mixin(targets = "mezz.jei.library.gui.GuiContainerHandlers", remap = false)
public abstract class JeiClickableAreaMixin {

    @Inject(method = "getGuiClickableArea", at = @At("RETURN"), cancellable = true, require = 0)
    private void legendaryinventory$hideCovered(AbstractContainerScreen<?> screen, double guiMouseX, double guiMouseY,
                                                CallbackInfoReturnable<Stream<IGuiClickableArea>> cir) {
        if (!InventoryExpansion.isExpanded(screen)) {
            return;
        }
        int top = InventoryExpansion.coverTop();
        int bottom = top + InventoryExpansion.coverHeight();
        int right = InventoryExpansion.panelWidth();
        cir.setReturnValue(cir.getReturnValue().filter(area -> {
            Rect2i r = area.getArea();
            return r.getX() >= right || r.getX() + r.getWidth() <= 0
                || r.getY() >= bottom || r.getY() + r.getHeight() <= top;
        }));
    }
}
