package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.ScrollPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;

public final class InventoryScrollBar {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(LegendaryInventory.MODID, "textures/gui/weight_bar.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 64;

    private static final int BAR_DX = 165;
    private static final int BAR_DY = -1;
    private static final int BAR_U = 184;
    private static final int BAR_W = 14;
    private static final int TOP_V = 0;
    private static final int TOP_H = 7;
    private static final int EXPANDED_TOP_U = 205;
    private static final int MID_V = 7;
    private static final int MID_H = 46;
    private static final int BOTTOM_V = 53;
    private static final int BOTTOM_H = 8;
    private static final int ROW_H = 18;

    private static final int TRACK_DX = BAR_DX + 1;
    private static final int TRACK_W = 7;

    private static final int BUTTON_U = 198;
    private static final int BUTTON_V = 0;
    private static final int BUTTON_W = 7;
    private static final int BUTTON_H = 11;

    private static boolean dragging;
    private static int grabOffset;

    private InventoryScrollBar() {}

    public static boolean supports(AbstractContainerScreen<?> screen) {
        return !(screen instanceof LIScreen) && ScrollRegistry.has(screen.getMenu());
    }

    private static int[] anchor(AbstractContainerScreen<?> screen) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot li && li.window() >= 0) {
                minX = Math.min(minX, slot.x);
                minY = Math.min(minY, slot.y);
            }
        }
        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new int[] {screen.getGuiLeft() + minX, screen.getGuiTop() + minY};
    }

    public static boolean visible() {
        return LIConfig.CLIENT.scrollBarVisible.get();
    }

    public static boolean shownOn(AbstractContainerScreen<?> screen) {
        return contextFor(screen) != null && anchor(screen) != null;
    }

    public static int width() {
        return BAR_W;
    }

    public static void toggle() {
        LIConfig.CLIENT.scrollBarVisible.set(!visible());
        LIConfig.CLIENT_SPEC.save();
        mouseReleased();
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static ScrollContext contextFor(AbstractContainerScreen<?> screen) {
        if (!supports(screen) || !visible()) {
            return null;
        }
        return ScrollRegistry.get(screen.getMenu());
    }

    private static int topExtension(ScrollContext context) {
        return context.isExpanded() ? InventoryExpansion.marginAboveRows() : 0;
    }

    private static int barTop(int[] at, ScrollContext context) {
        return at[1] + BAR_DY - topExtension(context);
    }

    private static int barHeight(ScrollContext context) {
        return context.visibleRows() * ROW_H + TOP_H + topExtension(context);
    }

    private static int trackHeight(ScrollContext context) {
        return barHeight(context) - TOP_H - BOTTOM_H;
    }

    private static int travel(ScrollContext context) {
        return trackHeight(context) - BUTTON_H;
    }

    private static int trackTop(int[] at, ScrollContext context) {
        return barTop(at, context) + TOP_H;
    }

    private static int buttonTop(int[] at, ScrollContext context) {
        int max = context.maxScrollRow();
        float ratio = max <= 0 ? 0.0F : ScrollAnimator.renderRow(context) / max;
        ratio = Math.min(Math.max(ratio, 0.0F), 1.0F);
        return trackTop(at, context) + Math.round(ratio * travel(context));
    }

    private static boolean overTrack(int[] at, ScrollContext context, double mx, double my) {
        int x = at[0] + TRACK_DX;
        int y = trackTop(at, context);
        return mx >= x && mx < x + TRACK_W && my >= y && my < y + trackHeight(context);
    }

    public static void render(GuiGraphics g, AbstractContainerScreen<?> screen) {
        ScrollContext context = contextFor(screen);
        int[] at = context == null ? null : anchor(screen);
        if (at == null) {
            return;
        }
        int barX = at[0] + BAR_DX;
        int barY = barTop(at, context);
        int height = barHeight(context);
        int topU = context.isExpanded() ? EXPANDED_TOP_U : BAR_U;
        g.blit(TEXTURE, barX, barY, 0, (float) topU, (float) TOP_V, BAR_W, TOP_H, TEX_W, TEX_H);
        int middle = height - TOP_H - BOTTOM_H;
        int drawn = 0;
        while (drawn < middle) {
            int h = Math.min(MID_H, middle - drawn);
            g.blit(TEXTURE, barX, barY + TOP_H + drawn, 0, (float) BAR_U, (float) MID_V, BAR_W, h, TEX_W, TEX_H);
            drawn += h;
        }
        g.blit(TEXTURE, barX, barY + height - BOTTOM_H, 0, (float) BAR_U, (float) BOTTOM_V, BAR_W, BOTTOM_H, TEX_W, TEX_H);
        g.blit(TEXTURE, at[0] + TRACK_DX, buttonTop(at, context), 0, (float) BUTTON_U, (float) BUTTON_V, BUTTON_W, BUTTON_H, TEX_W, TEX_H);
    }

    public static boolean mousePressed(AbstractContainerScreen<?> screen, double mx, double my, int button) {
        ScrollContext context = contextFor(screen);
        int[] at = context == null ? null : anchor(screen);
        if (at == null || button != 0 || !overTrack(at, context, mx, my)) {
            return false;
        }
        int top = buttonTop(at, context);
        if (my >= top && my < top + BUTTON_H) {
            grabOffset = (int) my - top;
        } else {
            grabOffset = BUTTON_H / 2;
        }
        dragging = true;
        scrollTo(at, context, my);
        return true;
    }

    public static boolean mouseDragged(AbstractContainerScreen<?> screen, double mx, double my) {
        if (!dragging) {
            return false;
        }
        ScrollContext context = contextFor(screen);
        int[] at = context == null ? null : anchor(screen);
        if (at == null) {
            dragging = false;
            return false;
        }
        scrollTo(at, context, my);
        return true;
    }

    public static void mouseReleased() {
        dragging = false;
    }

    private static void scrollTo(int[] at, ScrollContext context, double my) {
        int max = context.maxScrollRow();
        if (max <= 0) {
            return;
        }
        float ratio = (float) (my - grabOffset - trackTop(at, context)) / travel(context);
        int target = Math.round(Math.min(Math.max(ratio, 0.0F), 1.0F) * max);
        if (target != context.getScrollRow()) {
            context.setScrollRow(target);
            LINet.toServer(new ScrollPacket(target));
        }
    }
}