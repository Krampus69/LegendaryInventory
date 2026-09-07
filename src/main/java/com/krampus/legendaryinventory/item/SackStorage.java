package com.krampus.legendaryinventory.item;

import com.krampus.legendaryinventory.inventory.ExtendedInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SackStorage extends SavedData {

    private static final String NAME = "legendaryinventory_sacks";
    private static final String SACKS = "Sacks";
    private static final String ID = "Id";
    private static final String PACK = "Pack";

    private final Map<UUID, CompoundTag> packs = new HashMap<>();

    public static SackStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage()
            .computeIfAbsent(SackStorage::load, SackStorage::new, NAME);
    }

    public static SackStorage load(CompoundTag tag) {
        SackStorage data = new SackStorage();
        ListTag list = tag.getList(SACKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.packs.put(entry.getUUID(ID), entry.getCompound(PACK));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : packs.entrySet()) {
            CompoundTag stored = new CompoundTag();
            stored.putUUID(ID, entry.getKey());
            stored.put(PACK, entry.getValue());
            list.add(stored);
        }
        tag.put(SACKS, list);
        return tag;
    }

    public UUID store(ItemStackHandler handler) {
        UUID id = UUID.randomUUID();
        packs.put(id, handler.serializeNBT());
        setDirty();
        return id;
    }

    public ItemStackHandler read(UUID id) {
        ItemStackHandler handler = new ItemStackHandler(ExtendedInventory.SIZE);
        CompoundTag pack = packs.get(id);
        if (pack != null) {
            CompoundTag copy = pack.copy();
            copy.putInt("Size", ExtendedInventory.SIZE);
            handler.deserializeNBT(copy);
        }
        return handler;
    }

    public boolean has(UUID id) {
        return packs.containsKey(id);
    }

    public void write(UUID id, ItemStackHandler handler) {
        packs.put(id, handler.serializeNBT());
        setDirty();
    }

    public void remove(UUID id) {
        if (packs.remove(id) != null) {
            setDirty();
        }
    }

    public int size() {
        return packs.size();
    }
}
