package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.PickupNotifyPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class PickupEvents {

    private PickupEvents() {}

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || event.getItem().hasPickUpDelay()) {
            return;
        }
        ItemStack stack = event.getItem().getItem();
        if (!ExtendedAccess.accepts(stack)) {
            return;
        }
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }

        int before = stack.getCount();
        ItemStack original = stack.copy();
        ext.takeInserted();
        player.getInventory().add(stack);
        int toExtended = ext.takeInserted();
        int toVanilla = before - stack.getCount() - toExtended;

        if (toVanilla <= 0 && toExtended > 0 && player instanceof ServerPlayer sp) {
            LINet.toPlayer(sp, new PickupNotifyPacket(original.copyWithCount(toExtended)));
        }
        if (stack.getCount() < before) {
            event.setResult(Event.Result.ALLOW);
        }
    }
}
