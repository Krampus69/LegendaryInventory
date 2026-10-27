package com.krampus.legendaryinventory.sound;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LISounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, LegendaryInventory.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> INVENTORY_EXPAND = SOUNDS.register("inventory_expand",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "inventory_expand")));

    public static final DeferredHolder<SoundEvent, SoundEvent> INVENTORY_COLLAPSE = SOUNDS.register("inventory_collapse",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(LegendaryInventory.MODID, "inventory_collapse")));

    private LISounds() {}
}
