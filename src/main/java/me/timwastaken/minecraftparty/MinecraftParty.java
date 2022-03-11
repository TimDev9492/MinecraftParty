package me.timwastaken.minecraftparty;

import me.timwastaken.minecraftparty.commands.InventoryCommand;
import me.timwastaken.minecraftparty.commands.MinigameCommand;
import me.timwastaken.minecraftparty.commands.MusicCommand;
import me.timwastaken.minecraftparty.listeners.GameListener;
import me.timwastaken.minecraftparty.listeners.GlobalListener;
import me.timwastaken.minecraftparty.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

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

        new BukkitRunnable() {
            @Override
            public void run() {
                DatabaseManager.init();
                DatabaseManager.connect();
                GameManager.init();
                MusicManager.init();
                try {
                    KitManager.init();
                    InvGuiManager.init();
                    StructureManager.init();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);
        ScoreboardSystem.init();
        try {
            ConfigManager.setDebugging(true);
            ConfigManager.init();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bukkit.getPluginManager().registerEvents(new GlobalListener(), this);
        Bukkit.getPluginManager().registerEvents(new GameListener(), this);
        Bukkit.getPluginManager().registerEvents(new InvGuiManager(), this);

        getCommand("minigame").setExecutor(new MinigameCommand());
        getCommand("music").setExecutor(new MusicCommand());
        getCommand("inventory").setExecutor(new InventoryCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DatabaseManager.disconnect();
    }

    public static <K, V extends Comparable<V>> Map<K, V> sortMap(final Map<K, V> map) {
        // Static Method with return type Map and
        // extending comparator class which compares values
        // associated with two keys
        // return comparison results of values of
        // two keys
        Comparator<K> valueComparator = (k1, k2) -> {
            int comp = -map.get(k1).compareTo(map.get(k2));
            return comp == 0 ? 1 : comp;
        };

        // SortedMap created using the comparator
        Map<K, V> sorted = new TreeMap<>(valueComparator);

        sorted.putAll(map);

        return sorted;
    }

}
