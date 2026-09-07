package com.krampus.legendaryinventory.weight;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class WeightDirty {

    private static final Set<Player> DIRTY = Collections.newSetFromMap(new WeakHashMap<>());

    private WeightDirty() {}

    public static void mark(Player player) {
        DIRTY.add(player);
    }

    public static boolean consume(Player player) {
        return DIRTY.remove(player);
    }

    public static void watch(AbstractContainerMenu menu, Player player) {
        menu.addSlotListener(new Listener(player));
    }

    private static final class Listener implements ContainerListener {

        private final Player player;

        Listener(Player player) {
            this.player = player;
        }

        @Override
        public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) {
            DIRTY.add(player);
        }

        @Override
        public void dataChanged(AbstractContainerMenu menu, int slot, int value) {
        }
    }
}
