package com.krampus.legendaryinventory.mixin.compat;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.menu.AEBaseMenu;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
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
    @Shadow @Final protected IEnergySource powerSource;

    @Inject(method = "moveOneStackToPlayer", at = @At("RETURN"), cancellable = true, require = 0)
    private void legendaryinventory$spillIntoRows(AEItemKey key, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || storage == null || powerSource == null) {
            return;
        }
        AEBaseMenu menu = (AEBaseMenu) (Object) this;
        Player player = menu.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }
        ItemStack probe = key.toStack();
        if (!ExtendedAccess.accepts(probe)) {
            return;
        }
        int max = probe.getMaxStackSize();
        int room = max - ItemHandlerHelper.insertItemStacked(ext, probe.copyWithCount(max), true).getCount();
        if (room <= 0) {
            return;
        }
        long extracted = StorageHelper.poweredExtraction(powerSource, storage, key, room, menu.getActionSource());
        if (extracted <= 0) {
            return;
        }
        ItemStack moved = key.toStack((int) extracted);
        ExtendedAccess.insert(ext, moved);
        if (!moved.isEmpty()) {
            long back = StorageHelper.poweredInsert(powerSource, storage, key, moved.getCount(), menu.getActionSource());
            moved.shrink((int) back);
            if (!moved.isEmpty()) {
                player.getInventory().placeItemBackInInventory(moved);
            }
        }
        cir.setReturnValue(true);
    }
}
