package com.krampus.legendaryinventory.mixin.client.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "dev.obscuria.lootjournal.client.events.ItemPickupEvent", remap = false)
public abstract class LootJournalTotalMixin {

    @Shadow @Final private ItemStack stack;
    @Shadow private int total;

    @Shadow
    public abstract boolean supportsTotalCount();

    @Inject(method = "recomputeTotalCount", at = @At("TAIL"), remap = false)
    private void legendaryinventory$countExtended(int baseCount, CallbackInfo ci) {
        if (!supportsTotalCount()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(mc.player);
        Method resolve = Resolver.get();
        if (ext == null || resolve == null) {
            return;
        }
        try {
            for (int i = 0; i < ext.getSlots(); i++) {
                ItemStack present = ext.getStackInSlot(i);
                if (!present.isEmpty()) {
                    total += (int) resolve.invoke(null, stack, present);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegendaryInventory.LOGGER.warn("Loot Journal total count hook failed", e);
            Resolver.disable();
        }
    }

    private static final class Resolver {

        private static final String CLASS = "dev.obscuria.lootjournal.client.ItemCountResolver";
        private static boolean resolved;
        private static Method method;

        static Method get() {
            if (resolved) {
                return method;
            }
            resolved = true;
            try {
                method = Class.forName(CLASS).getMethod("resolveRecursive", ItemStack.class, ItemStack.class);
            } catch (ReflectiveOperationException e) {
                LegendaryInventory.LOGGER.warn("Loot Journal count resolver missing, extended rows will not be totalled", e);
            }
            return method;
        }

        static void disable() {
            method = null;
        }
    }
}
