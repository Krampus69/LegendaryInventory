package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.item.LIItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class PickupEvents {

    private PickupEvents() {}

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty() || stack.is(LIItems.SACK.get())) {
            return;
        }

        if (vanillaSpaceFor(player, stack) >= stack.getCount()) {
            return;
        }

        int before = stack.getCount();
        player.getInventory().add(stack);

        if (!stack.isEmpty()) {
            player.getCapability(LICaps.EXTENDED).ifPresent(ext -> {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(ext, stack.copy(), false);
                stack.setCount(remainder.getCount());
            });
        }

        if (stack.getCount() < before) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    private static int vanillaSpaceFor(Player player, ItemStack stack) {
        Inventory inv = player.getInventory();
        int cap = stack.getMaxStackSize();
        int needed = stack.getCount();
        int space = 0;

        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack cur = inv.items.get(i);
            if (cur.isEmpty()) {
                space += cap;
            } else if (ItemStack.isSameItemSameTags(cur, stack)) {
                int limit = Math.min(cap, cur.getMaxStackSize());
                if (cur.getCount() < limit) {
                    space += limit - cur.getCount();
                }
            }
            if (space >= needed) {
                return space;
            }
        }
        return space;
    }
}
