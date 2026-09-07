package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.event.WeightEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class FovEvents {

    private FovEvents() {}

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }

        AttributeModifier mod = speed.getModifier(WeightEvents.OVERLOAD_ID);
        if (mod == null || mod.amount() == 0.0D) {
            return;
        }

        double walk = player.getAbilities().getWalkingSpeed();
        if (walk <= 0.0D) {
            return;
        }

        double denominator = 1.0D + mod.amount();
        double withMod = speed.getValue();
        double withoutMod = denominator > 1.0E-4D ? withMod / denominator : walk;

        double factorWith = (withMod / walk + 1.0D) / 2.0D;
        double factorWithout = (withoutMod / walk + 1.0D) / 2.0D;
        if (factorWith <= 0.0D) {
            return;
        }

        event.setNewFovModifier((float) (event.getNewFovModifier() * factorWithout / factorWith));
    }
}
