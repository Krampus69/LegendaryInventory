package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.inventory.LIAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public final class CapacityBonus {

    private static final ResourceLocation MODIFIER_ID =
        ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "tablet_bonus");

    private CapacityBonus() {}

    public static int base(Player player) {
        AttributeInstance attr = player.getAttribute(LIAttributes.CARRY_CAPACITY);
        return attr == null ? LIConfig.COMMON.baseCapacity.get() : (int) attr.getBaseValue();
    }

    public static void setTotal(Player player, int total) {
        set(player, total - base(player));
    }

    public static int get(Player player) {
        return LIAttachments.profile(player).bonus();
    }

    public static void set(Player player, int bonus) {
        LIAttachments.setProfile(player, LIAttachments.profile(player).withBonus(bonus));
        apply(player);
    }

    public static boolean add(Player player, int amount) {
        if (amount <= 0) {
            return false;
        }
        int current = CarryLoad.capacity(player);
        if (limited() && current + amount > max()) {
            return false;
        }
        set(player, get(player) + amount);
        return true;
    }

    public static boolean limited() {
        return LIConfig.COMMON.maxCapacityEnabled.get();
    }

    public static int max() {
        return LIConfig.COMMON.maxCapacity.get();
    }

    public static void apply(Player player) {
        AttributeInstance attr = player.getAttribute(LIAttributes.CARRY_CAPACITY);
        if (attr == null) {
            return;
        }
        int bonus = get(player);
        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        if (existing != null && existing.amount() == bonus) {
            return;
        }
        if (existing != null) {
            attr.removeModifier(MODIFIER_ID);
        }
        if (bonus != 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
