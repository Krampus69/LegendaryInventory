package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.AugmentedScreens;
import com.krampus.legendaryinventory.client.InventorySearchBar;
import com.krampus.legendaryinventory.client.InventoryWeightBar;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class BlueSkiesCompat {

    private static final String TAB_PACKAGE = "com.legacy.blue_skies.client.gui.screen.widgets.";
    private static final int SEARCH_SHIFT_X = -10;
    private static final Map<AbstractWidget, Integer> BASE_X = new WeakHashMap<>();
    private static final Map<AbstractWidget, Integer> BASE_Y = new WeakHashMap<>();

    private BlueSkiesCompat() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        reposition(event.getScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        reposition(event.getScreen());
    }

    private static void reposition(Screen raw) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(raw);
        if (screen == null || !InventoryWeightBar.available(screen)) {
            return;
        }
        boolean searchOpen = InventorySearchBar.isOpen(screen);
        int offsetX = searchOpen ? SEARCH_SHIFT_X : 0;
        int offsetY = searchOpen
                ? InventorySearchBar.frameBottomBelowPanel()
                : InventoryWeightBar.extensionBelow();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && isTab(widget)) {
                int baseX = BASE_X.computeIfAbsent(widget, AbstractWidget::getX);
                int baseY = BASE_Y.computeIfAbsent(widget, AbstractWidget::getY);
                widget.setX(baseX + offsetX);
                widget.setY(baseY + offsetY);
            }
        }
    }

    private static boolean isTab(AbstractWidget widget) {
        return widget.getClass().getName().startsWith(TAB_PACKAGE);
    }
}