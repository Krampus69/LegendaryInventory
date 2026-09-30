package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.ExpandPacket;
import com.krampus.legendaryinventory.net.LINet;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class InventoryExpansion {

    private static final ResourceLocation INVENTORY =
            new ResourceLocation("textures/gui/container/inventory.png");

    private static final int PANEL_X = 7;
    private static final int PANEL_W = 162;
    private static final int PANEL_FULL_W = 176;
    private static final int MAIN_ROW_Y = 83;
    private static final int GAP_H = 9;
    private static final int TOP_H = 7;
    private static final int ROW_U = 7;
    private static final int ROW_V = 83;
    private static final int ROW_H = 18;
    private static final int FLAT_V = 138;
    private static final int LEFT_BORDER_U = 0;
    private static final int RIGHT_BORDER_U = 169;
    private static final int BORDER_W = 7;
    private static final int BORDER_H = 54;
    private static final int ROWS_TOP = MAIN_ROW_Y - ScrollContext.EXTRA_ROWS * ROW_H;
    private static final int COVER_TOP = ROWS_TOP - GAP_H - TOP_H;
    private static final String[] COVERED_WIDGETS = {
        "top.theillusivec4.curios.client.gui.CuriosButton"
    };
    private static final Set<AbstractWidget> HIDDEN = Collections.newSetFromMap(new WeakHashMap<>());

    private InventoryExpansion() {}

    public static int marginAboveRows() {
        return TOP_H + GAP_H;
    }

    public static int coverTop() {
        return COVER_TOP;
    }

    public static boolean supports(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen && !(screen instanceof LIScreen);
    }

    private static ScrollContext contextFor(AbstractContainerScreen<?> screen) {
        if (!supports(screen)) {
            return null;
        }
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        return context != null && context.isExpandable() ? context : null;
    }

    public static boolean isExpanded(AbstractContainerScreen<?> screen) {
        ScrollContext context = contextFor(screen);
        return context != null && context.isExpanded();
    }

    public static void applyPreference(ScrollContext context) {
        if (context != null && context.isExpandable()) {
            context.setExpanded(LIConfig.CLIENT.inventoryExpanded.get());
        }
    }

    public static void sync(AbstractContainerScreen<?> screen) {
        ScrollContext context = contextFor(screen);
        if (context == null) {
            return;
        }
        applyPreference(context);
        LINet.toServer(new ExpandPacket(context.isExpanded()));
    }

    public static boolean toggle(AbstractContainerScreen<?> screen) {
        ScrollContext context = contextFor(screen);
        if (context == null) {
            return false;
        }
        boolean value = !context.isExpanded();
        LIConfig.CLIENT.inventoryExpanded.set(value);
        LIConfig.CLIENT_SPEC.save();
        context.setExpanded(value);
        ScrollAnimator.reset(context);
        LINet.toServer(new ExpandPacket(value));
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    public static void updateWidgets(AbstractContainerScreen<?> screen) {
        if (!supports(screen)) {
            return;
        }
        boolean expanded = isExpanded(screen);
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop() + COVER_TOP;
        int right = left + PANEL_FULL_W;
        int bottom = screen.getGuiTop() + MAIN_ROW_Y;
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget)) {
                continue;
            }
            if (expanded && (covered(widget) || overlaps(widget, left, top, right, bottom))) {
                if (widget.visible) {
                    HIDDEN.add(widget);
                    widget.visible = false;
                }
            } else if (HIDDEN.remove(widget)) {
                widget.visible = true;
            }
        }
    }

    private static boolean overlaps(AbstractWidget widget, int left, int top, int right, int bottom) {
        int x = widget.getX();
        int y = widget.getY();
        return x < right && x + widget.getWidth() > left && y < bottom && y + widget.getHeight() > top;
    }

    private static boolean covered(AbstractWidget widget) {
        String name = widget.getClass().getName();
        for (String covered : COVERED_WIDGETS) {
            if (name.equals(covered)) {
                return true;
            }
        }
        return false;
    }

    public static void renderCover(GuiGraphics g, AbstractContainerScreen<?> screen) {
        if (!isExpanded(screen)) {
            return;
        }
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();
        int left = guiLeft + PANEL_X;
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        g.blit(INVENTORY, guiLeft, guiTop + COVER_TOP, 0, 0, PANEL_FULL_W, TOP_H);
        int gapTop = guiTop + COVER_TOP + TOP_H;
        for (int i = 0; i < GAP_H; i++) {
            g.blit(INVENTORY, left, gapTop + i, ROW_U, FLAT_V, PANEL_W, 1);
        }
        for (int row = 0; row < ScrollContext.EXTRA_ROWS; row++) {
            g.blit(INVENTORY, left, guiTop + ROWS_TOP + row * ROW_H, ROW_U, ROW_V, PANEL_W, ROW_H);
        }
        int sideTop = gapTop;
        int sideHeight = MAIN_ROW_Y - COVER_TOP - TOP_H;
        int drawn = 0;
        while (drawn < sideHeight) {
            int h = Math.min(BORDER_H, sideHeight - drawn);
            g.blit(INVENTORY, guiLeft, sideTop + drawn, LEFT_BORDER_U, ROW_V, BORDER_W, h);
            g.blit(INVENTORY, left + PANEL_W, sideTop + drawn, RIGHT_BORDER_U, ROW_V, BORDER_W, h);
            drawn += h;
        }
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }
}