package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.MenuAugment;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.menu.HiddenCollect;
import com.krampus.legendaryinventory.net.CollectPacket;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.ScrollPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class ClientMenuAugmentEvents {

    private static final long OPEN_CUE_HOLD_MILLIS = 250L;
    private static final long DOUBLE_CLICK_MILLIS = 250L;

    private static Slot lastClickSlot = null;
    private static long lastClickTime = 0L;
    private static int lastClickButton = -1;
    private static boolean doubleClick = false;

    private static final int SLOT_SIZE = 16;

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
            if (InventoryWeightBar.available(screen)) {
                InventorySearchBar.attach(screen);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen) {
            InventorySearchBar.detach();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (augmented(event.getScreen()) != null && InventorySearchBar.charTyped(event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        if (augmented(event.getScreen()) != null
            && InventorySearchBar.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
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

    private static boolean isSortKey(int keyCode, int scanCode) {
        InputConstants.Key key = LIKeys.SORT.getKey();
        if (key.getType() == InputConstants.Type.SCANCODE) {
            return key.getValue() == scanCode;
        }
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == keyCode;
    }

    private static void resetScroll(AbstractContainerMenu menu) {
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return;
        }
        int max = context.maxScrollRow();
        int target;
        if (max == 0 || !ClientIntroCue.consume()) {
            target = 0;
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
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        ScrollAnimator.tick(context);
        ScrollAnimator.setWindowHidden(screen, ScrollAnimator.animating(context));
    }

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = augmented(event.getContainerScreen());
        if (screen == null) {
            return;
        }
        ContainerSortButton.renderForeground(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null) {
            return;
        }
        ContainerSortButton.renderTooltip(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
        InventoryWeightBar.renderTooltip(event.getGuiGraphics(), screen, event.getMouseX(), event.getMouseY());
        ClientIntroCue.render(event.getGuiGraphics(), screen);
    }

    @SubscribeEvent
    public static void onTrackClick(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null) {
            return;
        }
        Slot slot = slotAt(screen, event.getMouseX(), event.getMouseY());
        long now = Util.getMillis();
        doubleClick = slot != null && slot == lastClickSlot
            && now - lastClickTime < DOUBLE_CLICK_MILLIS
            && lastClickButton == event.getButton();
        lastClickSlot = slot;
        lastClickTime = now;
        lastClickButton = event.getButton();
    }

    @SubscribeEvent
    public static void onTrackRelease(ScreenEvent.MouseButtonReleased.Post event) {
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null || !doubleClick) {
            return;
        }
        doubleClick = false;
        lastClickTime = 0L;
        if (event.getButton() != 0 || Screen.hasShiftDown()) {
            return;
        }
        Slot slot = slotAt(screen, event.getMouseX(), event.getMouseY());
        if (slot == null || !screen.getMenu().canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            return;
        }
        if (HiddenCollect.collect(screen.getMenu())) {
            LINet.toServer(new CollectPacket());
        }
    }

    private static Slot slotAt(AbstractContainerScreen<?> screen, double mx, double my) {
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

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null) {
            return;
        }
        if (InventorySearchBar.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (event.getButton() != 0) {
            return;
        }
        if (InventoryWeightBar.searchHovered(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            InventorySearchBar.toggle();
            return;
        }
        if (!ContainerSortButton.hovered(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        event.setCanceled(true);
        InventorySearchBar.clear();
        ContainerSortButton.click();
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post event) {
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null) {
            return;
        }
        if (!isSortKey(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        InventorySearchBar.clear();
        ContainerSortButton.click();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = augmented(event.getContainerScreen());
        if (screen == null) {
            return;
        }
        InventoryWeightBar.render(event.getGuiGraphics(), screen);
        clampScroll(screen.getMenu());
        if (screen instanceof InventoryScreen) {
            InventorySearchBar.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0.0F);
        }
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        if (!ScrollAnimator.animating(context)) {
            return;
        }
        ScrollAnimator.render(event.getGuiGraphics(), Minecraft.getInstance().font, screen, context);
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        AbstractContainerScreen<?> screen = augmented(event.getScreen());
        if (screen == null) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        if (context.maxScrollRow() <= 0) {
            return;
        }
        if (!overGrid(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        if (mouseButtonHeld()) {
            event.setCanceled(true);
            return;
        }

        int target = context.applyWheel(event.getScrollDeltaY(), Screen.hasShiftDown());
        if (target != context.getScrollRow()) {
            context.setScrollRow(target);
            LINet.toServer(new ScrollPacket(target));
        }
        event.setCanceled(true);
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

    private static AbstractContainerScreen<?> augmented(Screen candidate) {
        if (!(candidate instanceof AbstractContainerScreen<?> screen)) {
            return null;
        }
        if (screen instanceof LIScreen) {
            return null;
        }
        return ScrollRegistry.has(screen.getMenu()) ? screen : null;
    }

    private static boolean overGrid(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
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
