package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
    private HashMap<UUID, Integer> points;
    private ConcurrentHashMap<UUID, Long> lastTimeFired;
    private boolean IS_RUNNING = false;

    public Lasertag(Player... players) {
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
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1f , 1f);
    }

    public void givePointToPlayer(Player damager) {
        if (points.containsKey(damager.getUniqueId())) {
            points.put(damager.getUniqueId(), points.get(damager.getUniqueId()) + 1);
            damager.playSound(damager.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
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
        if (gameWorld.rayTraceBlocks(looking.getEyeLocation(), looking.getEyeLocation().getDirection(), distEyeToCenter) != null) return false;
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
    }

    @Override
    public void onWorldLoaded() {
        ConfigurationSection section = MinecraftParty.getInstance().getConfig().getConfigurationSection("minigames." + type.getAlias());
        gunMaterial = Material.valueOf(section.getString("gun_material"));
        spawnDelta = section.getInt("spawn_delta");
        hitDamage = section.getDouble("hit_damage");
        shotDelayTicks = section.getInt("shot_delay_ticks");
        ItemStack gun = new ItemStack(gunMaterial);
        ItemMeta meta = gun.getItemMeta();
        meta.setUnbreakable(true);
        gun.setItemMeta(meta);
        for (Player player : players) {
            player.getInventory().addItem(gun);
            player.teleport(randomSpawnOnMap());
        }
    }

    @Override
    public void onPlayerLeave(Player p) {

    }

}
