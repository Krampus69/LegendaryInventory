package com.krampus.legendaryinventory.api;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface IExtendedInventory extends IItemHandlerModifiable {

    int usedSlots();

    int totalWeight();

    void clearAll();
}
