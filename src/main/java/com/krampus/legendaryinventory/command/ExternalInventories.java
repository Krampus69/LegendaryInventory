package com.krampus.legendaryinventory.command;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.lang.reflect.Method;

public final class ExternalInventories {

    private static final String CURIOS = "curios";
    private static final String COSMETIC_ARMOR = "cosmeticarmorreworked";

    private ExternalInventories() {}

    public static int clearAll(ServerPlayer player) {
        int cleared = 0;
        if (ModList.get().isLoaded(CURIOS)) {
            cleared += clearCurios(player);
        }
        if (ModList.get().isLoaded(COSMETIC_ARMOR)) {
            cleared += clearCosmeticArmor(player);
        }
        return cleared;
    }

    @SuppressWarnings("unchecked")
    private static int clearCurios(ServerPlayer player) {
        try {
            Class<?> caps = Class.forName("top.theillusivec4.curios.api.CuriosCapability");
            Capability<IItemHandlerModifiable> cap =
                (Capability<IItemHandlerModifiable>) caps.getField("ITEM_HANDLER").get(null);
            return player.getCapability(cap).map(ExternalInventories::clearHandler).orElse(0);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    private static int clearCosmeticArmor(ServerPlayer player) {
        try {
            Class<?> api = Class.forName("lain.mods.cos.api.CosArmorAPI");
            Method get = api.getMethod("getCAStacks", java.util.UUID.class);
            Object stacks = get.invoke(null, player.getUUID());
            if (stacks instanceof IItemHandler handler) {
                return clearHandler(handler);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
        return 0;
    }

    private static int clearHandler(IItemHandler handler) {
        int cleared = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                continue;
            }
            if (handler instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(i, ItemStack.EMPTY);
            } else {
                handler.extractItem(i, Integer.MAX_VALUE, false);
            }
            cleared++;
        }
        return cleared;
    }
}
