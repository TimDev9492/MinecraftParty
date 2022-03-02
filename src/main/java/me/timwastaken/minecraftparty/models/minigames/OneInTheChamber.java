package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OneInTheChamber extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.ONE_IN_THE_CHAMBER;
    private final ArrayList<BukkitRunnable> gameLoops;
    private Random random;
    private final Player[] players;

    private int spawnDelta;
    private Material weaponMaterial;
    private int arrowGetDelayTicks;
    private int gameTimer;
    private HashMap<UUID, Integer> points;
    private boolean IS_RUNNING = false;

    public OneInTheChamber(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_FALL_DAMAGE, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT));
        super.addGameEventListeners(this);
        this.gameLoops = new ArrayList<>();
        this.players = players;
        this.points = new HashMap<>();
        for (Player player : players) {
            points.put(player.getUniqueId(), 0);
        }
        this.random = new Random();
    }

    @Override
    public void onGameStart() {
        IS_RUNNING = true;
        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                ScoreboardSystem.refreshScoreboards();
                if (gameTimer == 0) {
                    gameEnd();
                }
                gameTimer--;
            }
        };
        BukkitRunnable arrowTimer = new BukkitRunnable() {
            int counter = arrowGetDelayTicks - 1;
            @Override
            public void run() {
                if (++counter >= arrowGetDelayTicks) {
                    for (Player p : players) {
                        giveArrowToPlayer(p);
                    }
                    counter = 0;
                }
                float percentage = (float) counter / arrowGetDelayTicks;
                for (Player p : players) {
                    p.setExp(percentage);
                }
            }
        };
        timer.runTaskTimer(MinecraftParty.getInstance(), 0L, 20L);
        arrowTimer.runTaskTimer(MinecraftParty.getInstance(), 0L, 1L);
        gameLoops.add(timer);
        gameLoops.add(arrowTimer);
    }

    public void onPlayerDeath(Player p) {
        p.setHealth(20);
        p.teleport(randomSpawnOnMap());
        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 2f);
        clearAllArrows(p);
        giveArrowToPlayer(p);
    }

    public void givePointToPlayer(Player damager) {
        int newScore = 0;
        if (points.containsKey(damager.getUniqueId())) {
            newScore = points.get(damager.getUniqueId()) + 1;
            points.put(damager.getUniqueId(), newScore);
            damager.playSound(damager.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            giveArrowToPlayer(damager);
        }
        ScoreboardSystem.refreshScoreboards();
    }

    private void gameEnd() {
        ArrayList<Player> winners = new ArrayList<>();
        int measure = -1;
        for (Player p : players) {
            int currentScore = points.get(p.getUniqueId());
            if (currentScore == measure) {
                winners.add(p);
            } else if (currentScore > measure) {
                winners.clear();
                winners.add(p);
                measure = currentScore;
            }
        }
        announceWinners(winners);
    }

    private void announceWinners(ArrayList<Player> winners) {
        NotificationManager.announceGameWinners(winners.toArray(new Player[0]));
        endGame();
    }

    private Location randomSpawnOnMap() {
        int x = random.nextInt(-spawnDelta, spawnDelta + 1);
        int z = random.nextInt(-spawnDelta, spawnDelta + 1);
        Location loc = new Location(gameWorld, x, 0, z);
        while (loc.getBlock().getType() == Material.AIR || loc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR || loc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR) {
            loc.add(0, 1, 0);
        }
        loc.add(0.5, 1, 0.5);
        return loc;
    }

    public Material getWeaponMaterial() {
        return weaponMaterial;
    }

    public boolean isRunning() {
        return IS_RUNNING;
    }

    @Override
    public void onGameEnd() {
        IS_RUNNING = false;
        gameLoops.forEach(BukkitRunnable::cancel);
        Arrays.stream(players).forEach(p -> p.getInventory().clear());
        addFlag(MinigameFlag.NO_PVP);
    }

    @Override
    public void onWorldLoaded() {
        weaponMaterial = Material.valueOf(getConfig().getString("weapon_material"));
        spawnDelta = getConfig().getInt("spawn_delta");
        arrowGetDelayTicks = getConfig().getInt("arrow_get_delay_ticks");
        gameTimer = getConfig().getInt("game_time_seconds");
        ItemStack weapon = new ItemStack(weaponMaterial);
        ItemMeta meta = weapon.getItemMeta();
        meta.setUnbreakable(true);
        weapon.setItemMeta(meta);
        ItemStack bow = new ItemStack(Material.BOW);
        meta = bow.getItemMeta();
        meta.setUnbreakable(true);
        bow.setItemMeta(meta);
        ItemStack arrow = new ItemStack(Material.ARROW);
        for (Player player : players) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().addItem(weapon, bow);
            player.teleport(randomSpawnOnMap());
        }
    }

    private void giveArrowToPlayer(Player p) {
        // increases the arrow amount on last hotbar slot
        ItemStack lastSlot;
        if ((lastSlot = p.getInventory().getItem(8)) != null) {
            // last slot is occupied
            if (lastSlot.getType() == Material.ARROW && lastSlot.getAmount() < lastSlot.getMaxStackSize())
                lastSlot.setAmount(lastSlot.getAmount() + 1);
            else
                p.getInventory().addItem(new ItemStack(Material.ARROW));
        } else {
            p.getInventory().setItem(8, new ItemStack(Material.ARROW));
        }
    }

    private void clearAllArrows(Player p) {
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack == null) continue;
            if (stack.getType() == Material.ARROW)
                stack.setAmount(0);
        }
    }

    @Override
    public void onPlayerLeave(Player p) {

    }

    @Override
    public void onPlayerJoin(Player p) {

    }

    @Override
    public List<String> getScoreboardList() {
        List<String> toReturn = new ArrayList<>();
        if (IS_RUNNING) {
            toReturn.add(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", gameTimer / 60, gameTimer % 60));
        }
        Map<UUID, Integer> sorted = MinecraftParty.sortMap(points);
        for (Map.Entry<UUID, Integer> entry : sorted.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            toReturn.add(ChatColor.GOLD + "" + entry.getValue() + " " + ChatColor.GRAY + p.getName());
        }
        return toReturn;
    }

    @Override
    public String getPersonalLine(Player p) {
        return ChatColor.YELLOW + "" + ChatColor.ITALIC + "Kills: " + ChatColor.RESET + points.get(p.getUniqueId());
    }

    public void cloneArrow(Projectile arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Arrow clonedArrow = (Arrow) gameWorld.spawnEntity(arrow.getLocation().clone().add(arrow.getVelocity().clone().multiply(1 / 20f)), EntityType.ARROW);
                clonedArrow.setVelocity(arrow.getVelocity().clone());
                clonedArrow.setShooter(arrow.getShooter());
                clonedArrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            }
        }.runTaskLater(MinecraftParty.getInstance(), 1L);
    }

}
