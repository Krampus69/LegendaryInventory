package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.weight.WeightTable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public final class BackpackedCompat {

    public static final String MOD_ID = "backpacked";

    private static final MethodHandle GET_BACKPACKS = resolve();

    private BackpackedCompat() {}

    private static MethodHandle resolve() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> helper = Class.forName("com.mrcrayfish.backpacked.BackpackHelper");
            return MethodHandles.publicLookup().findStatic(helper, "getBackpacks",
                MethodType.methodType(net.minecraft.core.NonNullList.class, Player.class));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    public static int weight(Player player) {
        if (GET_BACKPACKS == null) {
            return 0;
        }
        try {
            List<?> backpacks = (List<?>) GET_BACKPACKS.invoke(player);
            int total = 0;
            for (Object entry : backpacks) {
                if (entry instanceof ItemStack stack) {
                    total += WeightTable.of(stack);
                }
            }
            return total;
        } catch (Throwable t) {
            return 0;
        }
    }
}
