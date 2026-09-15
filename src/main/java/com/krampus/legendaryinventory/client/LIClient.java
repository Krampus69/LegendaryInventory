package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.compat.TrashSlotCompat;
import com.krampus.legendaryinventory.menu.LIMenus;
import com.krampus.legendaryinventory.weight.CarryLoad;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LIClient {

    private LIClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CarryLoad.setClientCapacitySource(ClientWeightState::capacity);
            if (ModList.get().isLoaded(TrashSlotCompat.MOD_ID)) {
                TrashSlotCompat.init();
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(LIMenus.EXTENDED.get(), LIScreen::new);
    }
}
