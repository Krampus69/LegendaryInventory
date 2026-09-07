package com.krampus.legendaryinventory.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClientIntroCue {

    private static final long SHOW_MILLIS = 3000L;
    private static final long FADE_MILLIS = 500L;
    private static final int BOTTOM_OFFSET = 68;
    private static final int PADDING = 4;
    private static final int BACKGROUND = 0x000000;
    private static final int BACKGROUND_ALPHA = 0x90;

    private static boolean pending = false;
    private static Component message = null;
    private static long shownAt = 0L;

    private ClientIntroCue() {}

    public static void arm() {
        pending = true;
    }

    public static boolean consume() {
        boolean was = pending;
        pending = false;
        return was;
    }

    public static void show(Component text) {
        message = text;
        shownAt = Util.getMillis();
    }

    public static void render(GuiGraphics g, Screen screen) {
        if (message == null) {
            return;
        }
        long elapsed = Util.getMillis() - shownAt;
        if (elapsed >= SHOW_MILLIS) {
            message = null;
            return;
        }
        long left = SHOW_MILLIS - elapsed;
        float alpha = left < FADE_MILLIS ? left / (float) FADE_MILLIS : 1.0F;
        int a = Math.max(4, Math.round(alpha * 255.0F));

        Font font = Minecraft.getInstance().font;
        int width = font.width(message);
        int x = (screen.width - width) / 2;
        int y = screen.height - BOTTOM_OFFSET;

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 400.0F);
        int bg = (Math.round(alpha * BACKGROUND_ALPHA) << 24) | BACKGROUND;
        g.fill(x - PADDING, y - PADDING, x + width + PADDING, y + font.lineHeight + PADDING, bg);
        g.drawString(font, message, x, y, (a << 24) | 0xFFFFFF, true);
        g.pose().popPose();
    }
}
