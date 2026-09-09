package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class LootJournalCompat {

    private static final String MOD_ID = "loot_journal";
    private static final String HELPER = "dev.obscuria.lootjournal.LootJournalHelper";

    private static Method pickupItem;
    private static boolean resolved;

    private LootJournalCompat() {}

    public static void notifyPickup(ItemStack stack) {
        Method method = resolve();
        if (method == null || stack.isEmpty()) {
            return;
        }
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        try {
            method.invoke(null, player, stack);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LegendaryInventory.LOGGER.warn("Loot Journal notification failed", e);
            pickupItem = null;
        }
    }

    private static Method resolve() {
        if (resolved) {
            return pickupItem;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> helper = Class.forName(HELPER);
            pickupItem = helper.getMethod("pickupItem", AbstractClientPlayer.class, ItemStack.class);
        } catch (ReflectiveOperationException e) {
            LegendaryInventory.LOGGER.warn("Loot Journal found but its pickup hook is missing", e);
        }
        return pickupItem;
    }
}
