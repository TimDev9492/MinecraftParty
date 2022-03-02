package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Lasertag extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.LASERTAG;
    private final ArrayList<BukkitRunnable> gameLoops;
    private Random random;
    private final Player[] players;

    private double hitDamage;
    private Material gunMaterial;
    private int spawnDelta;
    private int shotDelayTicks;
    private int gameTimer;
    private HashMap<UUID, Integer> points;
    private ConcurrentHashMap<UUID, Long> lastTimeFired;
    private boolean IS_RUNNING = false;

    public Lasertag(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_FALL_DAMAGE, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT));
        super.addGameEventListeners(this);
        this.gameLoops = new ArrayList<>();
        this.players = players;
        this.points = new HashMap<>();
        this.lastTimeFired = new ConcurrentHashMap<>();
        for (Player p : players) {
            points.put(p.getUniqueId(), 0);
            lastTimeFired.put(p.getUniqueId(), -1L);
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
        timer.runTaskTimer(MinecraftParty.getInstance(), 0L, 20L);
        gameLoops.add(timer);
    }

    public void onPlayerShootGun(Player p) {
        if (!IS_RUNNING || !lastTimeFired.containsKey(p.getUniqueId())) return;

        long millisSinceLast = System.currentTimeMillis() - lastTimeFired.get(p.getUniqueId());
        if (millisSinceLast / 1000f * 20 >= shotDelayTicks) {
            Location start = p.getEyeLocation().clone();
            Vector direction = p.getEyeLocation().getDirection();
            Color randomColor = Color.fromBGR(random.nextInt(0b111111111111111111111111 + 1));

            float max = 32;
            float delta = 0.2f;
            for (int i = 0; i <= max; i++) {
                gameWorld.spawnParticle(Particle.REDSTONE, start.clone().add(direction.multiply(1 + i * delta)), 1, new Particle.DustOptions(randomColor, 1 + 5 * i / max));
            }
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1f, 1f);
            boolean hit = false;
            for (Player other : players) {
                if (p.getUniqueId() != other.getUniqueId()) {
                    if (playerLooksAtPlayer(p, other)) {
                        onPlayerDeath(other);
                        givePointToPlayer(p);
                        hit = true;
                    }
                }
            }
            if (!hit) {
                long time = System.currentTimeMillis();
                lastTimeFired.put(p.getUniqueId(), time);
                BukkitRunnable display = new BukkitRunnable() {
                    final Player displayPlayer = p;
                    final long lastTime = time;

                    @Override
                    public void run() {
                        long millis = System.currentTimeMillis() - lastTime;
                        float percentage = Math.min(millis / 1000f * 20 / shotDelayTicks, 1);
                        displayPlayer.setExp(percentage);
                        if (percentage == 1) this.cancel();
                    }
                };
                display.runTaskTimer(MinecraftParty.getInstance(), 0L, 1L);
                gameLoops.add(display);
            }
        }
    }

    public void onPlayerDeath(Player p) {
        p.setHealth(20);
        p.teleport(randomSpawnOnMap());
        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 2f);
    }

    public void givePointToPlayer(Player damager) {
        int newScore = 0;
        if (points.containsKey(damager.getUniqueId())) {
            newScore = points.get(damager.getUniqueId()) + 1;
            points.put(damager.getUniqueId(), newScore);
            damager.playSound(damager.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
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

    private boolean playerLooksAtPlayer(Player looking, Player at) {
        Vector atCenter = at.getLocation().clone().add(0, 0.9, 0).toVector();
        double distEyeToCenter = looking.getEyeLocation().toVector().distance(atCenter);
        if (gameWorld.rayTraceBlocks(looking.getEyeLocation(), looking.getEyeLocation().getDirection(), distEyeToCenter) != null)
            return false;
        Vector mapped = looking.getEyeLocation().toVector().add(looking.getEyeLocation().getDirection().multiply(distEyeToCenter));
        double distHoriz = atCenter.clone().setY(0).distance(mapped.clone().setY(0)); // horizontal distance
        double distVert = mapped.getY() - at.getLocation().getY();
        return distHoriz <= 0.43f && distVert >= 0 && distVert <= 2;
    }

    public Material getGunMaterial() {
        return gunMaterial;
    }

    public double getHitDamage() {
        return hitDamage;
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
        gunMaterial = Material.valueOf(getConfig().getString("gun_material"));
        spawnDelta = getConfig().getInt("spawn_delta");
        hitDamage = getConfig().getDouble("hit_damage");
        shotDelayTicks = getConfig().getInt("shot_delay_ticks");
        gameTimer = getConfig().getInt("game_time_seconds");
        ItemStack gun = new ItemStack(gunMaterial);
        ItemMeta meta = gun.getItemMeta();
        meta.setUnbreakable(true);
        gun.setItemMeta(meta);
        for (Player player : players) {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().addItem(gun);
            player.teleport(randomSpawnOnMap());
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

}
