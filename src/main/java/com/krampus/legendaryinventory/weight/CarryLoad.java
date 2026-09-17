package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.compat.BackpackedCompat;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntSupplier;

public final class CarryLoad {

    public static final int MAX_TIER = 4;

    private CarryLoad() {}

    public static int carriedWeight(Player player) {
        Inventory inv = player.getInventory();
        int total = sum(inv.items) + sum(inv.armor) + sum(inv.offhand);
        total += extendedWeight(player);
        total += cursorWeight(player);
        total += BackpackedCompat.weight(player);
        return total;
    }

    public static int cursorWeight(Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        return menu == null ? 0 : WeightTable.of(menu.getCarried());
    }

    public static int extendedWeight(Player player) {
        return LIAttachments.extended(player).totalWeight();
    }

    private static int sum(List<ItemStack> compartment) {
        int total = 0;
        for (int i = 0; i < compartment.size(); i++) {
            total += WeightTable.of(compartment.get(i));
        }
        return total;
    }

    private static IntSupplier clientCapacity = () -> -1;

    public static void setClientCapacitySource(IntSupplier source) {
        clientCapacity = source;
    }

    public static int capacity(Player player) {
        if (player.level().isClientSide()) {
            int synced = clientCapacity.getAsInt();
            if (synced > 0) {
                return synced;
            }
        }
        var attr = player.getAttribute(LIAttributes.CARRY_CAPACITY);
        return attr == null ? LIConfig.COMMON.baseCapacity.get() : (int) attr.getValue();
    }

    public static int tierFor(int carried, int capacity) {
        if (capacity <= 0) {
            return MAX_TIER;
        }
        double ratio = carried / (double) capacity;
        double[] thresholds = WeightRules.current().thresholds();
        for (int i = 0; i < thresholds.length; i++) {
            if (ratio < thresholds[i]) {
                return i;
            }
        }
        return MAX_TIER;
    }

    public static double ratioFor(int carried, int capacity) {
        return capacity <= 0 ? WeightRules.current().stall() : carried / (double) capacity;
    }

    public static boolean sprintBlocked(int carried, int capacity) {
        return ratioFor(carried, capacity) >= WeightRules.current().sprintBlock();
    }

    public static boolean stalled(int carried, int capacity) {
        return ratioFor(carried, capacity) >= WeightRules.current().stall();
    }

    public static boolean stalled(Player player) {
        if (player.isCreative() || player.isSpectator() || !WeightTable.isReady()) {
            return false;
        }
        return stalled(carriedWeight(player), capacity(player));
    }

    public static double penaltyFor(int carried, int capacity) {
        double ratio = ratioFor(carried, capacity);
        double start = WeightRules.current().slowStart();
        double span = WeightRules.current().stall() - start;
        if (ratio <= start || span <= 0.0D) {
            return ratio >= WeightRules.current().stall() ? -1.0D : 0.0D;
        }
        return -Math.min((ratio - start) / span, 1.0D);
    }
}
