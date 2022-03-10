package me.timwastaken.minecraftparty.models.minigames.kingofthehill;

import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KingOfTheHill extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.KING_OF_THE_HILLS;
    private static final Material plateMaterial = Material.LIGHT_WEIGHTED_PRESSURE_PLATE;
    private final ArrayList<Player> participants;
    private int hillHeight;
    private int gameTimeSeconds;
    private int timer;
    private Material weaponMaterial;
    private int knockbackLevel;
    private final Vector[] directions;

    public KingOfTheHill(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.ZERO_DAMAGE, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT, MinigameFlag.FREEZE_PLAYERS_UNTIL_START));
        super.addGameEventListeners(this);

        participants = new ArrayList<>(Arrays.asList(players));
        directions = new Vector[]{
                new Vector(1, 0, 0),
                new Vector(0, 0, 1),
                new Vector(-1, 0, 0),
                new Vector(0, 0, -1)
        };
    }

    public static Material getPlateMaterial() {
        return plateMaterial;
    }

    @Override
    public void onGameStart() {
        addGameLoop(new BukkitRunnable() {
            @Override
            public void run() {
                if (--timer == 0) gameEnd(null);
                ScoreboardSystem.refreshScoreboards();
            }
        }, 20L, 20L);
    }

    private void gameEnd(Player winner) {
        NotificationManager.announceGameWinners(winner);
        endGame();
    }

    @Override
    public void onGameEnd() {
        resetMap();
    }

    @Override
    public void onWorldLoaded() {
        hillHeight = getConfig().getInt("hill_height");
        gameTimeSeconds = getConfig().getInt("game_time_seconds");
        weaponMaterial = Material.valueOf(getConfig().getString("weapon_material"));
        knockbackLevel = getConfig().getInt("knockback_level");
        ItemStack weapon = new ItemStack(weaponMaterial);
        ItemMeta meta = weapon.getItemMeta();
        meta.addEnchant(Enchantment.KNOCKBACK, knockbackLevel, true);
        weapon.setItemMeta(meta);
        participants.forEach(p -> p.getInventory().addItem(weapon));
        timer = gameTimeSeconds;

        buildTower();
        spreadPlayers();
    }

    private void buildTower() {
        for (int i = 0; i < hillHeight; i++) {
            Block middle = origin.clone().add(0, i, 0).getBlock();
            middle.setType(Material.BARRIER);
            addPlacedBlock(middle);
            for (int j = 0; j < directions.length; j++) {
                Block ladder = middle.getLocation().clone().add(directions[j]).getBlock();
                ladder.setType(Material.LADDER);
                Directional meta = (Directional) ladder.getBlockData();
                meta.setFacing(middle.getFace(ladder));
                ladder.setBlockData(meta);
                addPlacedBlock(ladder);
            }
        }
        Block pressurePlate = origin.clone().add(0, hillHeight, 0).getBlock();
        pressurePlate.setType(plateMaterial);
        addPlacedBlock(pressurePlate);
    }

    public void onPlayerMove(Player p) {
        if (!participants.contains(p)) return;
        if (p.getLocation().getBlock().getType() == plateMaterial) gameEnd(p);
    }

    private void spreadPlayers() {
        float dist = 5;
        for (int i = 0; i < participants.size(); i++) {
            double angle = (float) i / participants.size() * Math.PI * 2;
            Location spawn = origin.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            spawn.setYaw((float) ((angle + Math.PI / 2) * 180 / Math.PI));
            participants.get(i).teleport(spawn);
        }
    }

    @Override
    public void onPlayerLeave(Player p) {
        participants.remove(p);
    }

    @Override
    public void onPlayerJoin(Player p) {
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(origin);
    }

    @Override
    public List<String> getScoreboardList() {
        String timeStr = String.format("%02d:%02d", timer / 60, timer % 60);
        return List.of(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Time: " + ChatColor.GRAY + timeStr);
    }

    @Override
    public String getPersonalLine(Player p) {
        return null;
    }

}
