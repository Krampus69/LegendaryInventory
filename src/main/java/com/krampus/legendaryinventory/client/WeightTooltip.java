package com.krampus.legendaryinventory.client;

import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.weight.WeightTable;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, value = Dist.CLIENT)
public final class WeightTooltip {

    private static final ResourceLocation ICON_FONT =
            new ResourceLocation(LegendaryInventory.MODID, "icons");
    private static final String ICON_CHAR = "\uE000";
    private static final int WEIGHT_COLOR = 0xAAAAAA;

    private WeightTooltip() {}

    @SubscribeEvent
    public static void onGatherComponents(RenderTooltipEvent.GatherComponents event) {
        if (!WeightTable.isReady() || !LIConfig.CLIENT.itemTooltipWeight.get()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty()) {
            return;
        }

        Optional<FormattedText> name = elements.get(0).left();
        if (name.isEmpty() || !(name.get() instanceof Component title)) {
            return;
        }

        MutableComponent weight = Component.literal(" " + WeightTable.of(stack))
                .withStyle(Style.EMPTY.withColor(WEIGHT_COLOR));
        MutableComponent icon = Component.literal(" " + ICON_CHAR)
                .withStyle(Style.EMPTY.withFont(ICON_FONT).withColor(0xFFFFFF));

        elements.set(0, Either.left(title.copy().append(weight).append(icon)));
    }
}
