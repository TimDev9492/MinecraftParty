package me.timwastaken.minecraftparty.models.minigames.hotpotato;

import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.other.MinigameUtils;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HotPotato extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.HOT_POTATO;

    // config values
    private double spawnRadius;
    private List<Material> allowedSpawningMaterials;
    private int potatoPhaseTicks;
    private int potatoSpeedLevel;
    private int gameTimeSeconds;

    private final List<Player> participants;
    private List<Player> hotPlayers;
    private final ItemStack potato;
    private BossBar bossBar;

    public HotPotato(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.ZERO_DAMAGE, MinigameFlag.NO_FALL_DAMAGE, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT, MinigameFlag.NO_BLOCK_INTERACTION));
        super.addGameEventListeners(this);
        participants = new ArrayList<>(Arrays.asList(players));
        potato = new ItemStack(Material.BAKED_POTATO);
        ItemMeta meta = potato.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Hot Potato");
        potato.setItemMeta(meta);
        hotPlayers = new ArrayList<>();
    }

    private void killHotPlayers() {
        hotPlayers.forEach(this::removePlayer);
    }

    private void makePlayersHot() {
        int hotAmount = participants.size() / 2;
        for (int i = 0; i < hotAmount; i++) {
            Player willBeHot = participants.get(rnd.nextInt(participants.size()));
            makePotato(willBeHot);
        }
    }

    public void onPlayerHitPlayer(Player hit, Player takeHit) {
        if (hotPlayers.contains(hit) && participants.contains(takeHit)) {
            removePotato(hit);
            makePotato(takeHit);
        }
    }

    private void makePotato(Player p) {
        // fill hotbar with potatoes
        hotPlayers.add(p);
        participants.remove(p);
        for (int i = 0; i < 9; i++) {
            if (i != 4 || p.getInventory().getItem(4) == null)
                p.getInventory().setItem(i, potato);
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, potatoSpeedLevel - 1, false, false));
        p.getInventory().setItem(EquipmentSlot.HEAD, new ItemStack(Material.TNT));
    }

    private void removePotato(Player p) {
        // remove potato items from inventory
        hotPlayers.remove(p);
        participants.add(p);
        p.getInventory().remove(potato.getType());
        p.getInventory().setItem(EquipmentSlot.HEAD, null);
        p.removePotionEffect(PotionEffectType.SPEED);
    }

    private void removePlayer(Player p) {
        NotificationManager.notifyPlayerOut(p, "You held onto the potato for too long");
        gameWorld.createExplosion(p.getLocation(), 0);
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(origin);
        participants.remove(p);
        hotPlayers.remove(p);
    }

    private void updateBossBar(double percentage) {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(ChatColor.DARK_RED + "" + ChatColor.BOLD + "HOT POTATO", BarColor.RED, BarStyle.SEGMENTED_10);
            gameWorld.getPlayers().forEach(bossBar::addPlayer);
        }
        bossBar.setProgress(percentage);
    }

    @Override
    public void onGameStart() {
        addGameLoop(new BukkitRunnable() {
            int counter = 0;
            @Override
            public void run() {
                double bossPercentage = 1 - (float) counter / potatoPhaseTicks;
                if (counter == 0) {
                    killHotPlayers();
                    makePlayersHot();
                } else if (counter == potatoPhaseTicks) {
                    killHotPlayers();
                } else if (counter == potatoPhaseTicks + 20) {
                    if (checkGameEnd()) {
                        this.cancel();
                        return;
                    }
                    makePlayersHot();
                    counter = 0;
                }
                updateBossBar(Math.max(bossPercentage, 0));
                counter++;
            }
        }, 0L, 1L);
    }

    private boolean checkGameEnd() {
        if (participants.size() == 1) {
            NotificationManager.announceGameWinners(participants.get(0));
            endGame();
        }
        return participants.size() == 1;
    }

    @Override
    public void onGameEnd() {
        bossBar.setVisible(false);
        bossBar.removeAll();
    }

    @Override
    public void onWorldLoaded() {
        spawnRadius = getConfig().getDouble("spawn_radius");
        allowedSpawningMaterials = getConfig().getStringList("allowed_spawning_materials").stream().map(Material::valueOf).toList();
        potatoPhaseTicks = getConfig().getInt("potato_phase_ticks");
        potatoSpeedLevel = getConfig().getInt("potato_speed_level");
        gameTimeSeconds = getConfig().getInt("game_time_seconds");

        participants.forEach(p -> {
            p.teleport(newPlayerSpawn());
        });
    }

    @Override
    public void onPlayerLeave(Player p) {

    }

    @Override
    public void onPlayerJoin(Player p) {

    }

    private Location newPlayerSpawn() {
        Block spawnBlock;
        do {
            Vector coordinates = MinigameUtils.uniformCircleDistribution(origin.toVector(), spawnRadius);
            spawnBlock = gameWorld.getHighestBlockAt(coordinates.getBlockX(), coordinates.getBlockZ());
        } while (!allowedSpawningMaterials.contains(spawnBlock.getType()));
        return spawnBlock.getLocation().add(0.5, 1, 0.5);
    }

}
