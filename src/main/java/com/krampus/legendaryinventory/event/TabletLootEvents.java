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
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
public final class TabletLootEvents {

    private static final String POOL_NAME = LegendaryInventory.MODID + ":weight_tablets";

    private static final Set<ResourceLocation> TIER_1 = Set.of(
        new ResourceLocation("chests/simple_dungeon"),
        new ResourceLocation("chests/abandoned_mineshaft"),
        new ResourceLocation("chests/desert_pyramid"),
        new ResourceLocation("chests/jungle_temple"),
        new ResourceLocation("chests/igloo_chest"),
        new ResourceLocation("chests/pillager_outpost"),
        new ResourceLocation("chests/shipwreck_treasure"),
        new ResourceLocation("chests/nether_bridge")
    );

    private static final Set<ResourceLocation> TIER_2 = Set.of(
        new ResourceLocation("chests/stronghold_corridor"),
        new ResourceLocation("chests/stronghold_crossing"),
        new ResourceLocation("chests/stronghold_library"),
        new ResourceLocation("chests/woodland_mansion"),
        new ResourceLocation("chests/bastion_other"),
        new ResourceLocation("chests/bastion_bridge"),
        new ResourceLocation("chests/buried_treasure")
    );

    private static final Set<ResourceLocation> TIER_3 = Set.of(
        new ResourceLocation("chests/bastion_treasure"),
        new ResourceLocation("chests/end_city_treasure"),
        new ResourceLocation("chests/ancient_city")
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
