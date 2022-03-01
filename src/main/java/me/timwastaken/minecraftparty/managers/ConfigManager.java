package me.timwastaken.minecraftparty.managers;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashMap;

public class ConfigManager {

    private static HashMap<String, FileConfiguration> configs;

    public static void init() throws IOException {
        configs = new HashMap<>();
        for (MinigameType type : MinigameType.values()) {
            FileConfiguration typeConfig = loadConfig(type.getAlias() + ".yml");
            configs.put(type.getAlias(), typeConfig);
            type.feedConfig(typeConfig);
        }
    }

    private static FileConfiguration loadConfig(String filename) throws IOException {
        File configFile = new File(MinecraftParty.getInstance().getDataFolder(), filename);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        InputStream stream = MinecraftParty.getInstance().getResource(filename);
        if (stream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
            config.setDefaults(defaultConfig);
            if (!configFile.exists()) {
                config.options().copyDefaults(true);
                config.save(configFile);
            }
        } else {
            throw new FileNotFoundException("The file '" + filename + "' does not exist");
        }
        return config;
    }

    public static FileConfiguration getConfig(String alias) {
        return configs.get(alias);
    }

}
