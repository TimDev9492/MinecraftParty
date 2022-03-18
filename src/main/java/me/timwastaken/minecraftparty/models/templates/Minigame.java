package me.timwastaken.minecraftparty.models.templates;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.ConfigManager;
import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.managers.ScoreSystem;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;

public abstract class Minigame {

    protected MinigameType type;
    protected List<MinigameFlag> flags;
    private final String gameWorldName;
    protected World gameWorld;
    private long whenStarted = -1;
    private boolean isRunning = false;
    private boolean hasStarted = false;
    protected Location origin;
    private final ArrayList<Block> placedBlocks;
    private final ArrayList<Entity> spawnedEntities;
    private ArrayList<BukkitRunnable> gameLoops;

    private final ArrayList<GameEventListener> gameEventListeners;

    public Minigame(MinigameType type, List<MinigameFlag> flags) throws IOException {
        this.type = type;
        this.flags = new ArrayList<>(flags);
        this.gameEventListeners = new ArrayList<>();
        this.gameWorldName = type.getWorldName();
        this.placedBlocks = new ArrayList<>();
        this.spawnedEntities = new ArrayList<>();
        this.gameLoops = new ArrayList<>();
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
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.teleport(origin);
            p.getInventory().clear();
        });
        gameEventListeners.forEach(GameEventListener::onWorldLoaded);
        ScoreSystem.addPlayers(Bukkit.getOnlinePlayers().toArray(new Player[0]));
        ScoreboardSystem.addPlayerScoreboard(Bukkit.getOnlinePlayers().toArray(new Player[0]));
        ScoreboardSystem.refreshScoreboards();
    }

    public void unloadWorld() {
        if (!gameWorldName.equals(GameManager.getDefaultWorldName()))
            Bukkit.getServer().unloadWorld(gameWorld, false);
    }

    public void addPlacedBlock(Block b) {
        if (!placedBlocks.contains(b))
            placedBlocks.add(b);
    }

    public void removePlacedBlock(Block b) {
        placedBlocks.remove(b);
    }

    public boolean isPlacedBlock(Block b) {
        return placedBlocks.contains(b);
    }

    public void resetMap() {
        for (int i = placedBlocks.size() - 1; i >= 0; i--) {
            placedBlocks.get(i).setType(Material.AIR);
            placedBlocks.remove(i);
        }
        spawnedEntities.forEach(Entity::remove);
    }

    public void startCountdown() {
        addGameLoop(new BukkitRunnable() {
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
        }, 0L, 20L);
    }

    private void startGame() {
        isRunning = true;
        hasStarted = true;
        whenStarted = System.currentTimeMillis();
        gameEventListeners.forEach(GameEventListener::onGameStart);
    }

    public void endGame() {
        isRunning = false;
        gameEventListeners.forEach(GameEventListener::onGameEnd);
        gameLoops.forEach(BukkitRunnable::cancel);
        new BukkitRunnable() {
            @Override
            public void run() {
                closeWorld();
                GameManager.clearMinigame();
                ScoreboardSystem.refreshScoreboards();
            }
        }.runTaskLater(MinecraftParty.getInstance(), 60L);
    }

    public void forceStop() {
        isRunning = false;
        gameEventListeners.forEach(GameEventListener::onGameEnd);
        gameLoops.forEach(BukkitRunnable::cancel);
        closeWorld();
        GameManager.clearMinigame();
        ScoreboardSystem.refreshScoreboards();
    }

//    public void abort() {
//        isRunning = false;
//        gameEventListeners.forEach(GameEventListener::onGameEnd);
//        closeWorld();
//    }

    private void closeWorld() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.setFallDistance(0);
            p.teleport(GameManager.getOrigin());
            p.getActivePotionEffects().forEach(potionEffect -> {
                p.removePotionEffect(potionEffect.getType());
            });
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(20);
        });
        unloadWorld();
    }

    public void addGameLoop(BukkitRunnable runnable, long delay, long period) {
        gameLoops.add(runnable);
        runnable.runTaskTimer(MinecraftParty.getInstance(), delay, period);
    }

    public void addTask(BukkitRunnable runnable, long delay) {
        gameLoops.add(runnable);
        runnable.runTaskLater(MinecraftParty.getInstance(), delay);
    }

    public List<String> getScoreboardList() {
        return null;
    }

    public String getPersonalLine(Player p) {
        return null;
    }

    public boolean hasFlag(MinigameFlag flag) {
        return flags != null && flags.contains(flag);
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

    public boolean hasStarted() {
        return hasStarted;
    }

    public long getWhenStarted() {
        return whenStarted;
    }

    protected FileConfiguration getConfig() {
        return ConfigManager.getConfig(type.getAlias());
    }

    protected void addFlag(MinigameFlag flag) {
        if (!this.flags.contains(flag)) this.flags.add(flag);
    }

    protected boolean isSpawnedEntity(Entity entity) {
        return spawnedEntities.contains(entity);
    }

    public void addSpawnedEntity(Entity entity) {
        spawnedEntities.add(entity);
    }
}
