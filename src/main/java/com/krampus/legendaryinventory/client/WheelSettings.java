package com.krampus.legendaryinventory.client;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;

public record WheelSettings(boolean enabled, boolean lastToFirst, String direction, boolean alwaysOne) {

    private static final WheelSettings FALLBACK =
        new WheelSettings(true, true, "NORMAL", false);

    private static boolean resolved = false;
    private static Field configField;
    private static Field enabledField;
    private static Field orderField;
    private static Field directionField;
    private static Field scalingField;

    public boolean inverted() {
        return direction.endsWith("INVERTED");
    }

    public boolean positionAware() {
        return direction.startsWith("INVENTORY_POSITION_AWARE");
    }

    public static WheelSettings current() {
        if (!resolve()) {
            return FALLBACK;
        }
        try {
            Object config = configField.get(null);
            if (config == null) {
                return FALLBACK;
            }
            return new WheelSettings(
                enabledField.getBoolean(config),
                name(orderField.get(config)).equals("LAST_TO_FIRST"),
                name(directionField.get(config)),
                name(scalingField.get(config)).equals("ALWAYS_ONE")
            );
        } catch (ReflectiveOperationException | RuntimeException e) {
            return FALLBACK;
        }
    }

    private static String name(Object value) {
        return value instanceof Enum<?> constant ? constant.name() : "";
    }

    private static boolean resolve() {
        if (resolved) {
            return configField != null;
        }
        resolved = true;
        if (!ModList.get().isLoaded("mousetweaks")) {
            return false;
        }
        try {
            Class<?> main = Class.forName("yalter.mousetweaks.Main");
            Class<?> config = Class.forName("yalter.mousetweaks.Config");
            configField = main.getDeclaredField("config");
            enabledField = config.getDeclaredField("wheelTweak");
            orderField = config.getDeclaredField("wheelSearchOrder");
            directionField = config.getDeclaredField("wheelScrollDirection");
            scalingField = config.getDeclaredField("scrollItemScaling");
            configField.setAccessible(true);
            enabledField.setAccessible(true);
            orderField.setAccessible(true);
            directionField.setAccessible(true);
            scalingField.setAccessible(true);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            configField = null;
            return false;
        }
    }
}
