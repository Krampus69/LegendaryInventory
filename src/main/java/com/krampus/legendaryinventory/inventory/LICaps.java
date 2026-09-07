package com.krampus.legendaryinventory.inventory;

import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LICaps {

    public static final Capability<ExtendedInventory> EXTENDED =
        CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation ID =
        new ResourceLocation(LegendaryInventory.MODID, "extended_inventory");

    private LICaps() {}

    public static ExtendedInventory get(Player player) {
        return player.getCapability(EXTENDED).orElseGet(() -> {
            LegendaryInventory.LOGGER.error(
                "Extended inventory capability missing on {}, returning a detached inventory",
                player.getName().getString());
            return new ExtendedInventory();
        });
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final ExtendedInventory inventory = new ExtendedInventory();
        private final LazyOptional<ExtendedInventory> holder = LazyOptional.of(() -> inventory);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return EXTENDED.orEmpty(cap, holder.cast());
        }

        @Override
        public CompoundTag serializeNBT() {
            return inventory.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            inventory.deserializeNBT(tag);
        }
    }

    @Mod.EventBusSubscriber(modid = LegendaryInventory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterCapabilitiesEvent event) {
            event.register(ExtendedInventory.class);
        }
    }

    @Mod.EventBusSubscriber(modid = LegendaryInventory.MODID)
    public static final class ForgeBus {

        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(ID, new Provider());
            }
        }

        @SubscribeEvent
        public static void clone(PlayerEvent.Clone event) {
            Player original = event.getOriginal();
            original.reviveCaps();
            original.getCapability(EXTENDED).ifPresent(from ->
                event.getEntity().getCapability(EXTENDED).ifPresent(to ->
                    to.deserializeNBT(from.serializeNBT())));
            original.invalidateCaps();
        }
    }
}
