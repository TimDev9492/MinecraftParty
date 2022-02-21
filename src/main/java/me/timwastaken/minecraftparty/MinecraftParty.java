package me.timwastaken.minecraftparty;

import me.timwastaken.minecraftparty.commands.MinigameCommand;
import me.timwastaken.minecraftparty.listeners.GameListener;
import me.timwastaken.minecraftparty.listeners.GlobalListener;
import me.timwastaken.minecraftparty.managers.DatabaseManager;
import me.timwastaken.minecraftparty.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftParty extends JavaPlugin {

    private static MinecraftParty instance;

    public static MinecraftParty getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        DatabaseManager.init();
        DatabaseManager.connect();
        GameManager.init();

        Bukkit.getPluginManager().registerEvents(new GlobalListener(), this);
        Bukkit.getPluginManager().registerEvents(new GameListener(), this);

        getCommand("minigame").setExecutor(new MinigameCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DatabaseManager.disconnect();
    }

}
