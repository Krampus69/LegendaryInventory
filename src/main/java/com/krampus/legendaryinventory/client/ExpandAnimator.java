package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.menu.ScrollContext;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ExpandAnimator {

    public static final long DURATION_MILLIS = 180L;
    private static final float Z = 500.0F;
    private static final int SLOT = 18;
    private static final int PANEL_X = 8;
    private static final int ROWS = ScrollContext.BASE_ROWS + ScrollContext.EXTRA_ROWS;

    private static final ResourceLocation SLOT_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int SLOT_U = 7;
    private static final int SLOT_V = 83;

    private static AbstractContainerScreen<?> screen;
    private static ScrollContext revealed;
    private static boolean opening;
    private static long start;

    private ExpandAnimator() {}

    public static void begin(AbstractContainerScreen<?> target, ScrollContext context, boolean expanding) {
        float from = progress();
        reveal(context);
        screen = target;
        opening = expanding;
        start = Util.getMillis();
        if (from > 0.0F && from < 1.0F) {
            start -= Math.round((expanding ? from : 1.0F - from) * DURATION_MILLIS);
        }
        if (expanding) {
            ContainerSortButton.popAt(target, start + DURATION_MILLIS);
        }
    }

    public static void tick(AbstractContainerScreen<?> target) {
        if (screen != null && !animating(target)) {
            reset();
        }
    }

    public static void reset() {
        screen = null;
        reveal(null);
    }

    private static void reveal(ScrollContext context) {
        if (revealed != null && revealed != context) {
            revealed.setCoveredRevealed(false);
        }
        revealed = context;
        if (context != null) {
            context.setCoveredRevealed(true);
        }
    }

    public static boolean animating(AbstractContainerScreen<?> target) {
        return screen != null && screen == target && Util.getMillis() - start < DURATION_MILLIS;
    }

    private static float progress() {
        if (screen == null) {
            return opening ? 1.0F : 0.0F;
        }
        float t = Math.min(1.0F, (Util.getMillis() - start) / (float) DURATION_MILLIS);
        float eased = t * t * (3.0F - 2.0F * t);
        return opening ? eased : 1.0F - eased;
    }

    public static float offset() {
        return (1.0F - progress()) * ScrollContext.EXTRA_ROWS * SLOT;
    }

    public static void render(GuiGraphics g, Font font, AbstractContainerScreen<?> target, ScrollContext context) {
        if (!animating(target)) {
            return;
        }
        int shift = Math.round(offset());
        int guiLeft = target.getGuiLeft();
        int guiTop = target.getGuiTop();
        int rowsTop = guiTop + InventoryExpansion.rowsTop();
        int panelTop = guiTop + InventoryExpansion.coverTop() + Math.round((1.0F - progress()) * InventoryExpansion.coverHeight());
        int bottom = rowsTop + ROWS * SLOT;

        g.enableScissor(guiLeft, panelTop, guiLeft + InventoryExpansion.panelWidth(), bottom);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, Z);
        g.pose().pushPose();
        g.pose().translate(0.0F, shift, 0.0F);
        InventoryExpansion.drawCover(g, target);
        g.pose().popPose();

        int gridX = guiLeft + PANEL_X;
        int gridY = rowsTop + shift;
        for (int r = 0; r < ROWS; r++) {
            int sy = gridY + r * SLOT;
            if (sy >= bottom) {
                break;
            }
            int row = context.getScrollRow() + r;
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
        g.pose().popPose();
        g.disableScissor();
    }

    private static ItemStack stackAt(ScrollContext context, int pos) {
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
