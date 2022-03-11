package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.other.MinigameUtils;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.awt.Color;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class MazeRunner extends Minigame implements GameEventListener {

    private static MinigameType type = MinigameType.MAZE_RUNNER;

    private ArrayList<Block> walls;
    private Material wallMaterial;
    private int mazeSize;
    private int mazeHeight;
    private int gameTimeSeconds;
    private final Vector[] directions;
    private Random rnd;
    private ArrayList<BukkitRunnable> gameLoops;

    private ArrayList<Player> participants;
    private Team noCollision;

    private ArrayList<Player> completed;
    private HashMap<UUID, Long> timeInMillis;

    private Block entrance;
    private Block exit;

    public MazeRunner(Player... players) throws IOException {
        super(type, List.of(
                MinigameFlag.NO_PVP,
                MinigameFlag.NO_BLOCK_DROPS,
                MinigameFlag.NO_BLOCK_BREAKING,
                MinigameFlag.NO_BLOCK_PLACEMENT
        ));
        super.addGameEventListeners(this);
        walls = new ArrayList<>();
        directions = new Vector[]{
                new Vector(2, 0, 0),
                new Vector(0, 0, 2),
                new Vector(-2, 0, 0),
                new Vector(0, 0, -2)
        };
        rnd = new Random();
        participants = new ArrayList<>(Arrays.asList(players));
        gameLoops = new ArrayList<>();
        completed = new ArrayList<>();
        timeInMillis = new HashMap<>();
        noCollision = MinigameUtils.getScoreboardTeam("mcparty_no_col");
        noCollision.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        participants.forEach(p -> noCollision.addEntry(p.getName()));

        entrance = null;
        exit = null;
    }

    @Override
    public void onGameStart() {
        breakBlock(entrance);
        BukkitRunnable timer = new BukkitRunnable() {
            @Override
            public void run() {
                ScoreboardSystem.refreshScoreboards();
                if (gameTimeSeconds == 0) {
                    gameEnd();
                }
                gameTimeSeconds--;
            }
        };
        gameLoops.add(timer);
        timer.runTaskTimer(MinecraftParty.getInstance(), 0L, 20L);
    }

    private void gameEnd() {
        NotificationManager.announceGameWinners(completed.size() == 0 ? null : completed.get(0));
        endGame();
    }

    public void onPlayerExitMaze(Player p) {
        if (completed.contains(p)) return;
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        Bukkit.getOnlinePlayers().forEach(online -> {
            if (online.getUniqueId() != p.getUniqueId())
                online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
        });
        Bukkit.broadcastMessage(ChatColor.GRAY + "" + ChatColor.BOLD + p.getName() + ChatColor.GREEN + " made it out of the maze");
        completed.add(p);
        timeInMillis.put(p.getUniqueId(), System.currentTimeMillis() - getWhenStarted());
        ScoreboardSystem.refreshScoreboards();
        if (completed.size() == participants.size() || completed.size() == 3) {
            gameEnd();
        }
    }

    @Override
    public void onGameEnd() {
        destroyMaze();
        resetMap();
        participants.forEach(p -> p.removePotionEffect(PotionEffectType.INVISIBILITY));
        noCollision.unregister();
        gameLoops.forEach(BukkitRunnable::cancel);
    }

    @Override
    public void onWorldLoaded() {
        wallMaterial = Material.valueOf(getConfig().getString("wall_material"));
        mazeSize = 2 * getConfig().getInt("maze_size");
        mazeHeight = getConfig().getInt("maze_height");
        gameTimeSeconds = getConfig().getInt("game_time_seconds");
        generateMaze();
        changeBlock(entrance, Material.RED_STAINED_GLASS);

        Location spawn = entrance.getLocation().clone().add(-4.5, 0, 0.5);
        spawn.setYaw(-90);
        Block b = spawn.clone().add(0, -1, 0).getBlock();
        if (b.getType().isAir()) {
            for (int deltaX = 0; deltaX <= 5; deltaX++) {
                Block between = b.getLocation().clone().add(deltaX, 0, 0).getBlock();
                if (between.getType().isAir()) {
                    between.setType(Material.GOLD_BLOCK);
                    addPlacedBlock(between);
                }
            }
        }
        participants.forEach(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            p.teleport(spawn);
        });
        for (int i = 0; i < participants.size(); i++) {
            float percentage = (float) i / participants.size();
            Color hsb = Color.getHSBColor(percentage, 1, 1);
            org.bukkit.Color bootColor = org.bukkit.Color.fromRGB(hsb.getRed(), hsb.getGreen(), hsb.getBlue());
            ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
            LeatherArmorMeta meta = (LeatherArmorMeta) leatherBoots.getItemMeta();
            meta.setColor(bootColor);
            leatherBoots.setItemMeta(meta);
            participants.get(i).getInventory().setItem(EquipmentSlot.FEET, leatherBoots);
        }
    }

    private void generateMaze() {
        // add all blocks as walls
        for (int x = origin.getBlockX() - mazeSize; x <= origin.getBlockX() + mazeSize; x++) {
            for (int z = origin.getBlockZ() - mazeSize; z <= origin.getBlockZ() + mazeSize; z++) {
                Block b = gameWorld.getBlockAt(x, origin.getBlockY(), z);
                walls.add(b);
            }
        }

        int rx = -mazeSize + 2 * rnd.nextInt(mazeSize) + 1;
        int rz = -mazeSize + 2 * rnd.nextInt(mazeSize) + 1;
        Block start = origin.clone().add(rx, 0, rz).getBlock();
        walls.remove(start);

        Set<Block> nextSteps = getNeighbors(start, true);

        while (!nextSteps.isEmpty()) {
            // random next step
            Block nextStep = nextSteps.stream().skip(rnd.nextInt(nextSteps.size())).findFirst().orElse(null);

            // get non-wall neighbors of nextStep
            Set<Block> pathCells = getNeighbors(nextStep, false);
            if (!pathCells.isEmpty()) {
                // connect nextStep with random path cell
                connect(nextStep, pathCells.stream().skip(rnd.nextInt(pathCells.size())).findFirst().orElse(null));
            }

            // add next steps and remove connected cell
            nextSteps.addAll(getNeighbors(nextStep, true));
            nextSteps.remove(nextStep);
        }
        // generate entrance and exit
        int z = -mazeSize + 2 * rnd.nextInt(mazeSize) + 1;
        entrance = gameWorld.getBlockAt(origin.getBlockX() - mazeSize, origin.getBlockY(), origin.getBlockZ() + z);
        z = -mazeSize + 2 * rnd.nextInt(mazeSize) + 1;
        exit = gameWorld.getBlockAt(origin.getBlockX() + mazeSize, origin.getBlockY(), origin.getBlockZ() + z);
        walls.remove(entrance);
        walls.remove(exit);
        constructMaze();
    }

    private void constructMaze() {
        for (int x = origin.getBlockX() - mazeSize; x <= origin.getBlockX() + mazeSize; x++) {
            for (int z = origin.getBlockZ() - mazeSize; z <= origin.getBlockZ() + mazeSize; z++) {
                Block b = gameWorld.getBlockAt(x, origin.getBlockY(), z);
                if (walls.contains(b)) {
                    Leaves leavesData = (Leaves) wallMaterial.createBlockData();
                    leavesData.setPersistent(true);
                    changeBlock(b, wallMaterial, leavesData);
                }
            }
        }
    }

    private void changeBlock(Block b, Material mat, BlockData blockData) {
        for (int i = 0; i < mazeHeight; i++) {
            Block constructing = b.getLocation().clone().add(0, i, 0).getBlock();
            constructing.setType(mat);
            if (blockData != null) {
                constructing.setBlockData(blockData);
            }
        }
    }

    private void changeBlock(Block b, Material mat) {
        changeBlock(b, mat, null);
    }

    private void breakBlock(Block b) {
        for (int i = 0; i < mazeHeight; i++) {
            Block breaking = b.getLocation().clone().add(0, i, 0).getBlock();
            breaking.setType(Material.AIR);
            participants.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1f));
        }
    }

    private void destroyMaze() {
        for (int x = origin.getBlockX() - mazeSize; x <= origin.getBlockX() + mazeSize; x++) {
            for (int z = origin.getBlockZ() - mazeSize; z <= origin.getBlockZ() + mazeSize; z++) {
                changeBlock(gameWorld.getBlockAt(x, origin.getBlockY(), z), Material.AIR);
            }
        }
    }

    @Override
    public void onPlayerLeave(Player p) {
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.getInventory().clear();
        noCollision.removeEntry(p.getName());
        ScoreboardSystem.removePlayerScoreboards(p);
    }

    @Override
    public void onPlayerJoin(Player p) {
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(origin.clone().add(0.5, 20, 0.5));
        p.setHealth(20);
    }

    private Set<Block> getNeighbors(Block from, boolean shoudBeWall) {
//        Set<Block> neighborsOnOuterWall = new HashSet<>();
        Set<Block> neighbors = new HashSet<>();
        for (Vector direction : directions) {
            Block neighbor = from.getLocation().add(direction).getBlock();
            if (insideMaze(neighbor) && shoudBeWall == walls.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    private boolean insideMaze(Block b) {
        return b.getX() >= origin.getBlockX() - mazeSize && b.getX() <= origin.getBlockX() + mazeSize
                && b.getZ() >= origin.getBlockZ() - mazeSize && b.getZ() <= origin.getBlockZ() + mazeSize;
    }

    private boolean isOuterWall(Block b) {
        return b.getX() == origin.getBlockX() - mazeSize || b.getX() == origin.getBlockX() + mazeSize
                || b.getZ() == origin.getBlockZ() - mazeSize || b.getZ() == origin.getBlockZ() + mazeSize;
    }

    private void connect(Block one, Block other) {
        Block between = one.getLocation().clone().add(other.getLocation().clone()).toVector().multiply(0.5f).toLocation(gameWorld).getBlock();
        walls.remove(one);
        walls.remove(between);
        walls.remove(other);
    }

    @Override
    public List<String> getScoreboardList() {
        List<String> scoreboardList = new ArrayList<>();
        if (isRunning()) {
            scoreboardList.add(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Time remaining: " + ChatColor.GRAY + String.format("%02d:%02d", gameTimeSeconds / 60, gameTimeSeconds % 60));
        }
        for (int i = 1; i <= 3; i++) {
            scoreboardList.add(ChatColor.GREEN + "" + ChatColor.BOLD + i + ". " + ChatColor.GRAY + (completed.size() >= i ? completed.get(i - 1).getName() + ChatColor.DARK_GRAY + String.format(" %.3fs", timeInMillis.get(completed.get(i - 1).getUniqueId()) / 1000f) : "-"));
        }
        return scoreboardList;
    }

    public Block getExitBlock() {
        return exit;
    }

}
