package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.FilterPacket;
import com.krampus.legendaryinventory.net.LINet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class InventorySearchBar {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(LegendaryInventory.MODID, "textures/gui/weight_bar.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 64;

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 175;
    private static final int FRAME_U = 0;
    private static final int FRAME_V = 43;
    private static final int FRAME_W = 156;
    private static final int FRAME_H = 19;
    private static final int FRAME_X = (PANEL_W - FRAME_W) / 2;
    private static final int FRAME_Y = PANEL_H;

    private static final int TEXT_X = 9;
    private static final int TEXT_Y = 4;
    private static final int TEXT_RIGHT = 5;
    private static final int TEXT_W = FRAME_W - TEXT_X - TEXT_RIGHT;
    private static final int TEXT_H = 8;
    private static final int MAX_LENGTH = 50;

    private static final Map<String, String> MOD_NAMES = new HashMap<>();

    private static boolean open = false;
    private static String query = "";
    private static EditBox box = null;
    private static AbstractContainerScreen<?> screen = null;

    private InventorySearchBar() {}

    public static boolean isFocused() {
        return open && box != null && box.isFocused();
    }

    public static void attach(AbstractContainerScreen<?> target) {
        screen = target;
        box = new EditBox(Minecraft.getInstance().font, 0, 0, TEXT_W, TEXT_H, Component.empty());
        box.setBordered(false);
        box.setMaxLength(MAX_LENGTH);
        box.setValue(query);
        box.setResponder(InventorySearchBar::onChanged);
        position();
        if (open && !query.isEmpty()) {
            applyFilter();
        }
    }

    public static void detach() {
        if (screen != null) {
            ScrollContext context = ScrollRegistry.get(screen.getMenu());
            if (context != null && context.getFilter() != null) {
                context.setFilter(null);
                LINet.toServer(new FilterPacket((int[]) null));
            }
        }
        open = false;
        query = "";
        screen = null;
        box = null;
    }

    public static void toggle() {
        open = !open;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (box == null || screen == null) {
            return;
        }
        if (open) {
            focus(true);
        } else {
            focus(false);
            if (!query.isEmpty()) {
                box.setValue("");
            }
        }
    }

    private static void focus(boolean focused) {
        box.setFocused(focused);
        screen.setFocused(focused ? box : null);
    }

    public static void clear() {
        query = "";
        if (box != null && !box.getValue().isEmpty()) {
            box.setValue("");
        }
    }

    public static void tick() {
        if (open && box != null) {
            box.tick();
        }
    }

    public static void render(GuiGraphics g, int mx, int my, float partial) {
        if (!open || box == null) {
            return;
        }
        position();
        g.blit(TEXTURE, screen.getGuiLeft() + FRAME_X, screen.getGuiTop() + FRAME_Y, 0,
            (float) FRAME_U, (float) FRAME_V, FRAME_W, FRAME_H, TEX_W, TEX_H);
        box.render(g, mx, my, partial);
    }

    public static boolean mouseClicked(double mx, double my, int button) {
        if (!open || box == null || screen == null) {
            return false;
        }
        int fx = screen.getGuiLeft() + FRAME_X;
        int fy = screen.getGuiTop() + FRAME_Y;
        boolean inside = mx >= fx && mx < fx + FRAME_W && my >= fy && my < fy + FRAME_H;
        if (inside) {
            focus(true);
            if (button == 1) {
                if (!box.getValue().isEmpty()) {
                    box.setValue("");
                }
                return true;
            }
            return box.mouseClicked(mx, my, button);
        }
        if (box.isFocused()) {
            focus(false);
        }
        return false;
    }

    public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if (Screen.isSelectAll(keyCode)) {
            box.moveCursorToEnd();
            box.setHighlightPos(0);
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(box.getHighlighted());
            return true;
        }
        if (Screen.isCut(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(box.getHighlighted());
            box.insertText("");
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            box.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        box.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    public static boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        box.charTyped(codePoint, modifiers);
        return true;
    }

    private static void position() {
        if (box != null && screen != null) {
            box.setX(screen.getGuiLeft() + FRAME_X + TEXT_X);
            box.setY(screen.getGuiTop() + FRAME_Y + TEXT_Y);
        }
    }

    private static void onChanged(String text) {
        query = text;
        applyFilter();
    }

    private static void applyFilter() {
        if (screen == null) {
            return;
        }
        ScrollContext context = ScrollRegistry.get(screen.getMenu());
        if (context == null) {
            return;
        }
        int[] filter = compute(context.getBacking(), query);
        context.setFilter(filter);
        ScrollAnimator.reset(context);
        LINet.toServer(new FilterPacket(filter));
    }

    private static int[] compute(CombinedInventoryHandler backing, String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return null;
        }
        boolean byMod = needle.startsWith("@");
        if (byMod) {
            needle = needle.substring(1);
        }
        int slots = backing.getSlots();
        int[] found = new int[slots];
        int count = 0;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = backing.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (matches(stack, needle, byMod)) {
                found[count++] = i;
            }
        }
        int[] result = new int[count];
        System.arraycopy(found, 0, result, 0, count);
        return result;
    }

    private static boolean matches(ItemStack stack, String needle, boolean byMod) {
        if (!byMod && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        String namespace = key.getNamespace();
        return namespace.contains(needle) || modName(namespace).contains(needle);
    }

    private static String modName(String namespace) {
        return MOD_NAMES.computeIfAbsent(namespace, ns -> ModList.get().getModContainerById(ns)
            .map(container -> container.getModInfo().getDisplayName().toLowerCase(Locale.ROOT))
            .orElse(ns));
    }
}
