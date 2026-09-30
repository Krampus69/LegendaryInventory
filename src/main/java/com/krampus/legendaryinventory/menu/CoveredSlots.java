package com.krampus.legendaryinventory.menu;

import net.minecraft.world.inventory.Slot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class CoveredSlots {

    private static final Map<Slot, ScrollContext> COVERED = Collections.synchronizedMap(new WeakHashMap<>());

    private CoveredSlots() {}

    public static void cover(Slot slot, ScrollContext context) {
        COVERED.put(slot, context);
    }

    public static boolean isHidden(Slot slot) {
        ScrollContext context = COVERED.get(slot);
        return context != null && context.isExpanded();
    }
}
