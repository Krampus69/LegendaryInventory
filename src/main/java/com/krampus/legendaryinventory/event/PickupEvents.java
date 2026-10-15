package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class PickupEvents {

    private PickupEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPickup(EntityItemPickupEvent event) {
        if (event.getResult() == Event.Result.ALLOW) {
            return;
        }
        Player player = event.getEntity();
        ItemEntity entity = event.getItem();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack stack = entity.getItem();
        if (entity.isRemoved() || stack.isEmpty()) {
            event.setCanceled(true);
            return;
        }
        if (entity.hasPickUpDelay()) {
            return;
        }
        if (!ExtendedAccess.accepts(stack)) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }

        int before = stack.getCount();
        ItemStack working = stack.copy();
        ext.takeInserted();
        player.getInventory().add(working);
        ext.takeInserted();
        int moved = before - working.getCount();
        if (moved <= 0) {
            return;
        }

        event.setCanceled(true);

        ForgeEventFactory.firePlayerItemPickupEvent(player, entity, stack.copyWithCount(moved));
        player.take(entity, moved);
        stack.shrink(moved);
        if (stack.isEmpty()) {
            entity.discard();
            stack.setCount(before);
        } else {
            entity.setItem(stack.copy());
        }
        player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), moved);
        player.onItemPickup(entity);
    }
}