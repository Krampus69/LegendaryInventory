package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.item.LIItems;
import com.krampus.legendaryinventory.item.SackItem;
import com.krampus.legendaryinventory.item.SackStorage;
import com.krampus.legendaryinventory.weight.CarryLoad;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class SackEvents {

    private static final int SCAN_INTERVAL = 20;
    private static final int RETRY_INTERVAL = 10;

    private SackEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity entity = event.getItemEntity();
        ItemStack sack = entity.getItem();
        if (!sack.is(LIItems.SACK.get())) {
            return;
        }

        event.setCanPickup(TriState.FALSE);

        Player player = event.getPlayer();
        if (CarryLoad.stalled(player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity.tickCount % RETRY_INTERVAL != 0) {
            return;
        }

        int moved = unpack(level, sack, player);
        if (moved <= 0) {
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.7F, 1.0F);

        if (SackItem.isEmpty(sack)) {
            player.take(entity, 1);
            entity.discard();
        } else {
            entity.setItem(sack);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity item) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) {
            return;
        }
        ItemStack stack = item.getItem();
        if (!stack.is(LIItems.SACK.get())) {
            return;
        }
        UUID id = SackItem.idOf(stack);
        if (id != null) {
            SackStorage.get(level.getServer()).remove(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(LIItems.SACK.get())) {
                continue;
            }
            player.getInventory().setItem(i, ItemStack.EMPTY);
            unpack(level, stack, player);
            if (!SackItem.isEmpty(stack)) {
                player.getInventory().setItem(i, stack);
            }
        }

        scan(level, LIAttachments.extended(player), player);
    }

    private static void scan(ServerLevel level, IItemHandlerModifiable handler, Player player) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.is(LIItems.SACK.get())) {
                continue;
            }
            handler.setStackInSlot(i, ItemStack.EMPTY);
            unpack(level, stack, player);
            if (!SackItem.isEmpty(stack)) {
                handler.setStackInSlot(i, stack);
            }
        }
    }

    private static int unpack(ServerLevel level, ItemStack sack, Player player) {
        UUID id = SackItem.idOf(sack);
        if (id == null) {
            SackItem.setCount(sack, 0);
            return 0;
        }

        SackStorage storage = SackStorage.get(level.getServer());
        if (!storage.has(id)) {
            SackItem.setCount(sack, 0);
            return 0;
        }

        IItemHandler target = LIAttachments.extended(player);

        ItemStackHandler contents = storage.read(id);
        int moved = 0;

        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack stack = contents.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
            if (remainder.getCount() != stack.getCount()) {
                moved++;
            }
            contents.setStackInSlot(i, remainder);
        }

        if (moved <= 0) {
            return 0;
        }

        int left = SackItem.used(contents);
        SackItem.setCount(sack, left);

        if (left == 0) {
            storage.remove(id);
        } else {
            storage.write(id, contents);
        }
        return moved;
    }
}
