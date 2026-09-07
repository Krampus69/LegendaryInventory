package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.net.ClearExtendedPacket;
import com.krampus.legendaryinventory.net.LINet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class CreativeClearEvents {

    private static final int DESTROY_SLOT_MIN_X = 170;

    private CreativeClearEvents() {}

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        if (!Screen.hasShiftDown()) {
            return;
        }

        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || slot.x < DESTROY_SLOT_MIN_X || slot.container instanceof Inventory) {
            return;
        }

        LINet.toServer(new ClearExtendedPacket());
    }
}
