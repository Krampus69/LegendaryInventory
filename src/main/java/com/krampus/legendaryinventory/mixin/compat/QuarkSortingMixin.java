package com.krampus.legendaryinventory.mixin.compat;

import com.krampus.legendaryinventory.compat.QuarkCompat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.base.handler.SortingHandler", remap = false)
public abstract class QuarkSortingMixin {

    @Inject(
        method = "sortInventory(Lnet/minecraft/world/entity/player/Player;Z)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void legendaryinventory$sortWithRows(Player player, boolean forcePlayer, CallbackInfo ci) {
        if (QuarkCompat.sortPlayer(player, forcePlayer)) {
            ci.cancel();
        }
    }
}
