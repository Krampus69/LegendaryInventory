package com.krampus.legendaryinventory.compat;

import net.minecraft.client.gui.screens.Screen;

public final class AE2Compat {

    private static final String BASE_SCREEN = "appeng.client.gui.AEBaseScreen";

    private static final ClassValue<Boolean> AE2_SCREEN = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                if (BASE_SCREEN.equals(c.getName())) {
                    return true;
                }
            }
            return false;
        }
    };

    private AE2Compat() {}

    public static boolean isAE2Screen(Screen screen) {
        return screen != null && AE2_SCREEN.get(screen.getClass());
    }
}
