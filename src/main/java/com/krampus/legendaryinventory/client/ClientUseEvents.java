package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.weight.WeightTags;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class ClientUseEvents {

    private static final long SUPPRESS_MILLIS = 1000L;

    private ClientUseEvents() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getItemStack().is(WeightTags.NO_WEIGHT_FLASH)) {
            ClientWeightState.suppress(SUPPRESS_MILLIS);
        }
    }
}
