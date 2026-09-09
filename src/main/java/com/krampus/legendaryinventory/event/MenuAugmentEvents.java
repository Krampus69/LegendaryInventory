package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.menu.MenuAugment;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.MirrorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
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
            ScrollContext context = ScrollRegistry.get(event.getContainer());
            if (context != null) {
                context.trim();
            }
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
            ExtendedInventory extended = LICaps.get(sp);
            extended.clearChanges();
            LINet.toPlayer(sp, MirrorPacket.full(extended));
        }
    }
}
