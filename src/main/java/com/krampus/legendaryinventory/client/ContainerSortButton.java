package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.menu.LISlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class ContainerSortButton {

    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/sort_button.png");

    private static final int SIZE = 10;
    private static final int TEX_W = 10;
    private static final int TEX_H = 20;
    private static final int HINT_COLOR = 0x9365A2;
    private static final int RIGHT_MARGIN = 17;
    private static final int TOP_MARGIN = 5;

    private static boolean keyHeld;

    private ContainerSortButton() {}

    public static void setKeyHeld(boolean held) {
        keyHeld = held;
    }

    private static int[] anchor(AbstractContainerScreen<?> screen) {
        boolean augmented = false;
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof LISlot) {
                augmented = true;
                break;
            }
        }
        if (!augmented) {
            return null;
        }
        return new int[] {screen.getXSize() - RIGHT_MARGIN, TOP_MARGIN};
    }

    public static boolean enabled() {
        return LIConfig.COMMON.sortEnabled.get();
    }

    public static boolean hovered(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        if (!enabled()) {
            return false;
        }
        int[] at = anchor(screen);
        if (at == null) {
            return false;
        }
        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= at[0] && relX < at[0] + SIZE && relY >= at[1] && relY < at[1] + SIZE;
    }

    public static void renderForeground(GuiGraphics g, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!enabled()) {
            return;
        }
        int[] at = anchor(screen);
        if (at == null) {
            return;
        }
        boolean hover = keyHeld || hovered(screen, mouseX, mouseY);
        g.blit(TEXTURE, at[0], at[1], 0, hover ? SIZE : 0, SIZE, SIZE, TEX_W, TEX_H);
    }

    public static void renderTooltip(GuiGraphics g, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!enabled()) {
            return;
        }
        if (!hovered(screen, mouseX, mouseY)) {
            return;
        }
        List<Component> tip = List.of(
            Component.translatable("gui.legendaryinventory.sort"),
            Component.translatable("gui.legendaryinventory.sort.weight")
                .withStyle(Style.EMPTY.withColor(HINT_COLOR)));
        g.renderComponentTooltip(Minecraft.getInstance().font, tip, mouseX, mouseY);
    }

    public static void click() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        SortAction.send(Screen.hasControlDown());
    }
}
