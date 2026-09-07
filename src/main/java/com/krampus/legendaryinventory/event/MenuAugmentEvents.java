package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.menu.MenuAugment;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.MirrorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class MenuAugmentEvents {

    private MenuAugmentEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        augmentInventory(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        augmentInventory(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        augmentInventory(event.getEntity());
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!MenuAugment.augment(event.getContainer(), event.getEntity())) {
            return;
        }
        sendMirror(event.getEntity());
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getContainer() == event.getEntity().inventoryMenu) {
            return;
        }
        ScrollRegistry.detach(event.getContainer());
    }

    private static void augmentInventory(Player player) {
        MenuAugment.augment(player.inventoryMenu, player);
        ScrollContext context = ScrollRegistry.get(player.inventoryMenu);
        if (context != null) {
            context.setScrollRow(0);
        }
        sendMirror(player);
    }

    private static void sendMirror(Player player) {
        if (player instanceof ServerPlayer sp) {
            ExtendedInventory extended = LIAttachments.extended(sp);
            extended.clearChanges();
            LINet.toPlayer(sp, MirrorPacket.full(extended));
        }
    }
}
