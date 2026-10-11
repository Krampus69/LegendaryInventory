package com.krampus.legendaryinventory.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class HotbarSwapKeys {

    private static boolean suppressNextChar = false;

    private HotbarSwapKeys() {}

    public static boolean handle(AbstractContainerScreen<?> screen, int keyCode, int scanCode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return false;
        }
        Slot hovered = screen.getSlotUnderMouse();
        if (hovered == null || !hovered.hasItem() || !screen.getMenu().getCarried().isEmpty()) {
            return false;
        }
        KeyMapping[] keys = mc.options.keyHotbarSlots;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].matches(keyCode, scanCode)) {
                mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, hovered.index, i, ClickType.SWAP, mc.player);
                suppressNextChar = true;
                return true;
            }
        }
        return false;
    }

    public static void suppressNextChar() {
        suppressNextChar = true;
    }

    public static boolean consumeSuppressedChar() {
        boolean was = suppressNextChar;
        suppressNextChar = false;
        return was;
    }
}
