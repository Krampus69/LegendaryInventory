package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.compat.LSOCompat;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightTable;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class WeightIconOverlay {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/weight_bar.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 64;
    private static final int ICON_U = 198;
    private static final int ICON_V = 11;
    private static final int ICON_SIZE = 22;
    private static final int HOTBAR_HALF_W = 91;
    private static final int HOTBAR_GAP = 6;
    private static final int ICON_GAP = 2;
    private static final int OFFHAND_W = 29;
    private static final int ATTACK_INDICATOR_W = 18;
    private static final int SNAP = 3;
    private static final float MOVE_Z = 300.0F;

    private static final int NUMBER_CENTER_Y = 13;
    private static final int NUMBER_MAX_W = 18;
    private static final int GLYPH_H = 7;

    private static final int FRACTION_CENTER_Y = 10;
    private static final int FRACTION_LINE_GAP = 2;

    private static final long POPUP_MILLIS = 1000L;
    private static final long MERGE_MILLIS = 250L;
    private static final int POPUP_START = -15;
    private static final int POPUP_END = 3;
    private static final int MAX_POPUPS = 4;
    private static final float POPUP_FADE_IN = 0.15F;
    private static final float POPUP_FADE_OUT = 0.6F;

    private static final float FADE_SECONDS = 0.15F;
    private static final float MIN_ALPHA = 0.02F;

    private static final int TEXT = 0xFFFFFF;
    private static final int GAIN = 0x53DA00;
    private static final int HALF = 0xFFBE2D;
    private static final int NEAR_FULL = 0xFF7C1C;
    private static final int FULL = 0xE63135;
    private static final int HALF_PERCENT = 50;
    private static final int NEAR_FULL_PERCENT = 90;
    private static final int FULL_PERCENT = 100;
    private static final int OUTLINE = 0x000000;

    private static final class Popup {
        int amount;
        final long start;

        Popup(int amount, long start) {
            this.amount = amount;
            this.start = start;
        }
    }

    private static final Deque<Popup> POPUPS = new ArrayDeque<>();
    private static float alpha = 0.0F;
    private static long lastNanos = 0L;

    private static AbstractContainerScreen<?> moveScreen;
    private static int moveX;
    private static int moveY;

    private WeightIconOverlay() {}

    @EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "weight_icon"),
                    WeightIconOverlay::render);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        moveScreen = null;
    }

    private static void clear() {
        POPUPS.clear();
        alpha = 0.0F;
        lastNanos = 0L;
    }

    public static boolean active() {
        return LIConfig.CLIENT.weightBar.get() == LIConfig.WeightBarMode.ICON;
    }

    public static boolean isCustom() {
        return LIConfig.CLIENT.iconMoved.get();
    }

    public static void pop(int amount) {
        if (amount == 0 || !active()) {
            return;
        }
        long now = Util.getMillis();
        Popup last = POPUPS.peekLast();
        if (last != null && now - last.start < MERGE_MILLIS && Integer.signum(last.amount) == Integer.signum(amount)) {
            last.amount += amount;
            return;
        }
        POPUPS.addLast(new Popup(amount, now));
        while (POPUPS.size() > MAX_POPUPS) {
            POPUPS.removeFirst();
        }
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        int width = g.guiWidth();
        int height = g.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!active() || player == null || mc.options.hideGui
                || player.isCreative() || player.isSpectator() || !WeightTable.isReady()) {
            clear();
            return;
        }
        float eased = step(LIKeys.SHOW_WEIGHT.isDown());
        if (moveScreen != null) {
            return;
        }
        int[] at = position(mc, player, width, height);
        drawBag(g, mc, player, at[0], at[1], eased, true);
    }

    public static boolean isMoving(AbstractContainerScreen<?> screen) {
        return moveScreen != null && moveScreen == screen;
    }

    public static void startMove(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int[] at = position(mc, mc.player, screen.width, screen.height);
        moveX = at[0];
        moveY = at[1];
        moveScreen = screen;
    }

    public static void mouseMoved(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!isMoving(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int x = clamp((int) Math.round(mouseX) - ICON_SIZE / 2, screen.width - ICON_SIZE);
        int y = clamp((int) Math.round(mouseY) - ICON_SIZE / 2, screen.height - ICON_SIZE);
        int[] auto = autoPosition(mc, mc.player, screen.width, screen.height);
        moveX = Math.abs(x - auto[0]) <= SNAP ? auto[0] : x;
        moveY = Math.abs(y - auto[1]) <= SNAP ? auto[1] : y;
    }

    public static void finishMove(boolean save) {
        AbstractContainerScreen<?> screen = moveScreen;
        moveScreen = null;
        Minecraft mc = Minecraft.getInstance();
        if (!save || screen == null || mc.player == null) {
            return;
        }
        int[] auto = autoPosition(mc, mc.player, screen.width, screen.height);
        if (moveX == auto[0] && moveY == auto[1]) {
            resetPosition();
            return;
        }
        LIConfig.CLIENT.iconMoved.set(true);
        LIConfig.CLIENT.iconX.set(moveX - screen.width / 2);
        LIConfig.CLIENT.iconY.set(moveY - screen.height);
        LIConfig.CLIENT_SPEC.save();
    }

    public static void cancelMove() {
        moveScreen = null;
    }

    public static void resetPosition() {
        LIConfig.CLIENT.iconMoved.set(false);
        LIConfig.CLIENT.iconX.set(0);
        LIConfig.CLIENT.iconY.set(-ICON_SIZE);
        LIConfig.CLIENT_SPEC.save();
    }

    public static void renderMove(GuiGraphics g, AbstractContainerScreen<?> screen) {
        if (!isMoving(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !WeightTable.isReady()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, MOVE_Z);
        drawBag(g, mc, mc.player, moveX, moveY, 0.0F, false);
        g.pose().popPose();
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private static int[] position(Minecraft mc, LocalPlayer player, int width, int height) {
        if (!isCustom()) {
            return autoPosition(mc, player, width, height);
        }
        int x = clamp(width / 2 + LIConfig.CLIENT.iconX.get(), width - ICON_SIZE);
        int y = clamp(height + LIConfig.CLIENT.iconY.get(), height - ICON_SIZE);
        return new int[] {x, y};
    }

    private static int[] autoPosition(Minecraft mc, LocalPlayer player, int width, int height) {
        int y = height - ICON_SIZE;
        int x = width / 2 + HOTBAR_HALF_W;
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        if (leftHanded && !player.getOffhandItem().isEmpty()) {
            x += OFFHAND_W;
        }
        if (!leftHanded && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            x += HOTBAR_GAP + ATTACK_INDICATOR_W;
        }
        x += ICON_GAP;
        int[] mannequin = LSOCompat.mannequinBounds(player, width, height);
        if (mannequin != null && overlaps(x, y, mannequin)) {
            x = mannequin[0] + mannequin[2] + ICON_GAP;
        }
        return new int[] {x, y};
    }

    private static boolean overlaps(int x, int y, int[] box) {
        return x < box[0] + box[2] && x + ICON_SIZE > box[0] && y < box[1] + box[3] && y + ICON_SIZE > box[1];
    }

    private static void drawBag(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y,
                                float exact, boolean popups) {
        int carried = ClientWeightState.get();
        if (carried < 0) {
            carried = CarryLoad.carriedWeight(player);
        }
        int capacity = CarryLoad.capacity(player);
        int color = loadColor(percent(carried, capacity));
        double scale = mc.getWindow().getGuiScale();
        Font font = mc.font;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        g.blit(TEXTURE, x, y, 0, (float) ICON_U, (float) ICON_V, ICON_SIZE, ICON_SIZE, TEX_W, TEX_H);
        if (carried > 0 && 1.0F - exact >= MIN_ALPHA) {
            drawPercent(g, font, percent(carried, capacity) + "%", x, y, color, 1.0F - exact, scale);
        }
        if (exact >= MIN_ALPHA) {
            drawFraction(g, font, carried, capacity, x, y, color, exact, scale);
        }
        if (popups) {
            drawPopups(g, font, x, y, scale);
        }

        RenderSystem.disableBlend();
    }

    private static int percent(int carried, int capacity) {
        if (capacity <= 0) {
            return 100;
        }
        return (int) ((long) carried * 100L / capacity);
    }

    private static int loadColor(int percent) {
        if (percent >= FULL_PERCENT) {
            return FULL;
        }
        if (percent >= NEAR_FULL_PERCENT) {
            return NEAR_FULL;
        }
        if (percent >= HALF_PERCENT) {
            return HALF;
        }
        return TEXT;
    }

    private static float step(boolean show) {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.0F : (now - lastNanos) / 1.0e9F;
        lastNanos = now;
        float delta = Math.min(dt, 0.1F) / FADE_SECONDS;
        alpha = show ? Math.min(1.0F, alpha + delta) : Math.max(0.0F, alpha - delta);
        return alpha * alpha * (3.0F - 2.0F * alpha);
    }

    private static float snap(float value, double scale) {
        return (float) (Math.round(value * scale) / scale);
    }

    private static float fitScale(int size, int max, double scale) {
        if (size <= max) {
            return 1.0F;
        }
        int pixels = (int) Math.floor(scale * max / size);
        return pixels >= 1 ? (float) (pixels / scale) : 1.0F;
    }

    private static int alphaBits(float opacity) {
        return Math.max(4, Math.round(opacity * 255.0F)) << 24;
    }

    private static void outlined(GuiGraphics g, Font font, String text, int x, int y, int color, float opacity) {
        int a = alphaBits(opacity);
        int shadow = a | OUTLINE;
        g.drawString(font, text, x + 1, y, shadow, false);
        g.drawString(font, text, x - 1, y, shadow, false);
        g.drawString(font, text, x, y + 1, shadow, false);
        g.drawString(font, text, x, y - 1, shadow, false);
        g.drawString(font, text, x, y, a | color, false);
    }

    private static void drawPercent(GuiGraphics g, Font font, String text, int x, int y,
                                    int color, float opacity, double scale) {
        int width = font.width(text);
        float s = fitScale(width, NUMBER_MAX_W, scale);
        float tx = snap(x + (ICON_SIZE - width * s) / 2.0F, scale);
        float ty = snap(y + NUMBER_CENTER_Y - GLYPH_H * s / 2.0F, scale);
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0.0F);
        g.pose().scale(s, s, 1.0F);
        outlined(g, font, text, 0, 0, color, opacity);
        g.pose().popPose();
    }

    private static void drawFraction(GuiGraphics g, Font font, int carried, int capacity,
                                     int x, int y, int color, float opacity, double scale) {
        String top = String.valueOf(carried);
        String bottom = String.valueOf(capacity);
        int block = Math.max(font.width(top), font.width(bottom));
        int lineY = GLYPH_H + FRACTION_LINE_GAP;
        int bottomY = lineY + 1 + FRACTION_LINE_GAP;
        int total = bottomY + GLYPH_H;
        float tx = snap(x + (ICON_SIZE - block) / 2.0F, scale);
        float ty = snap(y + FRACTION_CENTER_Y - total / 2.0F, scale);
        int a = alphaBits(opacity);

        g.pose().pushPose();
        g.pose().translate(tx, ty, 0.0F);
        outlined(g, font, top, (block - font.width(top)) / 2, 0, color, opacity);
        g.fill(-1, lineY - 1, block + 1, lineY + 2, a | OUTLINE);
        g.fill(0, lineY, block, lineY + 1, a | TEXT);
        outlined(g, font, bottom, (block - font.width(bottom)) / 2, bottomY, TEXT, opacity);
        g.pose().popPose();
    }

    private static void drawPopups(GuiGraphics g, Font font, int x, int y, double scale) {
        long now = Util.getMillis();
        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) {
            Popup popup = it.next();
            float t = (now - popup.start) / (float) POPUP_MILLIS;
            if (t >= 1.0F) {
                it.remove();
                continue;
            }
            float eased = t * t * (3.0F - 2.0F * t);
            boolean gain = popup.amount > 0;
            float from = gain ? POPUP_START : POPUP_END;
            float to = gain ? POPUP_END : POPUP_START;
            float opacity;
            if (t < POPUP_FADE_IN) {
                opacity = t / POPUP_FADE_IN;
            } else if (t > POPUP_FADE_OUT) {
                opacity = (1.0F - t) / (1.0F - POPUP_FADE_OUT);
            } else {
                opacity = 1.0F;
            }
            if (opacity < MIN_ALPHA) {
                continue;
            }
            String text = (gain ? "+" : "") + popup.amount;
            float tx = snap(x + (ICON_SIZE - font.width(text)) / 2.0F, scale);
            float ty = snap(y + from + (to - from) * eased, scale);
            g.pose().pushPose();
            g.pose().translate(tx, ty, 0.0F);
            outlined(g, font, text, 0, 0, gain ? GAIN : FULL, opacity);
            g.pose().popPose();
        }
    }
}
