package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.menu.MenuAugment;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.ScrollPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class ClientMenuAugmentEvents {

    private static final long OPEN_CUE_HOLD_MILLIS = 250L;

    private ClientMenuAugmentEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenInit(ScreenEvent.Init.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        MenuAugment.augment(screen.getMenu(), mc.player);
        if (screen instanceof InventoryScreen && !(screen instanceof LIScreen)) {
            resetScroll(screen.getMenu());
        }
        if (InventoryWeightBar.available(screen)) {
            InventorySearchBar.attach(screen);
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        InventorySearchBar.detach();
        ContainerSortButton.setKeyHeld(false);
        InventoryScrollBar.mouseReleased();
        if (screen instanceof InventoryScreen) {
            ScrollContext context = ScrollRegistry.get(screen.getMenu());
            if (context != null) {
                context.trim();
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        MenuAugment.augment(event.getPlayer().inventoryMenu, event.getPlayer());
    }

    @SubscribeEvent
    public static void onClientClone(ClientPlayerNetworkEvent.Clone event) {
        MenuAugment.augment(event.getNewPlayer().inventoryMenu, event.getNewPlayer());
    }

    private static void resetScroll(AbstractContainerMenu menu) {
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return;
        }
        int max = context.maxScrollRow();
        int target;
        if (max == 0 || !ClientIntroCue.consume()) {
            target = Math.min(context.firstOccupiedRow(), max);
            ScrollAnimator.reset(context);
        } else if (max == 1) {
            ClientIntroCue.show(Component.translatable("gui.legendaryinventory.rows.hint"));
            target = 0;
            ScrollAnimator.prime(context, 1, OPEN_CUE_HOLD_MILLIS);
        } else {
            ClientIntroCue.show(Component.translatable("gui.legendaryinventory.rows.hint"));
            target = max - 1;
            ScrollAnimator.prime(context, max, OPEN_CUE_HOLD_MILLIS);
        }
        if (context.getScrollRow() != target) {
            context.setScrollRow(target);
        }
        LINet.toServer(new ScrollPacket(target));
    }

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        ScrollAnimator.tick(context);
        ScrollAnimator.setWindowHidden(screen, ScrollAnimator.animating(context));
    }

    @SubscribeEvent
    public static void onRenderBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getContainerScreen());
        if (screen == null) {
            return;
        }
        InventoryWeightBar.render(event.getGuiGraphics(), screen);
        InventoryScrollBar.render(event.getGuiGraphics(), screen);
        clampScroll(screen.getMenu());
        InventorySearchBar.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0.0F);
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        if (!ScrollAnimator.animating(context)) {
            return;
        }
        ScrollAnimator.render(event.getGuiGraphics(), Minecraft.getInstance().font, screen, context);
    }

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getContainerScreen());
        if (screen == null) {
            return;
        }
        ContainerSortButton.renderForeground(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        ContainerSortButton.renderTooltip(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
        InventoryWeightBar.renderTooltip(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
        ClientIntroCue.render(event.getGuiGraphics(), screen);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSortClick(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null || event.getButton() != 0) {
            return;
        }
        if (InventoryScrollBar.mousePressed(screen, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (!ContainerSortButton.hovered(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        event.setCanceled(true);
        InventorySearchBar.clear();
        ContainerSortButton.click();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScrollBarDrag(ScreenEvent.MouseDragged.Pre event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        if (InventoryScrollBar.mouseDragged(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScrollBarRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 0) {
            InventoryScrollBar.mouseReleased();
        }
    }

    @SubscribeEvent
    public static void onSortKey(ScreenEvent.KeyPressed.Post event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        if (!isSortKey(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        InventorySearchBar.clear();
        ContainerSortButton.setKeyHeld(true);
        ContainerSortButton.click();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSortKeyReleased(ScreenEvent.KeyReleased.Post event) {
        if (isSortKey(event.getKeyCode(), event.getScanCode())) {
            ContainerSortButton.setKeyHeld(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        AbstractContainerScreen<?> screen = AugmentedScreens.of(event.getScreen());
        if (screen == null) {
            return;
        }
        if (!AugmentedScreens.overWindow(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        if (Screen.hasControlDown()) {
            if (WheelTransfer.handle(screen, event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
                event.setCanceled(true);
            }
            return;
        }
        event.setCanceled(true);
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        if (context.maxScrollRow() <= 0 || mouseButtonHeld()) {
            return;
        }

        int target = context.applyWheel(event.getScrollDelta(), Screen.hasShiftDown());
        if (target != context.getScrollRow()) {
            context.setScrollRow(target);
            LINet.toServer(new ScrollPacket(target));
        }
    }

    private static boolean isSortKey(int keyCode, int scanCode) {
        InputConstants.Key key = LIKeys.SORT.getKey();
        if (key.getType() == InputConstants.Type.SCANCODE) {
            return key.getValue() == scanCode;
        }
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == keyCode;
    }

    private static void clampScroll(AbstractContainerMenu menu) {
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null || mouseButtonHeld()) {
            return;
        }
        int max = context.maxScrollRow();
        if (context.getScrollRow() > max) {
            context.setScrollRow(max);
            LINet.toServer(new ScrollPacket(max));
        }
    }

    private static boolean mouseButtonHeld() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }
}
