package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.gui.LIScreen;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightTable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class InventoryWeightBar {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/weight_bar.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 64;

    private static final int BG_X = 0;
    private static final int BG_Y = 162;
    private static final int BG_W = 176;
    private static final int BG_H = 13;

    private static final int BAR_X = 7;
    private static final int BAR_Y = 164;
    private static final int BAR_W = 141;
    private static final int BAR_H = 5;

    private static final int V_EMPTY = 13;
    private static final int V_FULL = 18;

    private static final int NARROW_WIDTH = 379;
    private static final int SEARCH_X = 151;
    private static final int SEARCH_Y = 0;
    private static final int SEARCH_SIZE = 8;

    private InventoryWeightBar() {}

    private static LocalPlayer visibleFor(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof InventoryScreen) || screen instanceof LIScreen) {
            return null;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator() || !WeightTable.isReady()) {
            return null;
        }
        return player;
    }

    private static int carried(LocalPlayer player) {
        int synced = ClientWeightState.get();
        return synced >= 0 ? synced : CarryLoad.carriedWeight(player);
    }

    public static boolean hovered(AbstractContainerScreen<?> screen, double mx, double my) {
        if (visibleFor(screen) == null) {
            return false;
        }
        int x = screen.getGuiLeft() + BG_X;
        int y = screen.getGuiTop() + BG_Y;
        return mx >= x && mx < x + BG_W && my >= y && my < y + BG_H;
    }

    public static boolean searchHovered(AbstractContainerScreen<?> screen, double mx, double my) {
        if (visibleFor(screen) == null) {
            return false;
        }
        int x = screen.getGuiLeft() + BG_X + SEARCH_X;
        int y = screen.getGuiTop() + BG_Y + SEARCH_Y;
        return mx >= x && mx < x + SEARCH_SIZE && my >= y && my < y + SEARCH_SIZE;
    }

    public static boolean available(AbstractContainerScreen<?> screen) {
        return visibleFor(screen) != null;
    }

    public static void render(GuiGraphics g, AbstractContainerScreen<?> screen) {
        LocalPlayer player = visibleFor(screen);
        if (player == null) {
            return;
        }
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();

        g.blit(TEXTURE, left + BG_X, top + BG_Y, 0, 0.0F, 0.0F, BG_W, BG_H, TEX_W, TEX_H);
        g.blit(TEXTURE, left + BAR_X, top + BAR_Y, 0, 0.0F, (float) V_EMPTY, BAR_W, BAR_H, TEX_W, TEX_H);

        int carried = carried(player);
        int capacity = CarryLoad.capacity(player);
        float ratio = capacity <= 0 ? 1.0F : carried / (float) capacity;
        int filled = Math.round(Math.min(Math.max(ratio, 0.0F), 1.0F) * BAR_W);
        if (filled > 0) {
            g.blit(TEXTURE, left + BAR_X, top + BAR_Y, 0, 0.0F, (float) V_FULL, filled, BAR_H, TEX_W, TEX_H);
        }
    }

    public static void renderTooltip(GuiGraphics g, AbstractContainerScreen<?> screen, int mx, int my) {
        LocalPlayer player = visibleFor(screen);
        if (player == null) {
            return;
        }
        InventoryScreen inventory = (InventoryScreen) screen;
        if (inventory.getRecipeBookComponent().isVisible() && screen.width < NARROW_WIDTH) {
            return;
        }

        if (searchHovered(screen, mx, my)) {
            g.renderTooltip(Minecraft.getInstance().font,
                Component.translatable("gui.legendaryinventory.weight.search"), mx, my);
            return;
        }

        int x = screen.getGuiLeft() + BG_X;
        int y = screen.getGuiTop() + BG_Y;
        if (mx < x || mx >= x + BG_W || my < y || my >= y + BG_H) {
            return;
        }

        int carried = carried(player);
        int capacity = CarryLoad.capacity(player);
        int tier = CarryLoad.tierFor(carried, capacity);
        int percent = capacity <= 0 ? 0 : Math.round(carried * 100.0F / capacity);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.legendaryinventory.weight", carried, capacity)
            .withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("gui.legendaryinventory.weight.percent", percent)
            .withStyle(tier == 0 ? ChatFormatting.GRAY : ChatFormatting.RED));

        if (tier > 0) {
            double penalty = CarryLoad.penaltyFor(carried, capacity);
            if (penalty <= -1.0D) {
                lines.add(Component.translatable("gui.legendaryinventory.weight.immobile")
                    .withStyle(ChatFormatting.DARK_RED));
            } else {
                int slow = (int) Math.round(-penalty * 100.0D);
                lines.add(Component.translatable("gui.legendaryinventory.weight.overloaded", slow)
                    .withStyle(ChatFormatting.RED));
            }
        }

        g.renderComponentTooltip(Minecraft.getInstance().font, lines, mx, my);
    }
}
