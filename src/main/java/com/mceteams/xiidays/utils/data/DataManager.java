package com.mceteams.xiidays.utils.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mceteams.xiidays.XIIDays;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, JsonObject> cache = new HashMap<>();
    private static Path dataDir = null;

    private static Path getDataDir() {
        if (dataDir == null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                throw new RuntimeException("Server not available for data access");
            }
            dataDir = server.getWorldPath(LevelResource.ROOT).resolve("xiidaysdata");
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                XIIDays.LOGGER.error("Failed to create data directory: {}", e.getMessage());
            }
        }
        return dataDir;
    }

    public static JsonObject load(String domain) {
        if (cache.containsKey(domain)) {
            return cache.get(domain);
        }
        Path file = getDataDir().resolve(domain + ".json");
        JsonObject obj;
        if (Files.exists(file)) {
            try (FileReader reader = new FileReader(file.toFile())) {
                obj = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                XIIDays.LOGGER.error("Failed to load {}: {}", domain, e.getMessage());
                obj = new JsonObject();
            }
        } else {
            obj = new JsonObject();
        }
        cache.put(domain, obj);
        return obj;
    }

    public static void save(String domain) {
        JsonObject obj = cache.get(domain);
        if (obj == null) return;
        Path file = getDataDir().resolve(domain + ".json");
        try (FileWriter writer = new FileWriter(file.toFile())) {
            GSON.toJson(obj, writer);
        } catch (IOException e) {
            XIIDays.LOGGER.error("Failed to save {}: {}", domain, e.getMessage());
        }
    }

    public static void saveAll() {
        for (String domain : cache.keySet()) {
            save(domain);
        }
    }

    public static void reloadAll() {
        cache.clear();
        XIIDays.LOGGER.info("All data caches cleared, will reload on next access");
    }

    public static void reload(String domain) {
        cache.remove(domain);
        XIIDays.LOGGER.debug("Data cache '{}' cleared", domain);
    }

    public static String[] getAllDomains() {
        return cache.keySet().toArray(new String[0]);
    }
}
