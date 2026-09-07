package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightTable;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class WeightBarOverlay {

    private static final ResourceLocation WEIGHT_BAR =
            ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/weight_bar.png");

    private static final ResourceLocation XP_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation XP_PROGRESS = ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

    private static final int TEX_W = 256;
    private static final int TEX_H = 64;

    private static final int BAR_W = 182;
    private static final int BAR_H = 5;
    private static final int V_EMPTY = 23;
    private static final int V_FULL = 28;
    private static final int V_HEAVY = 33;
    private static final int V_CRITICAL = 38;

    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int LABEL_SHADOW = 0x000000;

    private static final float FADE_SECONDS = 0.25F;
    private static final float MIN_ALPHA = 0.02F;

    private static float alpha = 0.0F;
    private static long lastNanos = 0L;

    private WeightBarOverlay() {}

    private static int fillRow(int tier) {
        if (tier >= 4) {
            return V_CRITICAL;
        }
        if (tier == 3) {
            return V_HEAVY;
        }
        return V_FULL;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientWeightState.reset();
        alpha = 0.0F;
        lastNanos = 0L;
    }

    private static float step(boolean show) {
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0.0F : (now - lastNanos) / 1.0e9F;
        lastNanos = now;
        float delta = Math.min(dt, 0.1F) / FADE_SECONDS;
        alpha = show ? Math.min(1.0F, alpha + delta) : Math.max(0.0F, alpha - delta);
        return alpha * alpha * (3.0F - 2.0F * alpha);
    }

    @SubscribeEvent
    public static void onRenderExperience(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        LIConfig.WeightBarMode mode = LIConfig.CLIENT.weightBar.get();
        if (mode == LIConfig.WeightBarMode.OFF || player == null || mc.options.hideGui
                || player.isCreative() || player.isSpectator() || !WeightTable.isReady()) {
            alpha = 0.0F;
            lastNanos = 0L;
            return;
        }

        int carried = ClientWeightState.get();
        if (carried < 0) {
            carried = CarryLoad.carriedWeight(player);
        }
        int capacity = CarryLoad.capacity(player);
        boolean stalled = CarryLoad.stalled(carried, capacity);

        boolean held = LIKeys.SHOW_WEIGHT.isDown();
        boolean show = mode == LIConfig.WeightBarMode.ALWAYS || held || stalled || ClientWeightState.flashing();
        float eased = step(show);
        if (eased < MIN_ALPHA) {
            return;
        }

        event.setCanceled(true);

        GuiGraphics g = event.getGuiGraphics();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int x = width / 2 - BAR_W / 2;
        int y = height - 32 + 3;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (eased < 1.0F && mc.gameMode != null && mc.gameMode.hasExperience()) {
            g.blitSprite(XP_BACKGROUND, x, y, BAR_W, BAR_H);
            int xpFilled = (int) (player.experienceProgress * (BAR_W + 1));
            if (xpFilled > 0) {
                g.blitSprite(XP_PROGRESS, BAR_W, BAR_H, 0, 0, x, y, xpFilled, BAR_H);
            }
        }

        g.setColor(1.0F, 1.0F, 1.0F, eased);

        g.blit(WEIGHT_BAR, x, y, 0, 0.0F, (float) V_EMPTY, BAR_W, BAR_H, TEX_W, TEX_H);

        float ratio = capacity <= 0 ? 1.0F : carried / (float) capacity;
        int filled = Math.round(Math.min(Math.max(ratio, 0.0F), 1.0F) * BAR_W);

        if (filled > 0) {
            g.blit(WEIGHT_BAR, x, y, 0,
                    0.0F, (float) fillRow(CarryLoad.tierFor(carried, capacity)),
                    filled, BAR_H, TEX_W, TEX_H);
        }

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        drawLabel(g, mc.font, carried + " / " + capacity, width, height, eased);
        RenderSystem.disableBlend();
    }

    private static void drawLabel(GuiGraphics g, Font font, String label, int width, int height, float opacity) {
        int textX = (width - font.width(label)) / 2;
        int textY = height - 30;
        int a = Math.max(5, Math.round(opacity * 255.0F)) << 24;
        int shadow = a | LABEL_SHADOW;
        int color = a | LABEL_COLOR;

        g.drawString(font, label, textX + 1, textY, shadow, false);
        g.drawString(font, label, textX - 1, textY, shadow, false);
        g.drawString(font, label, textX, textY + 1, shadow, false);
        g.drawString(font, label, textX, textY - 1, shadow, false);
        g.drawString(font, label, textX, textY, color, false);
    }
}
