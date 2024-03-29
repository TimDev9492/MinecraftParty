package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.KitManager;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.other.MinigameUtils;
import me.timwastaken.minecraftparty.models.templates.InvLayoutBasedMinigame;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Score;

import java.io.IOException;
import java.util.*;

public class MlgRush extends InvLayoutBasedMinigame implements GameEventListener {

    private static final MinigameType type = MinigameType.MLG_RUSH;

    private final Random rnd;

    private final HashMap<UUID, Integer> gamesPlayed;
    private final HashMap<UUID, Integer> playerLives;

    private UUID[] currentlyFighting;
    private Location[] fightSpawns;
    private Location[] bedLocations;
    private Location spectatorSpawn;
    private ItemStack[] fighterInv;
    private final Player[] players;

    private int startLives;
    private int resetDepth;
    private int deathY;
    private int buildHeight;
    private double bedProtectionRadius;
    private Material bedMaterial;

    public MlgRush(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.ZERO_DAMAGE, MinigameFlag.NO_BLOCK_DROPS, MinigameFlag.NO_MAP_BREAKING), players);
        super.addGameEventListeners(this);
        this.players = players;
        rnd = new Random();
        gamesPlayed = new HashMap<>();
        playerLives = new HashMap<>();
        currentlyFighting = new UUID[2];
    }

    public int getDeathY() {
        return deathY;
    }

    public boolean isFighting(Player p) {
        return currentlyFighting[0] == p.getUniqueId() || currentlyFighting[1] == p.getUniqueId();
    }

    @Override
    public void onGameStart() {
        deathY = origin.getBlockY() - resetDepth;
        generateNewFightingPlayers();
    }

    @Override
    public void onGameEnd() {
        resetMap();
    }

    public Material getBedMaterial() {
        return bedMaterial;
    }

    @Override
    public void onWorldLoaded() {
        fightSpawns = new Location[]{
                new Location(gameWorld, getConfig().getInt("spawn1.x") + 0.5, getConfig().getInt("spawn1.y"), getConfig().getInt("spawn1.z") + 0.5),
                new Location(gameWorld, getConfig().getInt("spawn2.x") + 0.5, getConfig().getInt("spawn2.y"), getConfig().getInt("spawn2.z") + 0.5)
        };
        fightSpawns[0].setYaw((float) getConfig().getDouble("spawn1.yaw"));
        fightSpawns[1].setYaw((float) getConfig().getDouble("spawn2.yaw"));
        bedLocations = new Location[]{
                new Location(gameWorld, getConfig().getInt("bed1.x") + 0.5, getConfig().getInt("bed1.y"), getConfig().getInt("bed1.z") + 0.5),
                new Location(gameWorld, getConfig().getInt("bed2.x") + 0.5, getConfig().getInt("bed2.y"), getConfig().getInt("bed2.z") + 0.5)
        };
        spectatorSpawn = new Location(gameWorld, getConfig().getInt("spectator_spawn.x") + 0.5, getConfig().getInt("spectator_spawn.y"), getConfig().getInt("spectator_spawn.z") + 0.5);
        spectatorSpawn.setPitch((float) getConfig().getDouble("spectator_spawn.pitch"));
        spectatorSpawn.setYaw((float) getConfig().getDouble("spectator_spawn.yaw"));
        resetDepth = getConfig().getInt("reset_depth");
        buildHeight = origin.getBlockY() + getConfig().getInt("build_height") - 1;
        bedMaterial = Material.valueOf(getConfig().getString("bed_material"));
        bedProtectionRadius = getConfig().getDouble("bed_protection_radius");
        startLives = getConfig().getInt("lives");
        loadKit(KitManager.getKit(type.getAlias()));

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

    public void bedBroken(Player by, Block bed) {
        // Add point system
        if (!isFighting(by)) return;
        Player winner = by, looser;
        if (by.getUniqueId() == currentlyFighting[0]) {
            looser = Bukkit.getPlayer(currentlyFighting[1]);
        } else {
            looser = Bukkit.getPlayer(currentlyFighting[0]);
        }
        if (bed.getLocation().distanceSquared(getSpawn(winner)) < bed.getLocation().distanceSquared(getSpawn(looser))) {
            return;
        }
        makeSpectator(winner);
        makeSpectator(looser);
        winner.playSound(winner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        looser.playSound(looser.getLocation(), Sound.ENTITY_CAT_HISS, 1f, 1f);
        removeLife(looser.getUniqueId());
        ScoreboardSystem.refreshScoreboards();
        resetMap();
        if (gamesPlayed.keySet().size() > 1) generateNewFightingPlayers();
    }

    public void teleportBack(Player p) {
        if (!isFighting(p)) return;
        p.setFallDistance(0);
        if (p.getUniqueId() == currentlyFighting[0]) {
            p.teleport(fightSpawns[0]);
        } else {
            p.teleport(fightSpawns[1]);
        }
        p.setGameMode(GameMode.SURVIVAL);
        resetInventory(p);
    }

    private Location getSpawn(Player p) {
        if (!isFighting(p)) return null;
        if (p.getUniqueId() == currentlyFighting[0]) {
            return fightSpawns[0];
        } else {
            return fightSpawns[1];
        }
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

    public boolean isNearOwnBed(Player player, Block block) {
        if (!isFighting(player)) return false;
        Location comp = block.getLocation().add(0.5, 0, 0.5);
        Location bed;
        Location spawn;
        if (player.getUniqueId() == currentlyFighting[0]) {
            bed = bedLocations[0];
            spawn = fightSpawns[0];
        } else {
            bed = bedLocations[1];
            spawn = fightSpawns[1];
        }
        return bed.distance(comp) <= bedProtectionRadius || spawn.distance(comp) <= bedProtectionRadius;
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
