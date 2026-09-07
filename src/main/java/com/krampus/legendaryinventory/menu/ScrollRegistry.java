package com.krampus.legendaryinventory.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ScrollRegistry {

    private static final Map<AbstractContainerMenu, ScrollContext> CONTEXTS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private ScrollRegistry() {}

    public static void attach(AbstractContainerMenu menu, ScrollContext context) {
        CONTEXTS.put(menu, context);
    }

    @Nullable
    public static ScrollContext get(AbstractContainerMenu menu) {
        if (menu instanceof LIMenu liMenu) {
            return liMenu.context();
        }
        return CONTEXTS.get(menu);
    }

    public static boolean has(AbstractContainerMenu menu) {
        return get(menu) != null;
    }

    public static void detach(AbstractContainerMenu menu) {
        CONTEXTS.remove(menu);
    }
}
