package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class LegendaryTabsSuppressEvents {

    private static final String TABS_PACKAGE = "sfiomn.legendarytabs";

    private LegendaryTabsSuppressEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof LIScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.gameMode.getPlayerMode() != GameType.CREATIVE) {
            return;
        }

        List<GuiEventListener> doomed = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener.getClass().getName().startsWith(TABS_PACKAGE)) {
                doomed.add(listener);
            }
        }
        for (GuiEventListener listener : doomed) {
            event.removeListener(listener);
        }
    }
}
