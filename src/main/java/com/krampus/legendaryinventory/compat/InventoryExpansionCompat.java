package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

public final class InventoryExpansionCompat {

    private static final String MOD_ID = "invexp";
    private static final String BEHAVIORS = "derekahedron.invexp.item.ContainerItemBehaviors";

    private static boolean resolved;
    private static Method getContents;

    private InventoryExpansionCompat() {}

    public static boolean claimsWheel(ItemStack stack) {
        Method method = resolve();
        if (method == null || stack.isEmpty()) {
            return false;
        }
        try {
            Object result = method.invoke(null, stack);
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
                return false;
            }
            Object contents = optional.get();
            Class<?> type = contents.getClass();
            boolean canScroll = (boolean) type.getMethod("canScroll").invoke(contents);
            boolean empty = (boolean) type.getMethod("isEmpty").invoke(contents);
            return canScroll && !empty;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegendaryInventory.LOGGER.warn("Inventory Expansion wheel check failed, disabling", e);
            getContents = null;
            return false;
        }
    }

    private static Method resolve() {
        if (resolved) {
            return getContents;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        try {
            getContents = Class.forName(BEHAVIORS).getMethod("getContents", ItemStack.class);
        } catch (ReflectiveOperationException e) {
            LegendaryInventory.LOGGER.warn("Inventory Expansion found but its container API is missing", e);
        }
        return getContents;
    }
}
