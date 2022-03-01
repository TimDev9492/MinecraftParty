package me.timwastaken.minecraftparty.models.templates;

import me.timwastaken.minecraftparty.MinecraftParty;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public abstract class Configurable {

    private FileConfiguration config;
    private File configFile;
    private String filename;

    public Configurable(String filename) {
        this.filename = filename + (filename.endsWith(".yml") ? "" : ".yml");
    }

    public void loadConfig() throws IOException {
        configFile = new File(MinecraftParty.getInstance().getDataFolder(), filename);

        config = YamlConfiguration.loadConfiguration(configFile);

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
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
