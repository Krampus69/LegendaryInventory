package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public final class AugmentedScreens {

    public static final int SLOT_SIZE = 16;

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
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) {
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

    public static boolean overGrid(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean any = false;

        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof LISlot)) {
                continue;
            }
            any = true;
            minX = Math.min(minX, slot.x);
            minY = Math.min(minY, slot.y);
            maxX = Math.max(maxX, slot.x + SLOT_SIZE);
            maxY = Math.max(maxY, slot.y + SLOT_SIZE);
        }
        if (!any) {
            return false;
        }

        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= minX && relX < maxX && relY >= minY && relY < maxY;
    }
}
