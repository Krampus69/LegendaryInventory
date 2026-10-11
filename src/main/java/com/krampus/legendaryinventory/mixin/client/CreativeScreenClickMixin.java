package com.krampus.legendaryinventory.mixin.client;

import com.krampus.legendaryinventory.client.CreativeSlotGuard;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenClickMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void legendaryinventory$enterClick(Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        CreativeSlotGuard.enterUserAction();
    }

    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void legendaryinventory$exitClick(Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        CreativeSlotGuard.exitUserAction();
    }
}
