package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.api.WeightHooks;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.LICaps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

public final class MenuAugment {

    public static final int MAIN_FIRST = 9;
    public static final int MAIN_LAST = 35;
    public static final int MAIN_COUNT = 27;

    private static final int COVERED_FIRST = 0;
    private static final int COVERED_LAST = 8;
    private static final int OFFHAND_INDEX = 45;
    private static final int EXTRA_X = 8;
    private static final int MAIN_Y = 84;
    private static final int SLOT = 18;
    private static final int EXTRA_Y = MAIN_Y - ScrollContext.EXTRA_ROWS * SLOT;
    private static final String ADD_SLOT_SRG = "m_38897_";
    private static final Method ADD_SLOT = resolveAddSlot();

    private MenuAugment() {}

    private static Method resolveAddSlot() {
        try {
            return ObfuscationReflectionHelper.findMethod(AbstractContainerMenu.class, ADD_SLOT_SRG, Slot.class);
        } catch (Exception e) {
            LegendaryInventory.LOGGER.warn("addSlot not reachable, the expanded inventory will stay disabled", e);
            return null;
        }
    }

    private static boolean addExpansion(InventoryMenu menu, Inventory inventory, ScrollContext context) {
        if (ADD_SLOT == null) {
            return false;
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot instanceof LISlot || slot instanceof CoveredSlot) {
                continue;
            }
            if ((i >= COVERED_FIRST && i <= COVERED_LAST) || i == OFFHAND_INDEX) {
                CoveredSlot covered = new CoveredSlot(slot, context);
                covered.index = i;
                menu.slots.set(i, covered);
            }
        }
        try {
            for (int window = 0; window < ScrollContext.EXTRA_WINDOW; window++) {
                int col = window % ScrollContext.COLS;
                int row = window / ScrollContext.COLS;
                LISlot slot = new LISlot(inventory, LISlot.EXTRA_FIRST + window,
                    context, window, true, EXTRA_X + col * SLOT, EXTRA_Y + row * SLOT);
                ADD_SLOT.invoke(menu, slot);
            }
        } catch (Exception e) {
            LegendaryInventory.LOGGER.warn("failed to add expanded inventory slots", e);
            return false;
        }
        return true;
    }

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
            LISlot replacement = new LISlot(inventory, old.getSlotIndex(), context, window, false, old.x, old.y);
            replacement.index = menuIndex;
            menu.slots.set(menuIndex, replacement);
        }

        if (menu instanceof InventoryMenu inventoryMenu) {
            context.setExpandable(addExpansion(inventoryMenu, inventory, context));
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
