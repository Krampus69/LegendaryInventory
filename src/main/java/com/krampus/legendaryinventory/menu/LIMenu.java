package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.MirrorPacket;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LIMenu extends RecipeBookMenu<CraftingContainer> {

    public static final int COLS = 9;
    public static final int VISIBLE_ROWS = 3;
    public static final int WINDOW = COLS * VISIBLE_ROWS;

    public static final int RESULT = 0;
    public static final int ARMOR_START = 5;
    public static final int MAIN_START = 9;
    public static final int HOTBAR_START = MAIN_START + WINDOW;
    public static final int HOTBAR_END = HOTBAR_START + 9;
    public static final int OFFHAND = HOTBAR_END;
    public static final int END = OFFHAND + 1;
    public static final int TRASH = END;

    private static final int TRASH_X = 173;
    private static final int TRASH_Y = 112;

    public static final int MAIN_Y = 84;
    public static final int HOTBAR_Y = 142;

    public static final int MAIN_X = 8;
    public static final int CREATIVE_MAIN_X = 9;
    public static final int CREATIVE_MAIN_Y = 54;
    public static final int CREATIVE_HOTBAR_Y = 112;
    private static final int HIDDEN = -2000;
    private static final int[][] CREATIVE_ARMOR = {{54, 6}, {54, 33}, {108, 6}, {108, 33}};

    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final ResourceLocation[] ARMOR_ICONS = {
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    private final CombinedInventoryHandler backing;
    private final ScrollContext context;
    private final Player owner;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();



    public LIMenu(int id, Inventory playerInv) {
        super(LIMenus.EXTENDED.get(), id);
        this.owner = playerInv.player;
        this.backing = new CombinedInventoryHandler(playerInv, LICaps.get(owner));
        this.context = new ScrollContext(this, owner, this.backing);

        boolean creative = owner.isCreative();

        addSlot(new ResultSlot(owner, craftSlots, resultSlots, 0,
            creative ? HIDDEN : 154, creative ? HIDDEN : 28));

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new Slot(craftSlots, col + row * 2,
                    creative ? HIDDEN : 98 + col * 18,
                    creative ? HIDDEN : 18 + row * 18));
            }
        }

        for (int i = 0; i < 4; i++) {
            final EquipmentSlot equipment = ARMOR_ORDER[i];
            final ResourceLocation icon = ARMOR_ICONS[i];
            final int armorX = creative ? CREATIVE_ARMOR[i][0] : 8;
            final int armorY = creative ? CREATIVE_ARMOR[i][1] : 8 + i * 18;
            addSlot(new Slot(playerInv, 39 - i, armorX, armorY) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return equipment == Mob.getEquipmentSlotForItem(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    ItemStack stack = getItem();
                    return (stack.isEmpty() || player.isCreative() || !EnchantmentHelper.hasBindingCurse(stack))
                            && super.mayPickup(player);
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
                }
            });
        }

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new LISlot(context, row * COLS + col,
                    (creative ? CREATIVE_MAIN_X : MAIN_X) + col * 18,
                    (creative ? CREATIVE_MAIN_Y : MAIN_Y) + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col,
                (creative ? CREATIVE_MAIN_X : MAIN_X) + col * 18,
                creative ? CREATIVE_HOTBAR_Y : HOTBAR_Y));
        }

        addSlot(new Slot(playerInv, 40, creative ? 35 : 77, creative ? 20 : 62) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        if (creative) {
            addSlot(new TrashSlot(TRASH_X, TRASH_Y));
        }
    }

    public boolean hasTrash() {
        return slots.size() > TRASH && slots.get(TRASH) instanceof TrashSlot;
    }

    public ScrollContext context() {
        return context;
    }

    public CombinedInventoryHandler getBacking() {
        return context.getBacking();
    }

    public int getScrollRow() {
        return context.getScrollRow();
    }

    @Nullable
    public int[] getFilter() {
        return context.getFilter();
    }

    public void setFilter(@Nullable int[] indices) {
        context.setFilter(indices);
    }

    public int visibleCount() {
        return context.visibleCount();
    }

    public int totalRows() {
        return context.totalRows();
    }

    public int maxScrollRow() {
        return context.maxScrollRow();
    }

    public int physicalMaxScrollRow() {
        return context.physicalMaxScrollRow();
    }

    public void setScrollRow(int row) {
        context.setScrollRow(row);
    }

    public void onSlotContentsChanged() {
        broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots) {
            updateCraftingResult();
        }
    }

    private void updateCraftingResult() {
        Level level = owner.level();
        if (level.isClientSide() || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack result = ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftSlots, level);
        if (recipe.isPresent()) {
            CraftingRecipe crafting = recipe.get();
            if (resultSlots.setRecipeUsed(level, serverPlayer, crafting)) {
                ItemStack assembled = crafting.assemble(craftSlots, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(RESULT, result);
        serverPlayer.connection.send(
                new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        resultSlots.clearContent();
        if (player.level().isClientSide()) {
            return;
        }
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.removeItemNoUpdate(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(backing, stack, false);
            if (remainder.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents contents) {
        craftSlots.fillStackedContents(contents);
    }

    @Override
    public void clearCraftingContent() {
        resultSlots.clearContent();
        craftSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(Recipe<? super CraftingContainer> recipe) {
        return recipe.matches(craftSlots, owner.level());
    }

    @Override
    public int getResultSlotIndex() {
        return RESULT;
    }

    @Override
    public int getGridWidth() {
        return craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 5;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != getResultSlotIndex();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId == TRASH && type == ClickType.QUICK_MOVE && hasTrash()) {
            if (!player.level().isClientSide()) {
                destroyEverything(player);
            }
            return;
        }

        super.clicked(slotId, button, type, player);
        if (type != ClickType.PICKUP_ALL || slotId < 0) {
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty() || carried.getCount() >= carried.getMaxStackSize()) {
            return;
        }
        collectFromHidden(carried);
        broadcastChanges();
    }

    private void collectFromHidden(ItemStack carried) {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < backing.getSlots(); i++) {
                if (carried.getCount() >= carried.getMaxStackSize()) {
                    return;
                }
                if (isVisibleBackingIndex(i)) {
                    continue;
                }
                ItemStack inSlot = backing.getStackInSlot(i);
                if (inSlot.isEmpty() || !ItemStack.isSameItemSameTags(inSlot, carried)) {
                    continue;
                }
                if (pass == 0 && inSlot.getCount() >= inSlot.getMaxStackSize()) {
                    continue;
                }
                int want = Math.min(carried.getMaxStackSize() - carried.getCount(), inSlot.getCount());
                ItemStack taken = backing.extractItem(i, want, false);
                if (!taken.isEmpty()) {
                    carried.grow(taken.getCount());
                }
            }
        }
    }

    private void destroyEverything(Player player) {
        player.getInventory().clearContent();
        craftSlots.clearContent();
        resultSlots.clearContent();
        setCarried(ItemStack.EMPTY);

        ExtendedInventory extended = LICaps.get(player);
        extended.clearAll();
        context.setFilter(null);
        context.setScrollRow(0);

        broadcastFullState();
        if (player instanceof ServerPlayer serverPlayer) {
            extended.clearChanges();
            LINet.toPlayer(serverPlayer, MirrorPacket.full(extended));
        }
    }

    private boolean isVisibleBackingIndex(int backingIndex) {
        int first = context.getScrollRow() * COLS;
        int last = first + WINDOW;
        int[] filter = context.getFilter();
        if (filter == null) {
            return backingIndex >= first && backingIndex < last;
        }
        for (int pos = first; pos < last && pos < filter.length; pos++) {
            if (filter[pos] == backingIndex) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();

        if (index == RESULT) {
            if (!pushToBacking(inSlot) && !moveItemStackTo(inSlot, HOTBAR_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.set(inSlot);
            slot.onQuickCraft(inSlot, original);
        } else if (index >= MAIN_START && index < HOTBAR_START) {
            if (!tryEquip(inSlot) && !moveItemStackTo(inSlot, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
            slot.set(inSlot);
        } else {
            if (index >= HOTBAR_START && tryEquip(inSlot)) {
                slot.set(inSlot);
            } else if (pushToBacking(inSlot)) {
                slot.set(inSlot);
            } else if (index < HOTBAR_START && moveItemStackTo(inSlot, HOTBAR_START, HOTBAR_END, false)) {
                slot.set(inSlot);
            } else {
                return ItemStack.EMPTY;
            }
        }

        slot.setChanged();
        slot.onTake(player, original);
        return original;
    }

    private boolean pushToBacking(ItemStack stack) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(backing, stack.copy(), false);
        if (remainder.getCount() == stack.getCount()) {
            return false;
        }
        stack.setCount(remainder.getCount());
        return true;
    }

    private boolean tryEquip(ItemStack stack) {
        EquipmentSlot target = Mob.getEquipmentSlotForItem(stack);
        for (int i = 0; i < ARMOR_ORDER.length; i++) {
            if (ARMOR_ORDER[i] != target) {
                continue;
            }
            Slot armor = slots.get(ARMOR_START + i);
            if (!armor.hasItem() && armor.mayPlace(stack)) {
                armor.set(stack.split(1));
                return true;
            }
        }
        return false;
    }
}
