package com.krampus.legendaryinventory.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

public class SackItem extends Item {

    public SackItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ServerLevel level, ItemStackHandler contents, String owner) {
        UUID id = SackStorage.get(level.getServer()).store(contents);

        ItemStack sack = new ItemStack(LIItems.SACK.get());
        sack.set(LIItems.SACK_ID.get(), id);
        sack.set(LIItems.SACK_COUNT.get(), used(contents));
        sack.set(LIItems.SACK_OWNER.get(), owner);
        return sack;
    }

    public static UUID idOf(ItemStack sack) {
        return sack.get(LIItems.SACK_ID.get());
    }

    public static int used(ItemStackHandler handler) {
        int used = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    public static int countOf(ItemStack sack) {
        return sack.getOrDefault(LIItems.SACK_COUNT.get(), 0);
    }

    public static void setCount(ItemStack sack, int count) {
        sack.set(LIItems.SACK_COUNT.get(), count);
    }

    public static boolean isEmpty(ItemStack sack) {
        return countOf(sack) <= 0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String owner = stack.get(LIItems.SACK_OWNER.get());
        if (owner != null) {
            tooltip.add(Component.translatable("item.legendaryinventory.sack.owner", owner)
                .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.legendaryinventory.sack.count", countOf(stack))
            .withStyle(ChatFormatting.GRAY));

        if (flag.isAdvanced()) {
            UUID id = idOf(stack);
            if (id != null) {
                tooltip.add(Component.literal(id.toString().substring(0, 8))
                    .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
