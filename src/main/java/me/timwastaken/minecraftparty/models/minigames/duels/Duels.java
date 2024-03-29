package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.KitManager;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.other.InventoryKit;
import me.timwastaken.minecraftparty.models.other.MinigameUtils;
import me.timwastaken.minecraftparty.models.templates.InvLayoutBasedMinigame;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;

public class Duels extends InvLayoutBasedMinigame implements GameEventListener {

    private static final MinigameType type = MinigameType.DUELS;

    private final Random rnd;

    private final HashMap<UUID, Integer> gamesPlayed;
    private final HashMap<UUID, Integer> playerLives;

    private UUID[] currentlyFighting;
    private Location[] fightSpawns;
    private Location spectatorSpawn;
    private final Player[] players;

    private int startLives;
    private int buildHeight;

    public Duels(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_BLOCK_DROPS, MinigameFlag.NO_MAP_BREAKING), players);
        super.addGameEventListeners(this);
        this.players = players;
        rnd = new Random();
        gamesPlayed = new HashMap<>();
        playerLives = new HashMap<>();
        currentlyFighting = new UUID[2];
    }

    public boolean isFighting(Player p) {
        return currentlyFighting[0] == p.getUniqueId() || currentlyFighting[1] == p.getUniqueId();
    }

    @Override
    public void onGameStart() {
        generateNewFightingPlayers();
    }

    @Override
    public void onGameEnd() {
        resetMap();
    }

    @Override
    public void onWorldLoaded() {
        fightSpawns = new Location[]{
                new Location(gameWorld, getConfig().getInt("spawn1.x") + 0.5, getConfig().getInt("spawn1.y"), getConfig().getInt("spawn1.z") + 0.5),
                new Location(gameWorld, getConfig().getInt("spawn2.x") + 0.5, getConfig().getInt("spawn2.y"), getConfig().getInt("spawn2.z") + 0.5)
        };
        fightSpawns[0].setYaw((float) getConfig().getDouble("spawn1.yaw"));
        fightSpawns[1].setYaw((float) getConfig().getDouble("spawn2.yaw"));
        spectatorSpawn = new Location(gameWorld, getConfig().getInt("spectator_spawn.x") + 0.5, getConfig().getInt("spectator_spawn.y"), getConfig().getInt("spectator_spawn.z") + 0.5);
        spectatorSpawn.setPitch((float) getConfig().getDouble("spectator_spawn.pitch"));
        spectatorSpawn.setYaw((float) getConfig().getDouble("spectator_spawn.yaw"));
        buildHeight = origin.getBlockY() + getConfig().getInt("build_height") - 1;
        startLives = getConfig().getInt("lives");
        InventoryKit kit = KitManager.getKit("1_18_pvp");
        loadKit(kit);

        for (Player p : players) {
            gamesPlayed.put(p.getUniqueId(), 0);
            playerLives.put(p.getUniqueId(), startLives);
            p.getInventory().clear();
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(spectatorSpawn);
        }
    }

    @Override
    public void onPlayerLeave(Player p) {
        Player other = currentlyFighting[0] == p.getUniqueId() ? Bukkit.getPlayer(currentlyFighting[1]) : Bukkit.getPlayer(currentlyFighting[0]);
        if (other == null) return;
        gamesPlayed.remove(p.getUniqueId());
        playerLives.remove(p.getUniqueId());
        if (isFighting(p)) {
            resetMap();
            checkEnd();
            if (gamesPlayed.keySet().size() > 1)
                generateNewFightingPlayers();
        }
        ScoreboardSystem.removePlayerScoreboards(p);
        ScoreboardSystem.refreshScoreboards();
    }

    @Override
    public void onPlayerJoin(Player p) {
        makeSpectator(p);
    }

    private void makeSpectator(Player p) {
        p.getInventory().clear();
        p.updateInventory();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(spectatorSpawn);
    }

    public void onPlayerKill(Player by, Player killed) {
        // Add point system
        if (by != null) {
            if (!isFighting(by)) return;
            makeSpectator(by);
            by.playSound(by.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
        if (!isFighting(killed)) return;
        makeSpectator(killed);
        killed.playSound(killed.getLocation(), Sound.ENTITY_CAT_HISS, 1f, 1f);
        removeLife(killed.getUniqueId());
        ScoreboardSystem.refreshScoreboards();
        resetMap();
        if (gamesPlayed.keySet().size() > 1) generateNewFightingPlayers();
    }

    public void teleportBack(Player p) {
        if (!isFighting(p)) return;
        for (ItemStack stack : p.getInventory().getContents()) if (stack != null) p.setCooldown(stack.getType(), 0);
        p.setFallDistance(0);
        p.getActivePotionEffects().forEach(potionEffect -> {
            p.removePotionEffect(potionEffect.getType());
        });
        p.setHealth(20);
        if (p.getUniqueId() == currentlyFighting[0]) {
            p.teleport(fightSpawns[0]);
        } else {
            p.teleport(fightSpawns[1]);
        }
        p.setGameMode(GameMode.SURVIVAL);
        resetInventory(p);
    }

    private void removeLife(UUID uuid) {
        int next = playerLives.get(uuid) - 1;
        playerLives.put(uuid, next);
        if (next == 0) {
            playerOut(uuid);
        }
        checkEnd();
    }

    public int getBuildHeight() {
        return buildHeight;
    }

    private void playerOut(UUID id) {
        gamesPlayed.remove(id);
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        NotificationManager.notifyPlayerOut(p, "out of lives!");
    }

    private void checkEnd() {
        if (gamesPlayed.size() == 1) {
            NotificationManager.announceGameWinners(Bukkit.getPlayer(gamesPlayed.keySet().iterator().next()));
            endGame();
        }
    }

    private void generateNewFightingPlayers() {
        currentlyFighting = MinigameUtils.getNewFightingPair(gamesPlayed);
        gamesPlayed.put(currentlyFighting[0], gamesPlayed.get(currentlyFighting[0]) + 1);
        gamesPlayed.put(currentlyFighting[1], gamesPlayed.get(currentlyFighting[1]) + 1);
        Player p1 = Bukkit.getPlayer(currentlyFighting[0]);
        Player p2 = Bukkit.getPlayer(currentlyFighting[1]);

        teleportBack(p1);
        teleportBack(p2);
    }

    @Override
    public List<String> getScoreboardList() {
        List<String> toReturn = new ArrayList<>();
        Map<UUID, Integer> sorted = MinecraftParty.sortMap(playerLives);
        for (Map.Entry<UUID, Integer> entry : sorted.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            StringBuilder element = new StringBuilder();
            if (entry.getValue() > 0) element.append(ChatColor.GREEN).append("✚".repeat(entry.getValue()));
            else element.append(ChatColor.DARK_RED).append("✘");
            element.append(" ").append(ChatColor.GRAY).append(p.getName());
            toReturn.add(element.toString());
        }
        return toReturn;
    }

    @Override
    public String getPersonalLine(Player p) {
        int lives = playerLives.get(p.getUniqueId());
        return ChatColor.YELLOW + "" + ChatColor.ITALIC + "Lives: " + ChatColor.RESET + (lives > 0 ? ChatColor.GREEN + "✚".repeat(lives) : ChatColor.DARK_RED + "✘");
    }

}
