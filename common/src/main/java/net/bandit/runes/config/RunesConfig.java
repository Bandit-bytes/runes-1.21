package net.bandit.runes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RunesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config/runes_config.json";

    public static ConfigData configData = new ConfigData();

    public static void loadConfig() {
        File file = new File(CONFIG_FILE);

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                configData = GSON.fromJson(reader, ConfigData.class);

                if (configData == null) {
                    configData = new ConfigData();
                }
            } catch (IOException e) {
                e.printStackTrace();
                configData = new ConfigData();
            }
        } else {
            configData = new ConfigData();
        }
        saveConfig();
    }


    public static void saveConfig() {
        File file = new File(CONFIG_FILE);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(configData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ConfigData {
        public String defaultDimension = "minecraft:the_end";

        public boolean easyLootEnabled = true;
        public float easyLootDropChance = 0.55F;

        public boolean mediumLootEnabled = true;
        public float mediumLootDropChance = 0.45F;

        public boolean hardLootEnabled = true;
        public float hardLootDropChance = 0.28F;

        public boolean endgameLootEnabled = true;
        public float endgameLootDropChance = 0.18F;
    }

    public static ResourceKey<Level> getDefaultDimension() {
        if (configData.defaultDimension == null || configData.defaultDimension.isEmpty()) {
            return null;
        }

        return ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.parse(configData.defaultDimension)
        );
    }
}
