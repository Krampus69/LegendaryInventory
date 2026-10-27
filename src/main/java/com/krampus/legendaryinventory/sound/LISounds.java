package com.krampus.legendaryinventory.sound;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LISounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LegendaryInventory.MODID);

    public static final RegistryObject<SoundEvent> INVENTORY_EXPAND = SOUNDS.register("inventory_expand",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(LegendaryInventory.MODID, "inventory_expand")));

    public static final RegistryObject<SoundEvent> INVENTORY_COLLAPSE = SOUNDS.register("inventory_collapse",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(LegendaryInventory.MODID, "inventory_collapse")));

    private LISounds() {}
}
