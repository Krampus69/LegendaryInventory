package com.krampus.legendaryinventory;

import com.krampus.legendaryinventory.compat.WaystonesCompat;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.item.LIItems;
import com.krampus.legendaryinventory.item.WeightTabletItem;
import com.krampus.legendaryinventory.menu.LIMenus;
import com.krampus.legendaryinventory.weight.LIAttributes;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(LegendaryInventory.MODID)
public class LegendaryInventory {

    public static final String MODID = "legendaryinventory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LegendaryInventory(IEventBus modBus, ModContainer container) {
        LIAttributes.ATTRIBUTES.register(modBus);
        LIMenus.MENUS.register(modBus);
        LIItems.ITEMS.register(modBus);
        LIItems.COMPONENTS.register(modBus);
        LIAttachments.ATTACHMENTS.register(modBus);
        LIConfig.register(container);
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
}
