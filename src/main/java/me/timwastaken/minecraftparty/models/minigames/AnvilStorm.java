package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.GameEventListener;
import me.timwastaken.minecraftparty.models.Minigame;
import me.timwastaken.minecraftparty.models.MinigameType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class AnvilStorm extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.ANVIL_STORM;
    private final ArrayList<BukkitRunnable> gameLoops;
    private final ArrayList<UUID> ingamePlayers;
    private final Random random;

    private final int delta;
    private final int fallHeight;
    private final double anvilStartRate;

    public AnvilStorm(Player... players) {
        super(type);
        super.addGameEventListeners(this);

        gameLoops = new ArrayList<>();
        ConfigurationSection section = MinecraftParty.getInstance().getConfig().getConfigurationSection("minigames." + type.getAlias());
        this.delta = section.getInt("delta");
        this.fallHeight = section.getInt("fall_height");
        this.anvilStartRate = section.getDouble("anvils_per_second");
        this.ingamePlayers = new ArrayList<>();
        this.random = new Random();
        Arrays.stream(players).forEach(player -> ingamePlayers.add(player.getUniqueId()));
    }

    private void spawnRandomAnvil() {
        int dx = random.nextInt(-delta, delta + 1);
        int dz = random.nextInt(-delta, delta + 1);
//        FallingBlock anvil = origin.getWorld().spawnFallingBlock(origin.clone().add(dx + 0.5, fallHeight, dz + 0.5), Material.ANVIL.createBlockData());
        origin.getWorld().getBlockAt(origin.clone().add(dx, fallHeight, dz)).setType(Material.ANVIL);
    }

    public void onAnvilHitPlayer(Player p) {
        if (ingamePlayers.remove(p.getUniqueId())) {
            if (ingamePlayers.size() > 1) {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 2f);
                p.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You're out!", ChatColor.GRAY + "You were hit by an anvil", 10, 80, 10);
            } else {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                    if (ingamePlayers.size() == 0)
                        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Game won!", ChatColor.GRAY + "You won the game!", 10, 80, 10);
                    else
                        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + Bukkit.getPlayer(ingamePlayers.get(0)).getName(), ChatColor.GRAY + "won the game", 10, 80, 10);
                });
            }
            p.setGameMode(GameMode.SPECTATOR);
        }
    }

    @Override
    public void onGameStart() {
        BukkitRunnable gameLoop = new BukkitRunnable() {
            int counter = 0;
            double acc = 0;
            double anvilsPerSecond = anvilStartRate;
            double ticks = 0;

            @Override
            public void run() {
                if (anvilsPerSecond <= 20) {
                    counter++;
                    if (counter >= 20 / anvilsPerSecond) {
                        spawnRandomAnvil();
                    }
                } else {
                    double anvilAmount = anvilsPerSecond / 20f;
                    int distinctAnvils = (int) Math.floor(anvilAmount);
                    acc += anvilAmount - distinctAnvils;
                    if (acc >= 1) {
                        distinctAnvils++;
                        acc--;
                    }
                    for (int i = 0; i < distinctAnvils; i++) {
                        spawnRandomAnvil();
                    }
                }
                anvilsPerSecond = 0.005 * ticks * ticks + anvilStartRate;
                ticks++;
                if (ingamePlayers.size() == 1) {
                    endGame();
                }
            }
        };
        gameLoops.add(gameLoop);
        gameLoop.runTaskTimer(MinecraftParty.getInstance(), 0L, 1L);
    }

    @Override
    public void onGameEnd() {
        new BukkitRunnable() {
            @Override
            public void run() {
                gameWorld.getNearbyEntities(origin, delta + 1, fallHeight, delta + 1, entity -> entity instanceof FallingBlock fallingBlock && fallingBlock.getBlockData().getMaterial() == Material.ANVIL).forEach(Entity::remove);
            }
        }.runTaskLater(MinecraftParty.getInstance(), 2L);
        gameLoops.forEach(BukkitRunnable::cancel);
    }

    @Override
    public void onWorldLoaded() {
        Bukkit.getOnlinePlayers().forEach(player -> player.teleport(origin));
    }

    @Override
    public void onPlayerLeave(Player p) {
        ingamePlayers.remove(p.getUniqueId());
    }


}
