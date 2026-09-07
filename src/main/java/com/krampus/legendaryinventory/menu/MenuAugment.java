package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.api.WeightHooks;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.LICaps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public final class MenuAugment {

    public static final int MAIN_FIRST = 9;
    public static final int MAIN_LAST = 35;
    public static final int MAIN_COUNT = 27;

    private MenuAugment() {}

    public static boolean augment(AbstractContainerMenu menu, Player player) {
        if (menu == null || player == null) {
            return false;
        }
        if (menu instanceof LIMenu) {
            return skip(menu, player, "own inventory menu");
        }
        if (WeightHooks.isMenuExcluded(menu)) {
            return skip(menu, player, "excluded through the api");
        }
        if (ScrollRegistry.has(menu)) {
            return skip(menu, player, "already augmented");
        }

        Inventory inventory = player.getInventory();
        List<Integer> targets = new ArrayList<>(MAIN_COUNT);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!WeightHooks.isSlotClassAllowed(slot.getClass())) {
                continue;
            }
            if (slot.container != inventory) {
                continue;
            }
            int containerSlot = slot.getSlotIndex();
            if (containerSlot < MAIN_FIRST || containerSlot > MAIN_LAST) {
                continue;
            }
            targets.add(i);
        }

        if (targets.size() != MAIN_COUNT) {
            return skip(menu, player, "found " + targets.size() + " plain main inventory slots, need " + MAIN_COUNT);
        }

        CombinedInventoryHandler backing =
            new CombinedInventoryHandler(inventory, LICaps.get(player));
        ScrollContext context = new ScrollContext(menu, player, backing);

        for (int menuIndex : targets) {
            Slot old = menu.slots.get(menuIndex);
            int window = old.getSlotIndex() - MAIN_FIRST;
            LISlot replacement = new LISlot(context, window, old.x, old.y);
            replacement.index = menuIndex;
            menu.slots.set(menuIndex, replacement);
        }

        ScrollRegistry.attach(menu, context);
        if (LIConfig.COMMON.logMenuAugment.get()) {
            LegendaryInventory.LOGGER.info("[{}] augmented {}", side(player), menu.getClass().getName());
        }
        return true;
    }

    private static boolean skip(AbstractContainerMenu menu, Player player, String reason) {
        if (LIConfig.COMMON.logMenuAugment.get()) {
            LegendaryInventory.LOGGER.info("[{}] skipped {}: {}", side(player), menu.getClass().getName(), reason);
        }
        return false;
    }

    private static String side(Player player) {
        return player.level().isClientSide() ? "client" : "server";
    }
}
