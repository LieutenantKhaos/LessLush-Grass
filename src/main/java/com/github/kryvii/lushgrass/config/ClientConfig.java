package com.github.kryvii.lushgrass.config;

import com.github.kryvii.lushgrass.LushGrass;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientConfig {
    private static final String FULL_COVERAGE_KEY = "full_grass_block_coverage";
    private static final String GRASS_DENSITY_KEY = "grass_density";
    private static final String TUFT_VARIATION_KEY = "tuft_variation";
    private static final String SUPPRESS_DEVELOPED_KEY = "suppress_tufts_in_developed_areas";
    private static final String DEVELOPED_RADIUS_KEY = "developed_area_radius";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("lush_grass-client.json");

    private static volatile Settings settings = Settings.defaults();

    public static synchronized void load() {
        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                throw new IOException("Config root must be a JSON object");
            }
            JsonObject object = element.getAsJsonObject();
            settings = new Settings(
                    readBoolean(object, FULL_COVERAGE_KEY, true),
                    readInt(object, GRASS_DENSITY_KEY, 2, 0, 4),
                    readInt(object, TUFT_VARIATION_KEY, 1, 0, 2),
                    readBoolean(object, SUPPRESS_DEVELOPED_KEY, true),
                    readInt(object, DEVELOPED_RADIUS_KEY, 12, 0, 16)
            );
        } catch (IOException | RuntimeException exception) {
            settings = Settings.defaults();
            LushGrass.LOGGER.warn("Could not load Lush Grass client config from {}", CONFIG_FILE, exception);
            save();
        }
    }

    public static synchronized void save() {
        Path temporaryFile = CONFIG_FILE.resolveSibling(CONFIG_FILE.getFileName() + ".tmp");
        JsonObject object = new JsonObject();
        object.addProperty(FULL_COVERAGE_KEY, settings.fullGrassBlockCoverage());
        object.addProperty(GRASS_DENSITY_KEY, settings.grassDensity());
        object.addProperty(TUFT_VARIATION_KEY, settings.tuftVariation());
        object.addProperty(SUPPRESS_DEVELOPED_KEY, settings.suppressTuftsInDevelopedAreas());
        object.addProperty(DEVELOPED_RADIUS_KEY, settings.developedAreaRadius());

        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
            try {
                Files.move(
                        temporaryFile,
                        CONFIG_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            LushGrass.LOGGER.warn("Could not save Lush Grass client config from {}", CONFIG_FILE, exception);
        }
    }

    public static boolean fullGrassBlockCoverage() {
        return settings.fullGrassBlockCoverage();
    }

    public static int grassDensity() {
        return settings.grassDensity();
    }

    public static int tuftVariation() {
        return settings.tuftVariation();
    }

    public static boolean suppressTuftsInDevelopedAreas() {
        return settings.suppressTuftsInDevelopedAreas();
    }

    public static int developedAreaRadius() {
        return settings.developedAreaRadius();
    }

    public static synchronized boolean update(
            boolean fullGrassBlockCoverage,
            int grassDensity,
            int tuftVariation,
            boolean suppressTuftsInDevelopedAreas,
            int developedAreaRadius
    ) {
        Settings updated = new Settings(
                fullGrassBlockCoverage,
                grassDensity,
                tuftVariation,
                suppressTuftsInDevelopedAreas,
                developedAreaRadius
        );
        if (updated.equals(settings)) {
            return false;
        }
        settings = updated;
        return true;
    }

    private static boolean readBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Config value must be a boolean: " + key);
        }
        return value.getAsBoolean();
    }

    private static int readInt(JsonObject object, String key, int defaultValue, int min, int max) {
        JsonElement value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Config value must be a number: " + key);
        }
        int result = value.getAsInt();
        if (result < min || result > max) {
            throw new IllegalArgumentException("Config value out of range: " + key);
        }
        return result;
    }

    private record Settings(
            boolean fullGrassBlockCoverage,
            int grassDensity,
            int tuftVariation,
            boolean suppressTuftsInDevelopedAreas,
            int developedAreaRadius
    ) {
        private static Settings defaults() {
            return new Settings(true, 2, 1, true, 12);
        }
    }

    private ClientConfig() {
    }
}
