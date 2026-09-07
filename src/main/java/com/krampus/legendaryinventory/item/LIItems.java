package com.krampus.legendaryinventory.item;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public final class LIItems {

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(LegendaryInventory.MODID);

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LegendaryInventory.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SACK_ID =
        COMPONENTS.registerComponentType("sack_id",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SACK_COUNT =
        COMPONENTS.registerComponentType("sack_count",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SACK_OWNER =
        COMPONENTS.registerComponentType("sack_owner",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredItem<Item> SACK = ITEMS.register(
        "sack",
        () -> new SackItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant())
    );

    public static final DeferredItem<Item> WEIGHT_TABLET_1 = ITEMS.register(
        "weight_tablet_1",
        () -> new WeightTabletItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),
            () -> LIConfig.COMMON.tabletTier1.get())
    );

    public static final DeferredItem<Item> WEIGHT_TABLET_2 = ITEMS.register(
        "weight_tablet_2",
        () -> new WeightTabletItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
            () -> LIConfig.COMMON.tabletTier2.get())
    );

    public static final DeferredItem<Item> WEIGHT_TABLET_3 = ITEMS.register(
        "weight_tablet_3",
        () -> new WeightTabletItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC),
            () -> LIConfig.COMMON.tabletTier3.get())
    );

    private LIItems() {}
}
