package com.krampus.legendaryinventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.krampus.legendaryinventory.LegendaryInventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SortButtonLayout {

    public static final class Entry {
        public Integer x;
        public Integer y;
        public boolean hidden;
    }

    private static final String FILE = "legendaryinventory-sortbutton.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Entry>>() {}.getType();

    private static Map<String, Entry> entries;

    private SortButtonLayout() {}

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static Map<String, Entry> entries() {
        if (entries == null) {
            entries = new LinkedHashMap<>();
            Path path = path();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    Map<String, Entry> loaded = GSON.fromJson(reader, MAP_TYPE);
                    if (loaded != null) {
                        entries.putAll(loaded);
                    }
                } catch (IOException | RuntimeException e) {
                    LegendaryInventory.LOGGER.warn("Could not read {}", FILE, e);
                }
            }
        }
        return entries;
    }

    private static void save() {
        try (Writer writer = Files.newBufferedWriter(path())) {
            GSON.toJson(entries(), MAP_TYPE, writer);
        } catch (IOException e) {
            LegendaryInventory.LOGGER.warn("Could not write {}", FILE, e);
        }
    }

    private static final String EXPANDED_SUFFIX = "#expanded";

    private static String key(AbstractContainerScreen<?> screen) {
        String key = screen.getClass().getName();
        return InventoryExpansion.isExpanded(screen) ? key + EXPANDED_SUFFIX : key;
    }

    public static Entry get(AbstractContainerScreen<?> screen) {
        return entries().get(key(screen));
    }

    public static boolean isHidden(AbstractContainerScreen<?> screen) {
        Entry entry = get(screen);
        return entry != null && entry.hidden;
    }

    public static void setPosition(AbstractContainerScreen<?> screen, int x, int y) {
        Entry entry = entries().computeIfAbsent(key(screen), k -> new Entry());
        entry.x = x;
        entry.y = y;
        save();
    }

    public static void setHidden(AbstractContainerScreen<?> screen, boolean hidden) {
        Entry entry = entries().computeIfAbsent(key(screen), k -> new Entry());
        entry.hidden = hidden;
        save();
    }

    public static void reset(AbstractContainerScreen<?> screen) {
        if (entries().remove(key(screen)) != null) {
            save();
        }
    }
}
