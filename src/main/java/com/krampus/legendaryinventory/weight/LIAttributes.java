package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LIAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(Registries.ATTRIBUTE, LegendaryInventory.MODID);

    public static final DeferredHolder<Attribute, Attribute> CARRY_CAPACITY = ATTRIBUTES.register(
        "carry_capacity",
        () -> new RangedAttribute("attribute.name.legendaryinventory.carry_capacity", 1000.0D, 0.0D, 1000000.0D)
            .setSyncable(true)
    );

    private LIAttributes() {}
}
