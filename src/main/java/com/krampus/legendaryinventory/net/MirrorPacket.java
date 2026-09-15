package com.krampus.legendaryinventory.net;

import com.krampus.legendaryinventory.inventory.CombinedInventoryHandler;
import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.BitSet;

public record MirrorPacket(boolean full, int[] indices, ItemStack[] stacks) implements CustomPacketPayload {

    public static final Type<MirrorPacket> TYPE = new Type<>(LINet.id("mirror"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MirrorPacket> STREAM_CODEC =
        StreamCodec.of(MirrorPacket::write, MirrorPacket::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static MirrorPacket full(ExtendedInventory handler) {
        int size = handler.getSlots();
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            stacks[i] = handler.getStackInSlot(i);
        }
        return new MirrorPacket(true, null, stacks);
    }

    public static MirrorPacket mainDelta(Inventory inventory, BitSet changed) {
        int count = changed.cardinality();
        int[] indices = new int[count];
        ItemStack[] stacks = new ItemStack[count];
        int at = 0;
        for (int i = changed.nextSetBit(0); i >= 0; i = changed.nextSetBit(i + 1)) {
            indices[at] = ExtendedInventory.SIZE + i;
            stacks[at] = inventory.getItem(CombinedInventoryHandler.MAIN_OFFSET + i);
            at++;
        }
        return new MirrorPacket(false, indices, stacks);
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

    private static MirrorPacket read(RegistryFriendlyByteBuf buf) {
        boolean full = buf.readBoolean();
        int count = Math.min(buf.readVarInt(), ExtendedInventory.SIZE + CombinedInventoryHandler.MAIN_COUNT);
        int[] indices = full ? null : new int[count];
        ItemStack[] stacks = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            if (!full) {
                indices[i] = buf.readVarInt();
            }
            stacks[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        }
        return new MirrorPacket(full, indices, stacks);
    }

    private static void write(RegistryFriendlyByteBuf buf, MirrorPacket packet) {
        buf.writeBoolean(packet.full);
        buf.writeVarInt(packet.stacks.length);
        for (int i = 0; i < packet.stacks.length; i++) {
            if (!packet.full) {
                buf.writeVarInt(packet.indices[i]);
            }
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.stacks[i]);
        }
    }
}
