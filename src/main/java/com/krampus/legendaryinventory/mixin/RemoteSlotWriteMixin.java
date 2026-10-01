package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.menu.ScrollContext;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
public abstract class RemoteSlotWriteMixin {

    @Inject(method = "setItem", at = @At("HEAD"))
    private void legendaryinventory$beginSetItem(int slot, int stateId, ItemStack stack, CallbackInfo ci) {
        ScrollContext.setRemoteWrite(true);
    }

    @Inject(method = "setItem", at = @At("RETURN"))
    private void legendaryinventory$endSetItem(int slot, int stateId, ItemStack stack, CallbackInfo ci) {
        ScrollContext.setRemoteWrite(false);
    }

    @Inject(method = "initializeContents", at = @At("HEAD"))
    private void legendaryinventory$beginInitialize(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        ScrollContext.setRemoteWrite(true);
    }

    @Inject(method = "initializeContents", at = @At("RETURN"))
    private void legendaryinventory$endInitialize(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        ScrollContext.setRemoteWrite(false);
    }
}
