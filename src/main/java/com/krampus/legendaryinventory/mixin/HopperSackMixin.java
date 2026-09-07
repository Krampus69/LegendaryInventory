package com.krampus.legendaryinventory.mixin;

import com.krampus.legendaryinventory.item.LIItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(HopperBlockEntity.class)
public abstract class HopperSackMixin {

    @Inject(
        method = "getItemsAtAndAbove",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void legendaryinventory$skipSacks(
            Level level, Hopper hopper, CallbackInfoReturnable<List<ItemEntity>> cir) {

        List<ItemEntity> found = cir.getReturnValue();
        if (found == null || found.isEmpty()) {
            return;
        }

        boolean hasSack = false;
        for (int i = 0; i < found.size(); i++) {
            if (found.get(i).getItem().is(LIItems.SACK.get())) {
                hasSack = true;
                break;
            }
        }
        if (!hasSack) {
            return;
        }

        List<ItemEntity> filtered = new ArrayList<>(found.size());
        for (int i = 0; i < found.size(); i++) {
            ItemEntity entity = found.get(i);
            if (!entity.getItem().is(LIItems.SACK.get())) {
                filtered.add(entity);
            }
        }
        cir.setReturnValue(filtered);
    }
}
