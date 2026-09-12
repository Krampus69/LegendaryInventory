package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.menu.HiddenCollect;
import com.krampus.legendaryinventory.net.CollectPacket;
import com.krampus.legendaryinventory.net.LINet;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class DoubleClickCollect {

    private static final long DOUBLE_CLICK_MILLIS = 250L;

    private static Slot lastClickSlot = null;
    private static long lastClickTime = 0L;
    private static int lastClickButton = -1;
    private static boolean doubleClick = false;

    private DoubleClickCollect() {}

    @SubscribeEvent
    public static void onPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        Slot slot = AugmentedScreens.slotAt(screen, event.getMouseX(), event.getMouseY());
        long now = Util.getMillis();
        doubleClick = slot != null && slot == lastClickSlot
            && now - lastClickTime < DOUBLE_CLICK_MILLIS
            && lastClickButton == event.getButton();
        lastClickSlot = slot;
        lastClickTime = now;
        lastClickButton = event.getButton();
    }

    @SubscribeEvent
    public static void onReleased(ScreenEvent.MouseButtonReleased.Post event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null || !doubleClick) {
            return;
        }
        doubleClick = false;
        lastClickTime = 0L;
        if (event.getButton() != 0 || Screen.hasShiftDown()) {
            return;
        }
        Slot slot = AugmentedScreens.slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot == null || !screen.getMenu().canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            return;
        }
        if (HiddenCollect.collect(screen.getMenu())) {
            LINet.toServer(new CollectPacket());
        }
    }
}
