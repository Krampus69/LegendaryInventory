package com.krampus.legendaryinventory.client.gui;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.client.ClientWeightState;
import com.krampus.legendaryinventory.client.LIKeys;
import com.krampus.legendaryinventory.menu.LIMenu;
import com.krampus.legendaryinventory.menu.LISlot;
import com.krampus.legendaryinventory.menu.TrashSlot;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.ScrollPacket;
import com.krampus.legendaryinventory.net.SortPacket;
import com.krampus.legendaryinventory.weight.CarryLoad;
import com.krampus.legendaryinventory.weight.WeightTable;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

public class LIScreen extends EffectRenderingInventoryScreen<LIMenu> implements RecipeUpdateListener {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation CREATIVE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_inventory.png");
    private static final ResourceLocation SORT_BUTTON =
            ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/sort_button.png");

    private static final ResourceLocation WEIGHT_BAR =
            ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "textures/gui/weight_bar.png");

    private static final int BAR_TEX_W = 256;
    private static final int BAR_TEX_H = 64;

    private static final int BAR_BG_X = 0;
    private static final int BAR_BG_Y = 162;
    private static final int BAR_BG_W = 176;
    private static final int BAR_BG_H = 13;

    private static final int BAR_X = 7;
    private static final int BAR_Y = 164;
    private static final int BAR_W = 151;
    private static final int BAR_H = 5;

    private static final int BAR_V_EMPTY = 13;
    private static final int BAR_V_FULL = 18;

    private static final int CREATIVE_W = 195;
    private static final int CREATIVE_H = 136;
    private static final int CREATIVE_SORT_X = 178;
    private static final int PAGE_BUTTON = 20;
    private static final int PAGE_BUTTON_Y = 50;

    private static final int SORT_X = 159;
    private static final int SORT_Y = 5;
    private static final int SORT_SIZE = 10;
    private static final int HINT_COLOR = 0x9365A2;

    private final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
    private CreativeTabStrip tabStrip;
    private TextureButton sortButton;
    private boolean sortKeyHeld;

    private boolean widthTooNarrow;
    private float mouseX;
    private float mouseY;

    private double scrollAccum = 0.0D;
    private float renderScroll = 0.0F;
    private long lastScrollFrame = 0L;

    public LIScreen(LIMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        boolean creative = creative();
        this.imageWidth = creative ? CREATIVE_W : 176;
        this.imageHeight = creative ? CREATIVE_H : 166;

        super.init();
        this.tabStrip = creative ? new CreativeTabStrip() : null;

        if (this.tabStrip != null && this.tabStrip.pageCount() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"),
                            button -> this.tabStrip.previousPage())
                    .pos(this.leftPos, this.topPos - PAGE_BUTTON_Y)
                    .size(PAGE_BUTTON, PAGE_BUTTON)
                    .build());
            addRenderableWidget(Button.builder(Component.literal(">"),
                            button -> this.tabStrip.nextPage())
                    .pos(this.leftPos + this.imageWidth - PAGE_BUTTON, this.topPos - PAGE_BUTTON_Y)
                    .size(PAGE_BUTTON, PAGE_BUTTON)
                    .build());
        }
        this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);

        this.sortButton = new TextureButton(
                this.leftPos + sortX(), this.topPos + SORT_Y, SORT_SIZE, SORT_BUTTON,
                () -> LINet.toServer(new SortPacket(hasControlDown()))) {
            @Override
            public boolean isHoveredOrFocused() {
                return sortKeyHeld || super.isHoveredOrFocused();
            }
        };
        this.sortButton.setTooltip(Tooltip.create(
                Component.translatable("gui.legendaryinventory.sort")
                        .append("\n")
                        .append(Component.translatable("gui.legendaryinventory.sort.weight")
                                .withStyle(Style.EMPTY.withColor(HINT_COLOR)))));
        addRenderableWidget(this.sortButton);

        if (!creative) {
            addRenderableWidget(new ImageButton(
                    this.leftPos + 104, this.height / 2 - 22, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                    button -> {
                        this.recipeBookComponent.toggleVisibility();
                        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
                        button.setPosition(this.leftPos + 104, this.height / 2 - 22);
                        this.sortButton.setPosition(this.leftPos + sortX(), this.topPos + SORT_Y);
                    }));
        }

        addWidget(this.recipeBookComponent);
        setInitialFocus(this.recipeBookComponent);

        this.renderScroll = menu.getScrollRow();
        this.lastScrollFrame = 0L;
    }

    private void clampScrollToRows() {
        int max = menu.maxScrollRow();
        if (menu.getScrollRow() > max) {
            scrollAccum = 0.0D;
            setRow(max);
        }
    }

    private void tickScrollAnimation() {
        long now = Util.getMillis();
        float dt = lastScrollFrame == 0L ? 0.016F : Math.min(0.1F, (now - lastScrollFrame) / 1000.0F);
        lastScrollFrame = now;

        float target = menu.getScrollRow();
        float diff = target - renderScroll;
        if (Math.abs(diff) < 0.005F) {
            renderScroll = target;
        } else {
            renderScroll += diff * Math.min(1.0F, dt * 14.0F);
        }
    }

    private boolean isScrollAnimating() {
        return Math.abs(menu.getScrollRow() - renderScroll) > 0.001F;
    }

    private void setWindowHidden(boolean hidden) {
        for (Slot slot : menu.slots) {
            if (slot instanceof LISlot li) {
                li.setRenderHidden(hidden);
            }
        }
    }

    private ItemStack stackAtWindowPos(int pos) {
        if (pos < 0) {
            return ItemStack.EMPTY;
        }
        int[] filter = menu.getFilter();
        int backing = filter == null
                ? (pos < menu.visibleCount() ? pos : -1)
                : (pos < filter.length ? filter[pos] : -1);
        if (backing < 0 || backing >= menu.getBacking().getSlots()) {
            return ItemStack.EMPTY;
        }
        return menu.getBacking().getStackInSlot(backing);
    }

    private void renderScrollingGrid(GuiGraphics g) {
        int gridLeft = leftPos + gridX() - 1;
        int gridTop = topPos + gridY() - 1;
        int gridRight = gridLeft + LIMenu.COLS * 18;
        int gridBottom = gridTop + LIMenu.VISIBLE_ROWS * 18;

        g.enableScissor(gridLeft, gridTop, gridRight, gridBottom);

        int firstRow = (int) Math.floor(renderScroll);
        for (int r = 0; r <= LIMenu.VISIBLE_ROWS; r++) {
            int row = firstRow + r;
            if (row < 0) {
                continue;
            }
            int sy = topPos + gridY() + Math.round((row - renderScroll) * 18.0F);
            for (int col = 0; col < LIMenu.COLS; col++) {
                int sx = leftPos + gridX() + col * 18;
                g.blit(creative() ? CREATIVE_TEXTURE : TEXTURE, sx - 1, sy - 1,
                        gridX() - 1, gridY() - 1, 18, 18);

                ItemStack stack = stackAtWindowPos(row * LIMenu.COLS + col);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, sx, sy);
                    g.renderItemDecorations(this.font, stack, sx, sy);
                }
            }
        }

        g.disableScissor();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.recipeBookComponent.tick();
    }

    private void setRow(int row) {
        int clamped = Math.max(0, Math.min(row, menu.maxScrollRow()));
        if (clamped == menu.getScrollRow()) {
            return;
        }
        menu.setScrollRow(clamped);
        LINet.toServer(new ScrollPacket(clamped));

    }

    @Override
    public boolean mouseScrolled(double mx, double my, double deltaX, double delta) {
        if (super.mouseScrolled(mx, my, deltaX, delta)) {
            return true;
        }
        if (hasControlDown()) {
            return false;
        }
        if (menu.maxScrollRow() <= 0) {
            return false;
        }

        double rowsPerNotch = hasShiftDown() ? LIMenu.VISIBLE_ROWS : 1.0D;
        scrollAccum -= delta * rowsPerNotch;

        int step = (int) scrollAccum;
        if (step != 0) {
            scrollAccum -= step;
            int target = menu.getScrollRow() + step;
            if (target < 0 || target > menu.maxScrollRow()) {
                scrollAccum = 0.0D;
            }
            setRow(target);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (tabStrip != null
                && tabStrip.mouseClicked(mx, my, leftPos, topPos, imageWidth, imageHeight)) {
            return true;
        }
        if (this.recipeBookComponent.mouseClicked(mx, my, button)) {
            setFocused(this.recipeBookComponent);
            return true;
        }
        if (this.widthTooNarrow && this.recipeBookComponent.isVisible()) {
            return false;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        boolean outside = mx < (double) left || my < (double) top
                || mx >= (double) (left + this.imageWidth) || my >= (double) (top + this.imageHeight);
        return this.recipeBookComponent.hasClickedOutside(
                mx, my, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, button) && outside;
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mx, double my) {
        return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible())
                && super.isHovering(x, y, w, h, mx, my);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        super.slotClicked(slot, slotId, button, type);
        this.recipeBookComponent.slotClicked(slot);
    }

    @Override
    public void recipesUpdated() {
        this.recipeBookComponent.recipesUpdated();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return this.recipeBookComponent;
    }

    private int gridX() {
        return creative() ? LIMenu.CREATIVE_MAIN_X : LIMenu.MAIN_X;
    }

    private int gridY() {
        return creative() ? LIMenu.CREATIVE_MAIN_Y : LIMenu.MAIN_Y;
    }

    private int sortX() {
        return creative() ? CREATIVE_SORT_X : SORT_X;
    }

    private boolean creative() {
        return this.minecraft != null
                && this.minecraft.gameMode != null
                && this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE;
    }

    private static boolean isSortKey(int keyCode, int scanCode) {
        InputConstants.Key key = LIKeys.SORT.getKey();
        if (key.getType() == InputConstants.Type.SCANCODE) {
            return key.getValue() == scanCode;
        }
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == keyCode;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (isSortKey(keyCode, scanCode)) {
            this.sortKeyHeld = true;
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            LINet.toServer(new SortPacket(hasControlDown()));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isSortKey(keyCode, scanCode)) {
            this.sortKeyHeld = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        if (tabStrip != null) {
            tabStrip.renderTabs(g, this.font, leftPos, topPos, imageWidth, imageHeight);
        }

        g.blit(creative() ? CREATIVE_TEXTURE : TEXTURE,
                leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (tabStrip != null) {
            tabStrip.renderSelectedTab(g, this.font, leftPos, topPos, imageWidth, imageHeight);
        }

        renderWeightBar(g);

        if (isScrollAnimating()) {
            renderScrollingGrid(g);
        }

        if (minecraft != null && minecraft.player != null) {
            int scale = creative() ? 20 : 30;
            int x1 = leftPos + (creative() ? 73 : 26);
            int y1 = topPos + (creative() ? 6 : 8);
            int x2 = leftPos + (creative() ? 105 : 75);
            int y2 = topPos + (creative() ? 49 : 78);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, x1, y1, x2, y2, scale, 0.0625F, this.mouseX, this.mouseY, minecraft.player);
        }
    }

    private boolean weightBarVisible() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        LocalPlayer player = this.minecraft.player;
        return !player.isCreative() && !player.isSpectator() && WeightTable.isReady();
    }

    private int carriedWeight() {
        int synced = ClientWeightState.get();
        if (synced >= 0) {
            return synced;
        }
        return this.minecraft == null || this.minecraft.player == null
                ? 0
                : CarryLoad.carriedWeight(this.minecraft.player);
    }

    private void renderTrashTooltip(GuiGraphics g, int mx, int my) {
        if (this.hoveredSlot instanceof TrashSlot && getMenu().getCarried().isEmpty()) {
            g.renderTooltip(this.font, Component.translatable("inventory.binSlot"), mx, my);
        }
    }

    private void renderWeightTooltip(GuiGraphics g, int mx, int my) {
        if (!weightBarVisible()) {
            return;
        }
        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            return;
        }

        int x = leftPos + BAR_BG_X;
        int y = topPos + BAR_BG_Y;
        if (mx < x || mx >= x + BAR_BG_W || my < y || my >= y + BAR_BG_H) {
            return;
        }

        int carried = carriedWeight();
        int capacity = CarryLoad.capacity(this.minecraft.player);
        int tier = CarryLoad.tierFor(carried, capacity);
        int percent = capacity <= 0 ? 0 : Math.round(carried * 100.0F / capacity);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.legendaryinventory.weight",
                carried, capacity).withStyle(ChatFormatting.WHITE));
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

        g.renderComponentTooltip(this.font, lines, mx, my);
    }

    private void renderWeightBar(GuiGraphics g) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        LocalPlayer player = this.minecraft.player;
        if (!weightBarVisible()) {
            return;
        }

        g.blit(WEIGHT_BAR, leftPos + BAR_BG_X, topPos + BAR_BG_Y, 0,
                0.0F, 0.0F, BAR_BG_W, BAR_BG_H, BAR_TEX_W, BAR_TEX_H);

        g.blit(WEIGHT_BAR, leftPos + BAR_X, topPos + BAR_Y, 0,
                0.0F, (float) BAR_V_EMPTY, BAR_W, BAR_H, BAR_TEX_W, BAR_TEX_H);

        int carried = carriedWeight();
        int capacity = CarryLoad.capacity(player);
        float ratio = capacity <= 0 ? 1.0F : carried / (float) capacity;
        int filled = Math.round(Math.min(Math.max(ratio, 0.0F), 1.0F) * BAR_W);

        if (filled > 0) {
            g.blit(WEIGHT_BAR, leftPos + BAR_X, topPos + BAR_Y, 0,
                    0.0F, (float) BAR_V_FULL, filled, BAR_H, BAR_TEX_W, BAR_TEX_H);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        if (creative()) {
            return;
        }
        g.drawString(this.font, Component.translatable("container.crafting"), 97, 8, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        clampScrollToRows();
        tickScrollAnimation();
        setWindowHidden(isScrollAnimating());

        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            renderBackground(g, mx, my, partial);
            this.recipeBookComponent.render(g, mx, my, partial);
        } else {
            this.recipeBookComponent.render(g, mx, my, partial);
            super.render(g, mx, my, partial);
            this.recipeBookComponent.renderGhostRecipe(g, this.leftPos, this.topPos, false, partial);
        }

        if (tabStrip != null) {
            tabStrip.renderPageLabel(g, this.font, leftPos, topPos, imageWidth);
        }

        renderTooltip(g, mx, my);
        this.recipeBookComponent.renderTooltip(g, this.leftPos, this.topPos, mx, my);
        renderWeightTooltip(g, mx, my);
        renderTrashTooltip(g, mx, my);

        if (tabStrip != null) {
            tabStrip.renderTooltip(g, this.font, leftPos, topPos, imageWidth, imageHeight, mx, my);
        }

        this.mouseX = mx;
        this.mouseY = my;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class TextureButton extends AbstractButton {

        private final ResourceLocation texture;
        private final int size;
        private final Runnable action;

        TextureButton(int x, int y, int size, ResourceLocation texture, Runnable action) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
            this.size = size;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            int v = isHoveredOrFocused() ? size : 0;
            g.blit(texture, getX(), getY(), 0, 0.0F, (float) v, size, size, size, size * 2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
