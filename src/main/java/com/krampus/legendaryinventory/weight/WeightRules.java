package com.krampus.legendaryinventory.weight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.krampus.legendaryinventory.LegendaryInventory;
import com.krampus.legendaryinventory.config.LIConfig;
import com.krampus.legendaryinventory.net.LINet;
import com.krampus.legendaryinventory.net.RulesSyncPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class WeightRules {

    private static final String WEIGHTS_FILE = "weights.json";
    private static final String DEFAULTS_RESOURCE = "/legendaryinventory_defaults/weights.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Values(
        int stackable,
        int semiStackable,
        int unstackable,
        double slowStart,
        double heavy,
        double sprintBlock,
        double stall,
        Map<ResourceLocation, Integer> overrides
    ) {
        public double[] thresholds() {
            return new double[] {slowStart, heavy, sprintBlock, stall};
        }
    }

    private static Values active = new Values(1, 4, 24, 1.0D, 1.2D, 1.5D, 2.0D, Collections.emptyMap());

    private WeightRules() {}

    public static Values current() {
        return active;
    }

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(LIConfig.FOLDER);
    }

    public static void loadLocal() {
        LIConfig.Common c = LIConfig.COMMON;
        active = new Values(
            c.fallbackStackable.get(),
            c.fallbackSemiStackable.get(),
            c.fallbackUnstackable.get(),
            c.slowStart.get(),
            c.heavy.get(),
            c.sprintBlock.get(),
            c.stall.get(),
            readOverrides()
        );
        LegendaryInventory.LOGGER.info("Loaded {} item weight overrides", active.overrides().size());
    }

    public static void apply(Values synced) {
        active = synced;
        if (BuiltInRegistries.ITEM.size() > 0) {
            WeightTable.rebuild();
        }
    }

    public static void reloadAndSync() {
        loadLocal();
        WeightTable.rebuild();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        RulesSyncPacket packet = new RulesSyncPacket(active);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            LINet.toPlayer(player, packet);
        }
    }

    public static void sendTo(ServerPlayer player) {
        LINet.toPlayer(player, new RulesSyncPacket(active));
    }

    public static boolean setOverride(ResourceLocation id, int weight) {
        Path file = folder().resolve(WEIGHTS_FILE);
        try {
            Files.createDirectories(folder());
            if (!Files.exists(file)) {
                writeDefaults(file);
            }
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                root = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
            root.addProperty(id.toString(), Math.max(0, weight));
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LegendaryInventory.LOGGER.error("Could not write {}", file, e);
            return false;
        }
        reloadAndSync();
        return true;
    }

    private static Map<ResourceLocation, Integer> readOverrides() {
        Path file = folder().resolve(WEIGHTS_FILE);
        try {
            Files.createDirectories(folder());
            if (!Files.exists(file)) {
                writeDefaults(file);
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader));
            }
        } catch (IOException | RuntimeException e) {
            LegendaryInventory.LOGGER.error("Could not read {}", file, e);
            return Collections.emptyMap();
        }
    }

    private static void writeDefaults(Path file) throws IOException {
        try (InputStream in = WeightRules.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (in == null) {
                Files.writeString(file, GSON.toJson(new JsonObject()), StandardCharsets.UTF_8);
                return;
            }
            Files.copy(in, file);
        }
        LegendaryInventory.LOGGER.info("Wrote default {}", file);
    }

    private static Map<ResourceLocation, Integer> parse(JsonElement root) {
        Map<ResourceLocation, Integer> out = new HashMap<>();
        if (!root.isJsonObject()) {
            return out;
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                LegendaryInventory.LOGGER.warn("Skipping weight entry {}", entry.getKey());
                continue;
            }
            out.put(id, Math.max(0, entry.getValue().getAsInt()));
        }
        return out;
    }
}
