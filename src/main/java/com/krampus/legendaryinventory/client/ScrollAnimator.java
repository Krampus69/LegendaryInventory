package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.ScrollContext;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public final class ScrollAnimator {

    private static final float SPEED = 14.0F;
    private static final float SNAP = 0.005F;
    private static final float MIN_DT = 0.016F;
    private static final float MAX_DT = 0.1F;

    private static final int SLOT = 18;

    private static final ResourceLocation SLOT_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 83;

    private static final Map<ScrollContext, State> STATES = new WeakHashMap<>();

    private ScrollAnimator() {}

    private static final class State {
        float renderScroll;
        long lastFrame;
        long holdUntil;

        State(float start) {
            this.renderScroll = start;
            this.lastFrame = 0L;
            this.holdUntil = 0L;
        }
    }

    private static State state(ScrollContext context) {
        return STATES.computeIfAbsent(context, c -> new State(c.getScrollRow()));
    }

    public static void tick(ScrollContext context) {
        State s = state(context);
        long now = Util.getMillis();
        float dt = s.lastFrame == 0L ? MIN_DT : Math.min(MAX_DT, (now - s.lastFrame) / 1000.0F);
        s.lastFrame = now;
        if (now < s.holdUntil) {
            return;
        }

        float target = context.getScrollRow();
        float diff = target - s.renderScroll;
        if (Math.abs(diff) < SNAP) {
            s.renderScroll = target;
        } else {
            s.renderScroll += diff * Math.min(1.0F, dt * SPEED);
        }
    }

    public static void reset(ScrollContext context) {
        STATES.remove(context);
    }

    public static void prime(ScrollContext context, int fromRow, long holdMillis) {
        State s = new State(fromRow);
        s.holdUntil = Util.getMillis() + holdMillis;
        STATES.put(context, s);
    }

    public static float renderRow(ScrollContext context) {
        State s = STATES.get(context);
        return s == null ? context.getScrollRow() : s.renderScroll;
    }

    public static boolean animating(ScrollContext context) {
        State s = STATES.get(context);
        return s != null && Math.abs(context.getScrollRow() - s.renderScroll) > 0.001F;
    }

    public static void setWindowHidden(AbstractContainerScreen<?> screen, boolean hidden) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot li) {
                li.setRenderHidden(hidden);
            }
        }
    }

    public static void render(GuiGraphics g, Font font, AbstractContainerScreen<?> screen, ScrollContext context) {
        State s = STATES.get(context);
        if (s == null) {
            return;
        }

        int originX = Integer.MAX_VALUE;
        int originY = Integer.MAX_VALUE;
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot) {
                originX = Math.min(originX, slot.x);
                originY = Math.min(originY, slot.y);
            }
        }
        if (originX == Integer.MAX_VALUE) {
            return;
        }

        int gridX = screen.getGuiLeft() + originX;
        int gridY = screen.getGuiTop() + originY;
        int gridLeft = gridX - 1;
        int gridTop = gridY - 1;
        int gridRight = gridLeft + ScrollContext.COLS * SLOT;
        int gridBottom = gridTop + ScrollContext.VISIBLE_ROWS * SLOT;

        g.enableScissor(gridLeft, gridTop, gridRight, gridBottom);

        int firstRow = (int) Math.floor(s.renderScroll);
        for (int r = 0; r <= ScrollContext.VISIBLE_ROWS; r++) {
            int row = firstRow + r;
            if (row < 0) {
                continue;
            }
            int sy = gridY + Math.round((row - s.renderScroll) * SLOT);
            for (int col = 0; col < ScrollContext.COLS; col++) {
                int sx = gridX + col * SLOT;
                g.blit(SLOT_TEXTURE, sx - 1, sy - 1, SLOT_U, SLOT_V, SLOT, SLOT);

                ItemStack stack = stackAt(context, row * ScrollContext.COLS + col);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, sx, sy);
                    g.renderItemDecorations(font, stack, sx, sy);
                }
            }
        }

        g.disableScissor();
    }

    private static ItemStack stackAt(ScrollContext context, int pos) {
        if (pos < 0) {
            return ItemStack.EMPTY;
        }
        int[] filter = context.getFilter();
        int backing = filter == null
            ? (pos < context.visibleCount() ? pos : -1)
            : (pos < filter.length ? filter[pos] : -1);
        if (backing < 0 || backing >= context.getBacking().getSlots()) {
            return ItemStack.EMPTY;
        }
        return context.getBacking().getStackInSlot(backing);
    }
}
