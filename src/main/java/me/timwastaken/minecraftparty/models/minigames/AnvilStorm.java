package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.*;

public class AnvilStorm extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.ANVIL_STORM;
    private final ArrayList<BukkitRunnable> gameLoops;
    private final ArrayList<UUID> ingamePlayers;
    private final Random random;

    private final int delta;
    private final int fallHeight;
    private final double anvilStartRate;

    public AnvilStorm(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_PVP, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT));
        super.addGameEventListeners(this);

        gameLoops = new ArrayList<>();
        this.delta = getConfig().getInt("delta");
        this.fallHeight = getConfig().getInt("fall_height");
        this.anvilStartRate = getConfig().getDouble("anvils_per_second");
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
                NotificationManager.notifyPlayerOut(p, "You were hit by an anvil!");
            } else {
                NotificationManager.announceGameWinner(Bukkit.getPlayer(ingamePlayers.get(0)));
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
