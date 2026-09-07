package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class WeightTags {

    public static final TagKey<Item> NO_WEIGHT_FLASH = TagKey.create(
        BuiltInRegistries.ITEM.key(),
        new ResourceLocation(LegendaryInventory.MODID, "no_weight_flash")
    );

    public static final TagKey<EntityType<?>> BLOCKED_MOUNTS = TagKey.create(
        BuiltInRegistries.ENTITY_TYPE.key(),
        new ResourceLocation(LegendaryInventory.MODID, "blocked_mounts")
    );

    public static final TagKey<EntityType<?>> FREE_MOUNTS = TagKey.create(
        BuiltInRegistries.ENTITY_TYPE.key(),
        new ResourceLocation(LegendaryInventory.MODID, "free_mounts")
    );

    private WeightTags() {}
}
