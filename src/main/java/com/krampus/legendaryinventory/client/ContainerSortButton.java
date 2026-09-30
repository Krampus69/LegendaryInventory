package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.menu.LISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ContainerSortButton {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/sort_button.png");

    private static final int SIZE = 10;
    private static final int TEX_W = 10;
    private static final int TEX_H = 20;
    private static final int HINT_COLOR = 0x9365A2;
    private static final int RIGHT_MARGIN = 17;
    private static final int TOP_MARGIN = 5;
    private static final int SNAP = 3;

    private static final int MENU_PAD = 4;
    private static final int MENU_ROW = 12;
    private static final int MENU_BG = 0xFF555555;
    private static final int MENU_BORDER = 0xFF000000;
    private static final int MENU_HOVER = 0xFF8C8C8C;
    private static final int MENU_TEXT = 0xFFFFFF;

    private enum Action { MOVE, HIDE, SHOW, RESET }

    private static boolean keyHeld;

    private static AbstractContainerScreen<?> menuScreen;
    private static int menuX;
    private static int menuY;
    private static Action[] menuActions = new Action[0];

    private static AbstractContainerScreen<?> moveScreen;
    private static int moveX;
    private static int moveY;

    private ContainerSortButton() {}

    public static void setKeyHeld(boolean held) {
        keyHeld = held;
    }

    public static void reset() {
        keyHeld = false;
        menuScreen = null;
        moveScreen = null;
    }

    private static boolean augmented(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot) {
                return true;
            }
        }
        return false;
    }

    private static int topEdge(AbstractContainerScreen<?> screen) {
        return InventoryExpansion.isExpanded(screen) ? InventoryExpansion.coverTop() : 0;
    }

    private static int[] defaultAnchor(AbstractContainerScreen<?> screen) {
        return new int[] {screen.getXSize() - RIGHT_MARGIN, topEdge(screen) + TOP_MARGIN};
    }

    private static int[] anchor(AbstractContainerScreen<?> screen) {
        if (!augmented(screen)) {
            return null;
        }
        if (moveScreen == screen) {
            return new int[] {moveX, moveY};
        }
        SortButtonLayout.Entry entry = SortButtonLayout.get(screen);
        int[] at = entry != null && entry.x != null && entry.y != null
                ? new int[] {entry.x, entry.y}
                : defaultAnchor(screen);
        int absX = Math.max(0, Math.min(screen.width - SIZE, screen.getGuiLeft() + at[0]));
        int absY = Math.max(0, Math.min(screen.height - SIZE, screen.getGuiTop() + at[1]));
        return new int[] {absX - screen.getGuiLeft(), absY - screen.getGuiTop()};
    }

    public static boolean enabled() {
        return LIConfig.COMMON.sortEnabled.get();
    }

    private static boolean shown(AbstractContainerScreen<?> screen) {
        return enabled() && (moveScreen == screen || !SortButtonLayout.isHidden(screen));
    }

    public static boolean hovered(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!shown(screen)) {
            return false;
        }
        int[] at = anchor(screen);
        if (at == null) {
            return false;
        }
        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= at[0] && relX < at[0] + SIZE && relY >= at[1] && relY < at[1] + SIZE;
    }

    public static boolean busy(AbstractContainerScreen<?> screen) {
        return menuScreen == screen || moveScreen == screen;
    }

    public static void renderForeground(GuiGraphics g, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!shown(screen)) {
            return;
        }
        int[] at = anchor(screen);
        if (at == null) {
            return;
        }
        boolean hover = keyHeld || moveScreen == screen || hovered(screen, mouseX, mouseY);
        g.blit(TEXTURE, at[0], at[1], 0, hover ? SIZE : 0, SIZE, SIZE, TEX_W, TEX_H);
    }

    public static void renderTooltip(GuiGraphics g, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (menuScreen == screen) {
            renderMenu(g, mouseX, mouseY);
            return;
        }
        if (moveScreen == screen || !hovered(screen, mouseX, mouseY)) {
            return;
        }
        List<Component> tip = List.of(
                Component.translatable("gui.legendaryinventory.sort"),
                Component.translatable("gui.legendaryinventory.sort.weight")
                        .withStyle(Style.EMPTY.withColor(HINT_COLOR)));
        g.renderComponentTooltip(Minecraft.getInstance().font, tip, mouseX, mouseY);
    }

    public static void click() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        SortAction.send(Screen.hasControlDown());
    }

    public static void mouseMoved(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (moveScreen != screen) {
            return;
        }
        int absX = (int) Math.round(mouseX) - SIZE / 2;
        int absY = (int) Math.round(mouseY) - SIZE / 2;
        absX = Math.max(0, Math.min(screen.width - SIZE, absX));
        absY = Math.max(0, Math.min(screen.height - SIZE, absY));
        moveX = snap(absX - screen.getGuiLeft(), defaultAnchor(screen)[0], RIGHT_MARGIN - SIZE);
        moveY = snap(absY - screen.getGuiTop(), topEdge(screen) + TOP_MARGIN, screen.getYSize() - TOP_MARGIN - SIZE);
    }

    private static int snap(int value, int a, int b) {
        if (Math.abs(value - a) <= SNAP) {
            return a;
        }
        if (Math.abs(value - b) <= SNAP) {
            return b;
        }
        return value;
    }

    public static boolean mousePressed(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!enabled() || !augmented(screen)) {
            return false;
        }
        if (menuScreen == screen) {
            Action action = button == 0 ? menuEntryAt(mouseX, mouseY) : null;
            menuScreen = null;
            if (action != null) {
                run(screen, action);
            }
            return true;
        }
        if (moveScreen == screen) {
            if (button == 0) {
                SortButtonLayout.setPosition(screen, moveX, moveY);
                sound();
            }
            moveScreen = null;
            return true;
        }
        if (button != 1) {
            return false;
        }
        if (hovered(screen, mouseX, mouseY)) {
            openMenu(screen, mouseX, mouseY, Action.MOVE, Action.HIDE, Action.RESET);
            return true;
        }
        if (SortButtonLayout.isHidden(screen) && InventoryWeightBar.hovered(screen, mouseX, mouseY)) {
            openMenu(screen, mouseX, mouseY, Action.SHOW);
            return true;
        }
        return false;
    }

    public static boolean keyPressed(AbstractContainerScreen<?> screen, int keyCode) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if (menuScreen == screen) {
            menuScreen = null;
            return true;
        }
        if (moveScreen == screen) {
            moveScreen = null;
            return true;
        }
        return false;
    }

    private static void run(AbstractContainerScreen<?> screen, Action action) {
        sound();
        switch (action) {
            case MOVE -> {
                int[] at = anchor(screen);
                moveX = at == null ? defaultAnchor(screen)[0] : at[0];
                moveY = at == null ? defaultAnchor(screen)[1] : at[1];
                moveScreen = screen;
            }
            case HIDE -> SortButtonLayout.setHidden(screen, true);
            case SHOW -> SortButtonLayout.setHidden(screen, false);
            case RESET -> SortButtonLayout.reset(screen);
        }
    }

    private static void sound() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static void openMenu(AbstractContainerScreen<?> screen, double mouseX, double mouseY, Action... actions) {
        menuScreen = screen;
        menuActions = actions;
        int w = menuWidth();
        int h = menuActions.length * MENU_ROW + MENU_PAD;
        menuX = Math.max(0, Math.min(screen.width - w, (int) mouseX));
        menuY = Math.max(0, Math.min(screen.height - h, (int) mouseY));
    }

    private static Component label(Action action) {
        return Component.translatable("gui.legendaryinventory.sort.menu." + action.name().toLowerCase());
    }

    private static int menuWidth() {
        Font font = Minecraft.getInstance().font;
        int w = 0;
        for (Action action : menuActions) {
            w = Math.max(w, font.width(label(action)));
        }
        return w + MENU_PAD * 2;
    }

    private static Action menuEntryAt(double mouseX, double mouseY) {
        int w = menuWidth();
        if (mouseX < menuX || mouseX >= menuX + w) {
            return null;
        }
        int row = (int) ((mouseY - menuY - MENU_PAD / 2) / MENU_ROW);
        return row >= 0 && row < menuActions.length ? menuActions[row] : null;
    }

    private static void renderMenu(GuiGraphics g, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int w = menuWidth();
        int h = menuActions.length * MENU_ROW + MENU_PAD;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(menuX - 1, menuY - 1, menuX + w + 1, menuY + h + 1, MENU_BORDER);
        g.fill(menuX, menuY, menuX + w, menuY + h, MENU_BG);
        Action hovered = menuEntryAt(mouseX, mouseY);
        for (int i = 0; i < menuActions.length; i++) {
            int rowY = menuY + MENU_PAD / 2 + i * MENU_ROW;
            if (menuActions[i] == hovered) {
                g.fill(menuX + 1, rowY, menuX + w - 1, rowY + MENU_ROW, MENU_HOVER);
            }
            g.drawString(font, label(menuActions[i]), menuX + MENU_PAD, rowY + 2, MENU_TEXT);
        }
        g.pose().popPose();
    }
}