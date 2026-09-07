package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LIMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, LegendaryInventory.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<LIMenu>> EXTENDED = MENUS.register(
        "extended",
        () -> IMenuTypeExtension.create((id, inv, data) -> new LIMenu(id, inv))
    );

    private LIMenus() {}
}
