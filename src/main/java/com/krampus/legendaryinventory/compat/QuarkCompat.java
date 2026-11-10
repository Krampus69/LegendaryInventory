package com.krampus.legendaryinventory.compat;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.ExtendedAccess;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.InventorySorter;
import com.krampus.legendaryinventory.inventory.LICaps;
import com.krampus.legendaryinventory.menu.ScrollContext;
import com.krampus.legendaryinventory.menu.ScrollRegistry;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.MirrorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.function.BiPredicate;
import java.util.function.Function;

public final class QuarkCompat {

    private static final String BACKPACK_MENU = "org.violetmoon.quark.addons.oddities.inventory.BackpackMenu";

    private static volatile boolean transferActive = true;

    private QuarkCompat() {}

    public static boolean transferActive() {
        return transferActive;
    }

    public static void disableTransfer(LinkageError error) {
        if (transferActive) {
            transferActive = false;
            LegendaryInventory.LOGGER.warn("Quark transfer compat disabled, this Quark version is not supported", error);
        }
    }

    public static boolean sortPlayer(Player player, boolean forcePlayer) {
        if (!(player instanceof ServerPlayer serverPlayer) || !LIConfig.COMMON.sortEnabled.get()) {
            return false;
        }
        AbstractContainerMenu menu = serverPlayer.containerMenu;
        if (menu == null || BACKPACK_MENU.equals(menu.getClass().getName())) {
            return false;
        }
        if (!forcePlayer && menu != serverPlayer.inventoryMenu) {
            return false;
        }
        ScrollContext context = ScrollRegistry.get(menu);
        if (context == null) {
            return false;
        }
        InventorySorter.sort(context.getBacking(), false);
        serverPlayer.getInventory().setChanged();
        menu.broadcastFullState();
        ExtendedInventory extended = LICaps.get(serverPlayer);
        extended.clearChanges();
        LINet.toPlayer(serverPlayer, MirrorPacket.full(extended));
        return true;
    }

    public static void depositRows(Player player, Function<ItemStack, ItemStack> insert) {
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }
        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack stack = ext.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack rest = insert.apply(stack);
            if (!ItemStack.matches(stack, rest)) {
                ext.setStackInSlot(i, rest);
            }
        }
    }

    public static void restockRows(Player player, IItemHandler container, BiPredicate<ItemStack, IItemHandler> predicate) {
        ExtendedInventory ext = ExtendedAccess.of(player);
        if (ext == null) {
            return;
        }
        IItemHandler owned = new CombinedInvWrapper(new PlayerMainInvWrapper(player.getInventory()), ext);
        for (int i = container.getSlots() - 1; i >= 0; i--) {
            ItemStack available = container.extractItem(i, container.getStackInSlot(i).getCount(), true);
            if (available.isEmpty() || !ExtendedAccess.accepts(available) || !predicate.test(available, owned)) {
                continue;
            }
            int room = available.getCount() - ItemHandlerHelper.insertItemStacked(ext, available.copy(), true).getCount();
            if (room <= 0) {
                continue;
            }
            ItemStack taken = container.extractItem(i, room, false);
            ItemStack left = ItemHandlerHelper.insertItemStacked(ext, taken, false);
            if (!left.isEmpty()) {
                left = container.insertItem(i, left, false);
                if (!left.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(left);
                }
            }
        }
    }
}
