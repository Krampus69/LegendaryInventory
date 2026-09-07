package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.CreativeTabStrip;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.OpenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class CreativeInventoryEvents {

    private static final long COOLDOWN_MS = 300L;

    private static long lastRequest = 0L;

    private CreativeInventoryEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            openPack();
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            openPack();
        }
    }

    private static void openPack() {
        if (!CreativeTabStrip.inventoryTabSelected()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRequest < COOLDOWN_MS) {
            return;
        }
        lastRequest = now;

        LINet.toServer(new OpenPacket());
    }
}
