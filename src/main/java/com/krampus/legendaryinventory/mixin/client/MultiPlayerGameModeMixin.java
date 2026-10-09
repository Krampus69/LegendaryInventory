package com.krampus.legendaryinventory.mixin.client;

import com.krampus.legendaryinventory.client.CreativeSlotGuard;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"), cancellable = true)
    private void legendaryinventory$redirectCreativeSet(ItemStack stack, int slot, CallbackInfo ci) {
        if (CreativeSlotGuard.intercept(stack, slot)) {
            ci.cancel();
        }
    }
}
