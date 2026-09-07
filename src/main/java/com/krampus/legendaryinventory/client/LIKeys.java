package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class LIKeys {

    public static final KeyMapping SORT = new KeyMapping(
        "key.legendaryinventory.sort",
        KeyConflictContext.GUI,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        "key.categories.inventory"
    );

    public static final KeyMapping SHOW_WEIGHT = new KeyMapping(
        "key.legendaryinventory.show_weight",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_TAB,
        "key.categories.inventory"
    );

    private LIKeys() {}

    @EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(SORT);
            event.register(SHOW_WEIGHT);
        }
    }
}
