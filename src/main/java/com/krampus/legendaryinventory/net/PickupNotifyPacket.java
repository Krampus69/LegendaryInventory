package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.compat.LootJournalCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PickupNotifyPacket {

    private final ItemStack stack;

    public PickupNotifyPacket(ItemStack stack) {
        this.stack = stack;
    }

    public PickupNotifyPacket(FriendlyByteBuf buf) {
        this.stack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(stack);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> LootJournalCompat.notifyPickup(stack)));
        ctx.get().setPacketHandled(true);
    }
}
