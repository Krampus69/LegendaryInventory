package com.krampus.legendaryinventory.item;

import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.CapacityBonus;
import com.krampus.legendaryinventory.weight.CarryLoad;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class WeightTabletItem extends Item {

    private final Supplier<Integer> pods;

    public WeightTabletItem(Properties properties, Supplier<Integer> pods) {
        super(properties);
        this.pods = pods;
    }

    public static boolean enabled() {
        return LIConfig.COMMON.tabletsEnabled.get();
    }

    public int pods() {
        return pods.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!enabled()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        int amount = pods();
        if (!CapacityBonus.add(player, amount)) {
            player.displayClientMessage(Component.translatable(
                "gui.legendaryinventory.tablet.max", CapacityBonus.max()), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        int capacity = CarryLoad.capacity(player);
        Component message = CapacityBonus.limited()
            ? Component.translatable("gui.legendaryinventory.tablet.used.limited", amount, capacity, CapacityBonus.max())
            : Component.translatable("gui.legendaryinventory.tablet.used", amount, capacity);
        player.displayClientMessage(message, true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.legendaryinventory.weight_tablet.tooltip", pods())
            .withStyle(ChatFormatting.GRAY));
        if (!enabled()) {
            tooltip.add(Component.translatable("item.legendaryinventory.weight_tablet.disabled")
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
