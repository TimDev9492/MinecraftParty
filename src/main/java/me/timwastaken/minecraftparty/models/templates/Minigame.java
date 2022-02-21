package me.timwastaken.minecraftparty.models.templates;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.models.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public abstract class Minigame {

    protected MinigameType type;
    private final String gameWorldName;
    protected World gameWorld;
    private long whenStarted;
    private boolean isRunning = false;
    protected Location origin;

    private final ArrayList<GameEventListener> gameEventListeners;

    public Minigame(MinigameType type) {
        this.type = type;
        this.gameEventListeners = new ArrayList<>();
        this.gameWorldName = type.getWorldName();
    }

    public void addGameEventListeners(GameEventListener... listeners) {
        gameEventListeners.addAll(List.of(listeners));
    }

    public void loadWorld() {
        if ((gameWorld = Bukkit.getWorld(gameWorldName)) == null) {
            WorldCreator creator = new WorldCreator(gameWorldName);
            creator.generator("VoidGen");
            creator.environment(World.Environment.NORMAL);
            gameWorld = creator.createWorld();
            gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }
        origin = type.getOrigin().toLocation(gameWorld);
        gameEventListeners.forEach(GameEventListener::onWorldLoaded);
    }

    public void unloadWorld() {
        if (!gameWorldName.equals(GameManager.getDefaultWorldName()))
            Bukkit.getServer().unloadWorld(gameWorld, true);
    }

    public void startCountdown() {
        new BukkitRunnable() {
            int seconds = 5;

            @Override
            public void run() {
                if (seconds > 0) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendTitle(type.getDisplayName(), ChatColor.GRAY + "Starting in " + seconds + " seconds", 0, 21, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.5f);
                    });
                } else {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendTitle(type.getDisplayName(), ChatColor.GRAY + "Game started!", 0, 50, 10);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1f);
                    });
                    startGame();
                    this.cancel();
                }
                seconds--;
            }
        }.runTaskTimer(MinecraftParty.getInstance(), 0L, 20L);
    }

    private void startGame() {
        isRunning = true;
        whenStarted = System.currentTimeMillis();
        gameEventListeners.forEach(GameEventListener::onGameStart);
    }

    public void endGame() {
        isRunning = false;
        gameEventListeners.forEach(GameEventListener::onGameEnd);
        new BukkitRunnable() {
            @Override
            public void run() {
                closeWorld();
                GameManager.clearMinigame();
            }
        }.runTaskLater(MinecraftParty.getInstance(), 60L);
    }

//    public void abort() {
//        isRunning = false;
//        gameEventListeners.forEach(GameEventListener::onGameEnd);
//        closeWorld();
//    }

    private void closeWorld() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.teleport(GameManager.getOrigin());
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
        });
        unloadWorld();
    }

    public String getDisplayName() {
        return type.getDisplayName();
    }

    public String getAlias() {
        return type.getAlias();
    }

    public int getReward() {
        return type.getReward();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public long getWhenStarted() {
        return whenStarted;
    }

}