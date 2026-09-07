package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.client.ClientMirror;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.BitSet;
import java.util.function.Supplier;

public class MirrorPacket {

    private final boolean full;
    private final int[] indices;
    private final ItemStack[] stacks;

    private MirrorPacket(boolean full, int[] indices, ItemStack[] stacks) {
        this.full = full;
        this.indices = indices;
        this.stacks = stacks;
    }

    public static MirrorPacket full(ExtendedInventory handler) {
        int size = handler.getSlots();
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            stacks[i] = handler.getStackInSlot(i);
        }
        return new MirrorPacket(true, null, stacks);
    }

    public static MirrorPacket delta(ExtendedInventory handler, BitSet changed) {
        int count = changed.cardinality();
        int[] indices = new int[count];
        ItemStack[] stacks = new ItemStack[count];

        int at = 0;
        for (int i = changed.nextSetBit(0); i >= 0; i = changed.nextSetBit(i + 1)) {
            indices[at] = i;
            stacks[at] = handler.getStackInSlot(i);
            at++;
        }
        return new MirrorPacket(false, indices, stacks);
    }

    public MirrorPacket(FriendlyByteBuf buf) {
        this.full = buf.readBoolean();
        int count = Math.min(buf.readVarInt(), ExtendedInventory.SIZE);
        this.indices = full ? null : new int[count];
        this.stacks = new ItemStack[count];

        for (int i = 0; i < count; i++) {
            if (!full) {
                indices[i] = buf.readVarInt();
            }
            stacks[i] = buf.readItem();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(full);
        buf.writeVarInt(stacks.length);
        for (int i = 0; i < stacks.length; i++) {
            if (!full) {
                buf.writeVarInt(indices[i]);
            }
            buf.writeItem(stacks[i]);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientMirror.accept(full, indices, stacks)));
        ctx.get().setPacketHandled(true);
    }
}
