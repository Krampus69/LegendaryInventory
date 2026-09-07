package com.krampus.legendaryinventory.api;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface IWeightProvider {

    int PASS = -1;

    int getWeight(ItemStack stack, int current);
}
