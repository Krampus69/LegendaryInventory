package com.krampus.legendaryinventory.api;

import net.neoforged.neoforge.items.IItemHandlerModifiable;

public interface IExtendedInventory extends IItemHandlerModifiable {

    int usedSlots();

    int totalWeight();

    void clearAll();
}
