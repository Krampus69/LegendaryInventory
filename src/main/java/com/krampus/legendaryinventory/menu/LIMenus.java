package com.krampus.legendaryinventory.menu;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LIMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, LegendaryInventory.MODID);

    public static final RegistryObject<MenuType<LIMenu>> EXTENDED = MENUS.register(
        "extended",
        () -> IForgeMenuType.create((IContainerFactory<LIMenu>) (id, inv, data) -> new LIMenu(id, inv))
    );

    private LIMenus() {}
}
