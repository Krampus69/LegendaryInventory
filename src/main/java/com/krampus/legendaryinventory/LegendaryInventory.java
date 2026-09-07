package com.krampus.legendaryinventory;

import com.krampus.legendaryinventory.client.ClientWeightState;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.compat.WaystonesCompat;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.item.LIItems;
import com.krampus.legendaryinventory.item.WeightTabletItem;
import com.krampus.legendaryinventory.menu.LIMenus;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.LIAttributes;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LegendaryInventory.MODID)
public class LegendaryInventory {

    public static final String MODID = "legendaryinventory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LegendaryInventory() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        LIAttributes.ATTRIBUTES.register(modBus);
        LIMenus.MENUS.register(modBus);
        LIItems.ITEMS.register(modBus);
        LIConfig.register();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::buildCreativeTabs);
        if (ModList.get().isLoaded(WaystonesCompat.MOD_ID)) {
            WaystonesCompat.init();
        }
    }

    private void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES || !WeightTabletItem.enabled()) {
            return;
        }
        event.accept(LIItems.WEIGHT_TABLET_1);
        event.accept(LIItems.WEIGHT_TABLET_2);
        event.accept(LIItems.WEIGHT_TABLET_3);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(LINet::init);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(LIMenus.EXTENDED.get(), LIScreen::new);
            CarryLoad.setClientCapacitySource(ClientWeightState::capacity);
        });
    }
}
