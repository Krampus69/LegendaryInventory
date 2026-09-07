package com.krampus.legendaryinventory.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.List;

public final class CreativeTabStrip {

    private static final String SELECTED_TAB = "f_98507_";

    private static final int VIRTUAL_WIDTH = 195;
    private static final int TAB_W = 26;
    private static final int TAB_H = 32;
    private static final int SPACING = 27;
    private static final int COLUMNS = 7;
    private static final int TABS_PER_PAGE = 10;

    private static final int TOP_OFFSET = 28;
    private static final int BOTTOM_INSET = 4;
    private static final int PAGE_LABEL_Y = 44;

    private static final int V_TOP = 0;
    private static final int V_TOP_SELECTED = 32;
    private static final int V_BOTTOM = 64;
    private static final int V_BOTTOM_SELECTED = 96;


    private final List<CreativeTabsScreenPage> pages = new ArrayList<>();
    private CreativeTabsScreenPage page;

    public CreativeTabStrip() {
        rebuild();
    }

    private void rebuild() {
        pages.clear();
        List<CreativeModeTab> current = new ArrayList<>();
        int index = 0;

        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            current.add(tab);
            index++;
            if (index == TABS_PER_PAGE) {
                pages.add(new CreativeTabsScreenPage(current));
                current = new ArrayList<>();
                index = 0;
            }
        }
        if (index != 0) {
            pages.add(new CreativeTabsScreenPage(current));
        }

        page = pages.isEmpty()
            ? new CreativeTabsScreenPage(new ArrayList<>())
            : pages.get(0);

        CreativeModeTab selected = selectedTab();
        if (selected == null) {
            return;
        }
        for (CreativeTabsScreenPage candidate : pages) {
            if (candidate.getVisibleTabs().contains(selected)) {
                page = candidate;
                return;
            }
        }
    }

    public static boolean inventoryTabSelected() {
        CreativeModeTab tab = selectedTab();
        return tab != null && tab.getType() == CreativeModeTab.Type.INVENTORY;
    }

    private static CreativeModeTab selectedTab() {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(
                CreativeModeInventoryScreen.class, null, SELECTED_TAB);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean selectTab(CreativeModeTab tab) {
        try {
            ObfuscationReflectionHelper.setPrivateValue(
                CreativeModeInventoryScreen.class, null, tab, SELECTED_TAB);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int baseX(int leftPos, int imageWidth) {
        return leftPos - (VIRTUAL_WIDTH - imageWidth) / 2;
    }

    private int tabX(CreativeModeTab tab, int leftPos, int imageWidth) {
        int column = page.getColumn(tab);
        int offset = tab.isAlignedRight()
            ? VIRTUAL_WIDTH - SPACING * (COLUMNS - column) + 1
            : SPACING * column;
        return baseX(leftPos, imageWidth) + offset;
    }

    private int tabY(CreativeModeTab tab, int topPos, int imageHeight) {
        return page.isTop(tab) ? topPos - TOP_OFFSET : topPos + imageHeight - BOTTOM_INSET;
    }

    public void renderTabs(GuiGraphics g, Font font, int leftPos, int topPos,
                           int imageWidth, int imageHeight) {
        CreativeModeTab selected = selectedTab();
        for (CreativeModeTab tab : page.getVisibleTabs()) {
            if (tab != selected) {
                drawTab(g, font, tab, leftPos, topPos, imageWidth, imageHeight, false);
            }
        }
    }

    public void renderSelectedTab(GuiGraphics g, Font font, int leftPos, int topPos,
                                  int imageWidth, int imageHeight) {
        CreativeModeTab selected = selectedTab();
        if (selected == null || !page.getVisibleTabs().contains(selected)) {
            return;
        }
        drawTab(g, font, selected, leftPos, topPos, imageWidth, imageHeight, true);
    }

    private void drawTab(GuiGraphics g, Font font, CreativeModeTab tab, int leftPos, int topPos,
                         int imageWidth, int imageHeight, boolean selected) {
        boolean top = page.isTop(tab);
        int x = tabX(tab, leftPos, imageWidth);
        int y = tabY(tab, topPos, imageHeight);
        int u = page.getColumn(tab) * TAB_W;
        int v;
        if (top) {
            v = selected ? V_TOP_SELECTED : V_TOP;
        } else {
            v = selected ? V_BOTTOM_SELECTED : V_BOTTOM;
        }

        g.blit(tab.getTabsImage(), x, y, u, v, TAB_W, TAB_H);

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 100.0F);
        int iconX = x + 5;
        int iconY = y + 8 + (top ? 1 : -1);
        ItemStack icon = tab.getIconItem();
        g.renderItem(icon, iconX, iconY);
        g.renderItemDecorations(font, icon, iconX, iconY);
        g.pose().popPose();
    }

    public int pageCount() {
        return pages.size();
    }

    public void previousPage() {
        int index = pages.indexOf(page);
        page = pages.get(Math.max(index - 1, 0));
    }

    public void nextPage() {
        int index = pages.indexOf(page);
        page = pages.get(Math.min(index + 1, pages.size() - 1));
    }

    public void renderPageLabel(GuiGraphics g, Font font, int leftPos, int topPos, int imageWidth) {
        if (pages.size() < 2) {
            return;
        }
        String label = (pages.indexOf(page) + 1) + " / " + pages.size();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        g.drawString(font, label,
            leftPos + imageWidth / 2 - font.width(label) / 2,
            topPos - PAGE_LABEL_Y, 0xFFFFFFFF);
        g.pose().popPose();
    }

    public void renderTooltip(GuiGraphics g, Font font, int leftPos, int topPos,
                              int imageWidth, int imageHeight, int mouseX, int mouseY) {
        CreativeModeTab hovered = tabAt(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
        if (hovered != null) {
            g.renderTooltip(font, hovered.getDisplayName(), mouseX, mouseY);
        }
    }

    private CreativeModeTab tabAt(double mouseX, double mouseY, int leftPos, int topPos,
                                  int imageWidth, int imageHeight) {
        for (CreativeModeTab tab : page.getVisibleTabs()) {
            int x = tabX(tab, leftPos, imageWidth);
            int y = tabY(tab, topPos, imageHeight);
            if (mouseX >= x && mouseX <= x + TAB_W && mouseY >= y && mouseY <= y + TAB_H) {
                return tab;
            }
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int leftPos, int topPos,
                                int imageWidth, int imageHeight) {
        CreativeModeTab tab = tabAt(mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
        if (tab == null) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (!selectTab(tab)) {
            return false;
        }

        mc.setScreen(new CreativeModeInventoryScreen(
            mc.player,
            mc.player.connection.enabledFeatures(),
            mc.options.operatorItemsTab().get()));
        return true;
    }
}
