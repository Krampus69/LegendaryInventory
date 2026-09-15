package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class SearchInputEvents {

    private SearchInputEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (AugmentedScreens.of(event.getScreen()) != null
            && InventorySearchBar.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (AugmentedScreens.of(event.getScreen()) != null
            && InventorySearchBar.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        if (InventorySearchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == 0 && InventoryWeightBar.searchHovered(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            InventorySearchBar.toggle();
            return;
        }
        if (event.getButton() == 0 && InventoryWeightBar.scrollToggleHovered(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            InventoryScrollBar.toggle();
        }
    }
}
