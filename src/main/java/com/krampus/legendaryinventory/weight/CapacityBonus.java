package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class CapacityBonus {

    private static final String KEY = LegendaryInventory.MODID + ":capacity_bonus";
    private static final UUID MODIFIER_ID = UUID.fromString("7c2d1c6e-4f7a-4c3b-9a0e-2b8f0d5e6a11");
    private static final String MODIFIER_NAME = LegendaryInventory.MODID + ":tablet_bonus";

    private CapacityBonus() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        int bonus = get(event.getOriginal());
        if (bonus != 0) {
            set(event.getEntity(), bonus);
        }
    }

    public static int base(Player player) {
        AttributeInstance attr = player.getAttribute(LIAttributes.CARRY_CAPACITY.get());
        return attr == null ? LIConfig.COMMON.baseCapacity.get() : (int) attr.getBaseValue();
    }

    public static void setTotal(Player player, int total) {
        set(player, total - base(player));
    }

    public static int get(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(KEY);
    }

    public static void set(Player player, int bonus) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(KEY, bonus);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
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
        AttributeInstance attr = player.getAttribute(LIAttributes.CARRY_CAPACITY.get());
        if (attr == null) {
            return;
        }
        int bonus = get(player);
        AttributeModifier existing = attr.getModifier(MODIFIER_ID);
        if (existing != null && existing.getAmount() == bonus) {
            return;
        }
        if (existing != null) {
            attr.removeModifier(MODIFIER_ID);
        }
        if (bonus != 0) {
            attr.addPermanentModifier(new AttributeModifier(
                MODIFIER_ID, MODIFIER_NAME, bonus, AttributeModifier.Operation.ADDITION));
        }
    }
}
