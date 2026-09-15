package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class ScrollContext {

    public static final int COLS = 9;
    public static final int VISIBLE_ROWS = 3;
    public static final int WINDOW = COLS * VISIBLE_ROWS;
    public static final int HELD_ROWS = 3;

    private final AbstractContainerMenu menu;
    private final Player owner;
    private final CombinedInventoryHandler backing;

    private int scrollRow = 0;
    private int[] filter = null;

    private int cachedVisible = -1;
    private long cachedTime = -1L;
    private boolean cachedCarrying = false;
    private int stickyVisible = 0;

    private double scrollAccum = 0.0D;

    public ScrollContext(AbstractContainerMenu menu, Player owner, CombinedInventoryHandler backing) {
        this.menu = menu;
        this.owner = owner;
        this.backing = backing;
    }

    public AbstractContainerMenu getMenu() {
        return menu;
    }

    public CombinedInventoryHandler getBacking() {
        return backing;
    }

    public int getScrollRow() {
        return scrollRow;
    }

    @Nullable
    public int[] getFilter() {
        return filter;
    }

    public void setFilter(@Nullable int[] indices) {
        boolean changed = (this.filter == null) != (indices == null) || indices != null;
        this.filter = indices;
        if (changed) {
            setScrollRow(0);
        }
    }

    public int visibleCount() {
        if (filter != null) {
            return filter.length;
        }
        long time = owner.level().getGameTime();
        boolean carrying = !menu.getCarried().isEmpty();
        if (time != cachedTime || carrying != cachedCarrying) {
            cachedTime = time;
            cachedCarrying = carrying;
            cachedVisible = computeVisible();
        }
        return cachedVisible;
    }

    private int computeVisible() {
        int needed = CombinedInventoryHandler.MAIN_COUNT + extraRows() * COLS;
        int limit = backing.getSlots();
        int current;
        if (!menu.getCarried().isEmpty()) {
            current = Math.max(stickyVisible, needed + HELD_ROWS * COLS);
        } else {
            int windowEnd = (scrollRow + VISIBLE_ROWS) * COLS;
            current = Math.max(needed, Math.min(stickyVisible, windowEnd));
        }
        current = Math.min(current, limit);
        stickyVisible = current;
        return current;
    }

    private int extraRows() {
        int base = CombinedInventoryHandler.MAIN_COUNT;
        for (int i = backing.getSlots() - 1; i >= base; i--) {
            if (!backing.getStackInSlot(i).isEmpty()) {
                return ((i - base) / COLS) + 1;
            }
        }
        return 0;
    }

    public int totalRows() {
        return (visibleCount() + COLS - 1) / COLS;
    }

    public void trim() {
        stickyVisible = 0;
        cachedTime = -1L;
    }

    public int firstOccupiedRow() {
        for (int i = 0; i < backing.getSlots(); i++) {
            if (!backing.getStackInSlot(i).isEmpty()) {
                return i / COLS;
            }
        }
        return 0;
    }

    public int maxScrollRow() {
        return Math.max(0, totalRows() - VISIBLE_ROWS);
    }

    public int physicalMaxScrollRow() {
        int rows = (backing.getSlots() + COLS - 1) / COLS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    public void setScrollRow(int row) {
        this.scrollRow = Math.max(0, Math.min(row, physicalMaxScrollRow()));
    }

    public int applyWheel(double delta, boolean page) {
        double rowsPerNotch = page ? VISIBLE_ROWS : 1.0D;
        scrollAccum -= delta * rowsPerNotch;
        int step = (int) scrollAccum;
        if (step == 0) {
            return scrollRow;
        }
        scrollAccum -= step;
        int target = scrollRow + step;
        int max = maxScrollRow();
        if (target < 0 || target > max) {
            scrollAccum = 0.0D;
        }
        return Math.max(0, Math.min(target, max));
    }

    public int backingIndexFor(int window) {
        int pos = scrollRow * COLS + window;
        if (filter == null) {
            return pos < visibleCount() ? pos : -1;
        }
        return pos < filter.length ? filter[pos] : -1;
    }

    public boolean isBackingVisible(int backingIndex) {
        for (int window = 0; window < WINDOW; window++) {
            if (backingIndexFor(window) == backingIndex) {
                return true;
            }
        }
        return false;
    }

    public int writeIndexFor(int window) {
        int pos = scrollRow * COLS + window;
        if (filter == null) {
            return pos < backing.getSlots() ? pos : -1;
        }
        return pos < filter.length ? filter[pos] : -1;
    }

    public Player getOwner() {
        return owner;
    }

    public void onSlotContentsChanged() {
        menu.broadcastChanges();
    }
}
