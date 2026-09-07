package com.krampus.legendaryinventory.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

public class SackItem extends Item {

    public static final String ID = "SackId";
    public static final String COUNT = "Count";
    public static final String OWNER = "Owner";

    public SackItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ServerLevel level, ItemStackHandler contents, String owner) {
        UUID id = SackStorage.get(level.getServer()).store(contents);

        ItemStack sack = new ItemStack(LIItems.SACK.get());
        CompoundTag tag = sack.getOrCreateTag();
        tag.putUUID(ID, id);
        tag.putInt(COUNT, used(contents));
        tag.putString(OWNER, owner);
        return sack;
    }

    public static UUID idOf(ItemStack sack) {
        CompoundTag tag = sack.getTag();
        if (tag == null || !tag.hasUUID(ID)) {
            return null;
        }
        return tag.getUUID(ID);
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
        CompoundTag tag = sack.getTag();
        return tag == null ? 0 : tag.getInt(COUNT);
    }

    public static void setCount(ItemStack sack, int count) {
        sack.getOrCreateTag().putInt(COUNT, count);
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
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(OWNER, Tag.TAG_STRING)) {
            tooltip.add(Component.translatable("item.legendaryinventory.sack.owner", tag.getString(OWNER))
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
