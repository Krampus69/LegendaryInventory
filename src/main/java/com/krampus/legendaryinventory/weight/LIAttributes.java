package com.krampus.legendaryinventory.weight;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LIAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, LegendaryInventory.MODID);

    public static final RegistryObject<Attribute> CARRY_CAPACITY = ATTRIBUTES.register(
        "carry_capacity",
        () -> new RangedAttribute("attribute.name.legendaryinventory.carry_capacity", 2000.0D, 0.0D, 1000000.0D)
            .setSyncable(true)
    );

    private LIAttributes() {}
}
