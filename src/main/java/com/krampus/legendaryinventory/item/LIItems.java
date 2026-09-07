package com.krampus.legendaryinventory.item;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LIItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, LegendaryInventory.MODID);

    public static final RegistryObject<Item> SACK = ITEMS.register(
        "sack",
        () -> new SackItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant())
    );

    public static final RegistryObject<Item> WEIGHT_TABLET_1 = ITEMS.register(
        "weight_tablet_1",
        () -> new WeightTabletItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),
            () -> LIConfig.COMMON.tabletTier1.get())
    );

    public static final RegistryObject<Item> WEIGHT_TABLET_2 = ITEMS.register(
        "weight_tablet_2",
        () -> new WeightTabletItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
            () -> LIConfig.COMMON.tabletTier2.get())
    );

    public static final RegistryObject<Item> WEIGHT_TABLET_3 = ITEMS.register(
        "weight_tablet_3",
        () -> new WeightTabletItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC),
            () -> LIConfig.COMMON.tabletTier3.get())
    );

    private LIItems() {}
}
