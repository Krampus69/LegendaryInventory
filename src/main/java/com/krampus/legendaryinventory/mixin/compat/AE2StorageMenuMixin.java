package com.krampus.legendaryinventory.mixin.compat;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.menu.AEBaseMenu;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "appeng.menu.me.common.MEStorageMenu", remap = false)
public abstract class AE2StorageMenuMixin {

    @Shadow @Final protected MEStorage storage;
    @Shadow @Final protected IEnergySource energySource;

    @Inject(method = "moveOneStackToPlayer", at = @At("RETURN"), cancellable = true, require = 0)
    private void legendaryinventory$spillIntoRows(AEItemKey key, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || storage == null || energySource == null) {
            return;
        }
        AEBaseMenu menu = (AEBaseMenu) (Object) this;
        Player player = menu.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(menu);
        IItemHandler target = context != null ? context.getBacking() : ExtendedAccess.of(player);
        if (target == null) {
            return;
        }
        ItemStack probe = key.toStack();
        if (!ExtendedAccess.accepts(probe)) {
            return;
        }
        int max = probe.getMaxStackSize();
        int room = max - ItemHandlerHelper.insertItemStacked(target, probe.copyWithCount(max), true).getCount();
        if (room <= 0) {
            return;
        }
        long extracted = StorageHelper.poweredExtraction(energySource, storage, key, room, menu.getActionSource());
        if (extracted <= 0) {
            return;
        }
        ItemStack moved = ItemHandlerHelper.insertItemStacked(target, key.toStack((int) extracted), false);
        if (!moved.isEmpty()) {
            long back = StorageHelper.poweredInsert(energySource, storage, key, moved.getCount(), menu.getActionSource());
            moved.shrink((int) back);
            if (!moved.isEmpty()) {
                player.getInventory().placeItemBackInInventory(moved);
            }
        }
        cir.setReturnValue(true);
    }
}
