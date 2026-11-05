package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.compat.AE2Compat;
import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public final class AugmentedScreens {

    public static final int SLOT_SIZE = 16;
    private static final int PANEL_W = 176;
    private static final int GRID_INSET = 8;

    private AugmentedScreens() {}

    @Nullable
    public static AbstractContainerScreen<?> of(Screen candidate) {
        if (!(candidate instanceof AbstractContainerScreen<?> screen)) {
            return null;
        }
        if (screen instanceof LIScreen) {
            return null;
        }
        return ScrollRegistry.has(screen.getMenu()) ? screen : null;
    }

    @Nullable
    public static Slot slotAt(AbstractContainerScreen<?> screen, double mx, double my) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        AbstractContainerMenu menu = screen.getMenu();
        for (Slot slot : menu.slots) {
            if (!slot.isActive() || !isReal(menu, slot)) {
                continue;
            }
            int x = left + slot.x;
            int y = top + slot.y;
            if (mx >= x - 1 && mx < x + SLOT_SIZE + 1 && my >= y - 1 && my < y + SLOT_SIZE + 1) {
                return slot;
            }
        }
        return null;
    }

    public static boolean isReal(AbstractContainerMenu menu, Slot slot) {
        return slot.index >= 0 && slot.index < menu.slots.size() && menu.slots.get(slot.index) == slot;
    }

    public static void followReplaced(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot li) {
                li.followReplaced();
            }
        }
    }

    public static int centeredLeft(AbstractContainerScreen<?> screen, int width) {
        if (AE2Compat.isAE2Screen(screen)) {
            int grid = Integer.MAX_VALUE;
            for (Slot slot : screen.getMenu().slots) {
                if (slot instanceof LISlot li && li.window() >= 0) {
                    grid = Math.min(grid, slot.x);
                }
            }
            if (grid != Integer.MAX_VALUE) {
                return screen.getGuiLeft() + grid - GRID_INSET + (PANEL_W - width) / 2;
            }
        }
        return screen.getGuiLeft() + (screen.getXSize() - width) / 2;
    }

    public static boolean overRows(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot li && li.window() >= 0) {
                minX = Math.min(minX, slot.x);
                minY = Math.min(minY, slot.y);
                maxX = Math.max(maxX, slot.x);
                maxY = Math.max(maxY, slot.y);
            }
        }
        if (minX == Integer.MAX_VALUE) {
            return false;
        }
        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= minX - 1 && relX < maxX + SLOT_SIZE + 1 && relY >= minY - 1 && relY < maxY + SLOT_SIZE + 1;
    }

    public static boolean overWindow(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= 0 && relX < screen.getXSize() && relY >= 0 && relY < screen.getYSize();
    }
}
