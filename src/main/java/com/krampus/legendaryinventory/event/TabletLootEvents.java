package com.krampus.legendaryinventory.event;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.item.LIItems;
import com.krampus.legendaryinventory.item.WeightTabletItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class TabletLootEvents {

    private static final String POOL_NAME = LegendaryInventory.MODID + ":weight_tablets";

    private static final Set<ResourceLocation> TIER_1 = Set.of(
        ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
        ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft"),
        ResourceLocation.withDefaultNamespace("chests/desert_pyramid"),
        ResourceLocation.withDefaultNamespace("chests/jungle_temple"),
        ResourceLocation.withDefaultNamespace("chests/igloo_chest"),
        ResourceLocation.withDefaultNamespace("chests/pillager_outpost"),
        ResourceLocation.withDefaultNamespace("chests/shipwreck_treasure"),
        ResourceLocation.withDefaultNamespace("chests/nether_bridge")
    );

    private static final Set<ResourceLocation> TIER_2 = Set.of(
        ResourceLocation.withDefaultNamespace("chests/stronghold_corridor"),
        ResourceLocation.withDefaultNamespace("chests/stronghold_crossing"),
        ResourceLocation.withDefaultNamespace("chests/stronghold_library"),
        ResourceLocation.withDefaultNamespace("chests/woodland_mansion"),
        ResourceLocation.withDefaultNamespace("chests/bastion_other"),
        ResourceLocation.withDefaultNamespace("chests/bastion_bridge"),
        ResourceLocation.withDefaultNamespace("chests/buried_treasure")
    );

    private static final Set<ResourceLocation> TIER_3 = Set.of(
        ResourceLocation.withDefaultNamespace("chests/bastion_treasure"),
        ResourceLocation.withDefaultNamespace("chests/end_city_treasure"),
        ResourceLocation.withDefaultNamespace("chests/ancient_city")
    );

    private static final Map<Integer, int[]> CHANCES = Map.of(
        1, new int[] {1, 9},
        2, new int[] {1, 9},
        3, new int[] {1, 9}
    );

    private TabletLootEvents() {}

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!WeightTabletItem.enabled()) {
            return;
        }
        int tier = tierFor(event.getName());
        if (tier == 0) {
            return;
        }
        Item item = switch (tier) {
            case 1 -> LIItems.WEIGHT_TABLET_1.get();
            case 2 -> LIItems.WEIGHT_TABLET_2.get();
            default -> LIItems.WEIGHT_TABLET_3.get();
        };
        int[] weights = CHANCES.get(tier);
        LootPool pool = LootPool.lootPool()
            .name(POOL_NAME)
            .setRolls(ConstantValue.exactly(1.0F))
            .add(LootItem.lootTableItem(item).setWeight(weights[0]))
            .add(EmptyLootItem.emptyItem().setWeight(weights[1]))
            .build();
        event.getTable().addPool(pool);
    }

    private static int tierFor(ResourceLocation name) {
        if (TIER_3.contains(name)) {
            return 3;
        }
        if (TIER_2.contains(name)) {
            return 2;
        }
        if (TIER_1.contains(name)) {
            return 1;
        }
        return 0;
    }
}
