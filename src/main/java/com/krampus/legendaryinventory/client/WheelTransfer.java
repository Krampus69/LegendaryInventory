package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.menu.LISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WheelTransfer {

    private static double accumulated = 0.0D;
    private static int lastSlotIndex = -1;

    private WheelTransfer() {}

    public static boolean handle(AbstractContainerScreen<?> screen, double mouseX, double mouseY, double delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || delta == 0.0D) {
            return false;
        }
        WheelSettings settings = WheelSettings.current();
        if (!settings.enabled()) {
            return false;
        }

        Slot source = AugmentedScreens.slotAt(screen, mouseX, mouseY);
        if (source == null || !source.isActive() || isCraftingOutput(source)) {
            return false;
        }

        int amount = amountFor(settings, source, delta);
        if (amount <= 0) {
            return true;
        }

        boolean push = pushes(settings, screen, source, delta);
        if (push) {
            pushFrom(screen, source, amount);
        } else {
            pullInto(screen, source, amount);
        }
        return true;
    }

    private static int amountFor(WheelSettings settings, Slot source, double delta) {
        if (lastSlotIndex != source.index || Math.signum(accumulated) != Math.signum(delta)) {
            accumulated = 0.0D;
        }
        lastSlotIndex = source.index;

        if (settings.alwaysOne()) {
            accumulated = 0.0D;
            return 1;
        }
        accumulated += delta;
        int whole = (int) Math.abs(accumulated);
        accumulated -= Math.signum(accumulated) * whole;
        return whole;
    }

    private static boolean pushes(WheelSettings settings, AbstractContainerScreen<?> screen, Slot source, double delta) {
        boolean push = delta < 0.0D;
        if (settings.positionAware() && otherSideIsAbove(screen, source)) {
            push = !push;
        }
        if (settings.inverted()) {
            push = !push;
        }
        return push;
    }

    private static boolean otherSideIsAbove(AbstractContainerScreen<?> screen, Slot source) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        boolean sourcePlayerSide = isPlayerSide(source, inventory);
        int above = 0;
        int below = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || isPlayerSide(slot, inventory) == sourcePlayerSide) {
                continue;
            }
            if (slot.y < source.y) {
                above++;
            } else {
                below++;
            }
        }
        return above > below;
    }

    private static void pushFrom(AbstractContainerScreen<?> screen, Slot source, int amount) {
        ItemStack moving = source.getItem();
        if (moving.isEmpty()) {
            return;
        }
        int total = Math.min(amount, moving.getCount());
        List<Slot> targets = findTargets(screen, source, moving, total);
        if (targets.isEmpty()) {
            return;
        }

        click(screen, source, 0, ClickType.PICKUP);
        int left = total;
        for (Slot target : targets) {
            if (left <= 0) {
                break;
            }
            int room = target.getMaxStackSize(moving) - target.getItem().getCount();
            int placed = Math.min(room, left);
            for (int i = 0; i < placed; i++) {
                click(screen, target, 1, ClickType.PICKUP);
            }
            left -= placed;
        }
        click(screen, source, 0, ClickType.PICKUP);
    }

    private static void pullInto(AbstractContainerScreen<?> screen, Slot target, int amount) {
        ItemStack existing = target.getItem();
        Slot source = findSource(screen, target, existing);
        if (source == null) {
            return;
        }
        ItemStack moving = source.getItem();
        int room = target.getMaxStackSize(moving) - existing.getCount();
        int total = Math.min(Math.min(amount, moving.getCount()), room);
        if (total <= 0) {
            return;
        }

        click(screen, source, 0, ClickType.PICKUP);
        for (int i = 0; i < total; i++) {
            click(screen, target, 1, ClickType.PICKUP);
        }
        click(screen, source, 0, ClickType.PICKUP);
    }

    private static List<Slot> findTargets(AbstractContainerScreen<?> screen, Slot source, ItemStack moving, int amount) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        boolean wantPlayerSide = !isPlayerSide(source, inventory);
        List<Slot> partial = new ArrayList<>();
        List<Slot> empty = new ArrayList<>();
        int room = 0;

        for (Slot slot : ordered(screen)) {
            if (slot == source || !slot.isActive() || isCraftingOutput(slot)) {
                continue;
            }
            if (isPlayerSide(slot, inventory) != wantPlayerSide || !slot.mayPlace(moving)) {
                continue;
            }
            ItemStack existing = slot.getItem();
            if (existing.isEmpty()) {
                empty.add(slot);
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(existing, moving)) {
                continue;
            }
            int free = slot.getMaxStackSize(moving) - existing.getCount();
            if (free <= 0) {
                continue;
            }
            partial.add(slot);
            room += free;
            if (room >= amount) {
                return partial;
            }
        }

        for (Slot slot : empty) {
            partial.add(slot);
            room += slot.getMaxStackSize(moving);
            if (room >= amount) {
                break;
            }
        }
        return partial;
    }

    @Nullable
    private static Slot findSource(AbstractContainerScreen<?> screen, Slot target, ItemStack existing) {
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        boolean wantPlayerSide = !isPlayerSide(target, inventory);

        for (Slot slot : ordered(screen)) {
            if (slot == target || !slot.isActive() || isCraftingOutput(slot)) {
                continue;
            }
            if (isPlayerSide(slot, inventory) != wantPlayerSide) {
                continue;
            }
            ItemStack candidate = slot.getItem();
            if (candidate.isEmpty() || !slot.mayPickup(Minecraft.getInstance().player)) {
                continue;
            }
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, candidate)) {
                continue;
            }
            if (!target.mayPlace(candidate)) {
                continue;
            }
            return slot;
        }
        return null;
    }

    private static List<Slot> ordered(AbstractContainerScreen<?> screen) {
        List<Slot> slots = new ArrayList<>(screen.getMenu().slots);
        if (WheelSettings.current().lastToFirst()) {
            java.util.Collections.reverse(slots);
        }
        return slots;
    }

    private static boolean isPlayerSide(Slot slot, Inventory inventory) {
        return slot instanceof LISlot || slot.container == inventory;
    }

    private static boolean isCraftingOutput(Slot slot) {
        return slot.container instanceof ResultContainer;
    }

    private static void click(AbstractContainerScreen<?> screen, Slot slot, int button, ClickType type) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        AbstractContainerMenu menu = screen.getMenu();
        if (player == null || mc.gameMode == null) {
            return;
        }
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, button, type, player);
    }
}
