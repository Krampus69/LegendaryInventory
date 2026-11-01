package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class LIKeys {

    private static final String CATEGORY = "key.categories.legendaryinventory";

    private static final IKeyConflictContext SORT_CONTEXT = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return KeyConflictContext.GUI.isActive();
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other != this && KeyConflictContext.GUI.conflicts(other);
        }
    };

    public static final KeyMapping SORT_INVENTORY = new KeyMapping(
        "key.legendaryinventory.sort",
        SORT_CONTEXT,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    public static final KeyMapping SORT_CONTAINER = new KeyMapping(
        "key.legendaryinventory.sort_container",
        SORT_CONTEXT,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    public static final KeyMapping SHOW_WEIGHT = new KeyMapping(
        "key.legendaryinventory.show_weight",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_TAB,
        CATEGORY
    );

    public static final KeyMapping EXPAND = new KeyMapping(
        "key.legendaryinventory.expand",
        KeyConflictContext.GUI,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_TAB,
        CATEGORY
    );

    public static final KeyMapping SEARCH = new KeyMapping(
        "key.legendaryinventory.search",
        KeyConflictContext.GUI,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_GRAVE_ACCENT,
        CATEGORY
    );

    private LIKeys() {}

    public static boolean matches(KeyMapping mapping, int keyCode, int scanCode) {
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.SCANCODE) {
            return key.getValue() == scanCode;
        }
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == keyCode;
    }

    @EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(SORT_INVENTORY);
            event.register(SORT_CONTAINER);
            event.register(SHOW_WEIGHT);
            event.register(EXPAND);
            event.register(SEARCH);
        }
    }
}
