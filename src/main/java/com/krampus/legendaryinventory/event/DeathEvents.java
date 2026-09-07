package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import com.krampus.legendaryinventory.item.SackItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class DeathEvents {

    private static final int PICKUP_DELAY = 40;

    private DeathEvents() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        ExtendedInventory ext = LIAttachments.extended(player);
        ItemStackHandler contents = new ItemStackHandler(ExtendedInventory.SIZE);
        int packed = 0;

        for (int i = 0; i < ext.getSlots(); i++) {
            ItemStack stack = ext.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            ext.setStackInSlot(i, ItemStack.EMPTY);
            if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                continue;
            }
            contents.setStackInSlot(packed, stack);
            packed++;
        }

        if (packed == 0) {
            return;
        }

        ItemStack sack = SackItem.of(level, contents, player.getGameProfile().getName());
        event.getDrops().add(toDrop(player, sack));
    }

    private static ItemEntity toDrop(Player player, ItemStack stack) {
        ItemEntity entity = new ItemEntity(
            player.level(), player.getX(), player.getEyeY() - 0.3D, player.getZ(), stack);
        entity.setPickUpDelay(PICKUP_DELAY);

        float speed = player.getRandom().nextFloat() * 0.2F;
        float angle = player.getRandom().nextFloat() * ((float) Math.PI * 2.0F);
        entity.setDeltaMovement(
            -Mth.sin(angle) * speed, 0.2D, Mth.cos(angle) * speed);

        return entity;
    }
}
