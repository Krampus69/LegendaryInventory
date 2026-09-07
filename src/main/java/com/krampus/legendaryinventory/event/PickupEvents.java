package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.item.LIItems;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class PickupEvents {

    private PickupEvents() {}

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) {
            return;
        }
        ItemEntity entity = event.getItemEntity();
        if (entity.hasPickUpDelay()) {
            return;
        }

        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || stack.is(LIItems.SACK.get())) {
            return;
        }

        if (vanillaSpaceFor(player, stack) >= stack.getCount()) {
            return;
        }

        int before = stack.getCount();
        player.getInventory().add(stack);

        if (!stack.isEmpty()) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(LIAttachments.extended(player), stack.copy(), false);
            stack.setCount(remainder.getCount());
        }

        int moved = before - stack.getCount();
        if (moved <= 0) {
            return;
        }
        player.take(entity, moved);
        player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), moved);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
        event.setCanPickup(TriState.FALSE);
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
            } else if (ItemStack.isSameItemSameComponents(cur, stack)) {
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
