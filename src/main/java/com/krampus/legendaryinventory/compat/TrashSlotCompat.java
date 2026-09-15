package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.client.InventorySearchBar;
import com.krampus.legendaryinventory.client.InventoryWeightBar;
import net.blay09.mods.trashslot.api.IGuiContainerLayout;
import net.blay09.mods.trashslot.api.SlotRenderStyle;
import net.blay09.mods.trashslot.api.Snap;
import net.blay09.mods.trashslot.api.TrashSlotAPI;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

public final class TrashSlotCompat implements IGuiContainerLayout {

    public static final String MOD_ID = "trashslot";

    private static final int PANEL_EXTENSION = 9;
    private static final int HIDDEN_OFFSET = -100000;
    private static final int CHEST_BOTTOM_OFFSET = -1;
    private static final int LARGE_CHEST_SLOTS = 63;
    private static final String LARGE_SUFFIX = "_large";

    private final boolean chest;

    private TrashSlotCompat(boolean chest) {
        this.chest = chest;
    }

    public static void init() {
        TrashSlotCompat layout = new TrashSlotCompat(false);
        TrashSlotAPI.registerLayout(InventoryScreen.class, layout);
        TrashSlotAPI.registerLayout(CraftingScreen.class, layout);
        TrashSlotAPI.registerLayout(ContainerScreen.class, new TrashSlotCompat(true));
    }

    private static int extension(AbstractContainerScreen<?> screen) {
        return InventoryWeightBar.available(screen) ? PANEL_EXTENSION : 0;
    }

    private static int bottom(AbstractContainerScreen<?> screen) {
        return screen.getGuiTop() + screen.getYSize() + extension(screen);
    }

    private static boolean bottomAttached(SlotRenderStyle style) {
        return switch (style) {
            case ATTACH_BOTTOM_CENTER, ATTACH_BOTTOM_LEFT, ATTACH_BOTTOM_RIGHT,
                ATTACH_LEFT_BOTTOM, ATTACH_RIGHT_BOTTOM -> true;
            default -> false;
        };
    }

    @Override
    public List<Rect2i> getCollisionAreas(AbstractContainerScreen<?> screen) {
        return List.of(new Rect2i(screen.getGuiLeft(), screen.getGuiTop(),
            screen.getXSize(), screen.getYSize() + extension(screen)));
    }

    @Override
    public List<Snap> getSnaps(AbstractContainerScreen<?> screen, SlotRenderStyle style) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        return List.of(
            new Snap(Snap.Type.HORIZONTAL, 0, top),
            new Snap(Snap.Type.HORIZONTAL, 0, bottom(screen) - style.getHeight()),
            new Snap(Snap.Type.VERTICAL, left, 0),
            new Snap(Snap.Type.VERTICAL, left + screen.getXSize() - style.getWidth(), 0));
    }

    @Override
    public SlotRenderStyle getSlotRenderStyle(AbstractContainerScreen<?> screen, int x, int y) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int right = left + screen.getXSize();
        int bottom = bottom(screen);
        int width = SlotRenderStyle.LONE.getWidth();
        int height = SlotRenderStyle.LONE.getHeight();

        if (y == bottom) {
            SlotRenderStyle style = along(x, x + width, left, right,
                SlotRenderStyle.ATTACH_BOTTOM_LEFT, SlotRenderStyle.ATTACH_BOTTOM_RIGHT, SlotRenderStyle.ATTACH_BOTTOM_CENTER);
            if (style != null) {
                return style;
            }
        }
        if (y + height == top) {
            SlotRenderStyle style = along(x, x + width, left, right,
                SlotRenderStyle.ATTACH_TOP_LEFT, SlotRenderStyle.ATTACH_TOP_RIGHT, SlotRenderStyle.ATTACH_TOP_CENTER);
            if (style != null) {
                return style;
            }
        }
        if (x + width == left) {
            SlotRenderStyle style = along(y, y + height, top, bottom,
                SlotRenderStyle.ATTACH_LEFT_TOP, SlotRenderStyle.ATTACH_LEFT_BOTTOM, SlotRenderStyle.ATTACH_LEFT_CENTER);
            if (style != null) {
                return style;
            }
        }
        if (x == right) {
            SlotRenderStyle style = along(y, y + height, top, bottom,
                SlotRenderStyle.ATTACH_RIGHT_TOP, SlotRenderStyle.ATTACH_RIGHT_BOTTOM, SlotRenderStyle.ATTACH_RIGHT_CENTER);
            if (style != null) {
                return style;
            }
        }
        return SlotRenderStyle.LONE;
    }

    private static SlotRenderStyle along(int start, int end, int edgeStart, int edgeEnd,
                                         SlotRenderStyle atStart, SlotRenderStyle atEnd, SlotRenderStyle between) {
        if (start == edgeStart) {
            return atStart;
        }
        if (end == edgeEnd) {
            return atEnd;
        }
        if (start >= edgeStart && end < edgeEnd) {
            return between;
        }
        return null;
    }

    @Override
    public int getDefaultSlotX(AbstractContainerScreen<?> screen) {
        return screen.getXSize() / 2 - SlotRenderStyle.LONE.getWidth();
    }

    @Override
    public int getDefaultSlotY(AbstractContainerScreen<?> screen) {
        return screen.getYSize() / 2;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public int getSlotOffsetX(AbstractContainerScreen<?> screen, SlotRenderStyle style) {
        return 0;
    }

    @Override
    public int getSlotOffsetY(AbstractContainerScreen<?> screen, SlotRenderStyle style) {
        if (InventorySearchBar.isOpen(screen)) {
            return HIDDEN_OFFSET;
        }
        if (chest && extension(screen) == 0 && bottomAttached(style)) {
            return CHEST_BOTTOM_OFFSET;
        }
        return 0;
    }

    @Override
    public String getContainerId(AbstractContainerScreen<?> screen) {
        String id = IGuiContainerLayout.super.getContainerId(screen);
        if (chest && screen.getMenu().slots.size() > LARGE_CHEST_SLOTS) {
            return id + LARGE_SUFFIX;
        }
        return id;
    }
}
