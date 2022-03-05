package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;

public class MazeRunner extends Minigame implements GameEventListener {

    private static MinigameType type = MinigameType.MAZE_RUNNER;

    private ArrayList<Block> walls;
    private Material wallMaterial;
    private int mazeSize;
    private int gameTimeSeconds;
    private final Vector[] directions;
    private Random rnd;
    private ArrayList<BukkitRunnable> gameLoops;

    private int entranceWall;
    private Block entrance;
    private Block exit;

    public MazeRunner(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_PVP));
        super.addGameEventListeners(this);
        walls = new ArrayList<>();
        directions = new Vector[]{
                new Vector(2, 0, 0),
                new Vector(0, 0, 2),
                new Vector(-2, 0, 0),
                new Vector(0, 0, -2)
        };
        rnd = new Random();
        gameLoops = new ArrayList<>();

        entrance = null;
        exit = null;
    }

    @Override
    public void onGameStart() {

    }

    @Override
    public void onGameEnd() {
        gameLoops.forEach(BukkitRunnable::cancel);
        destroyMaze();
    }

    @Override
    public void onWorldLoaded() {
        wallMaterial = Material.valueOf(getConfig().getString("wall_material"));
        mazeSize = 2 * getConfig().getInt("maze_size");
        gameTimeSeconds = getConfig().getInt("game_time_seconds");
        generateMaze();
    }

    private void generateMaze() {
        // add all blocks as walls
        for (int x = origin.getBlockX() - mazeSize; x <= origin.getBlockX() + mazeSize; x++) {
            for (int z = origin.getBlockZ() - mazeSize; z <= origin.getBlockZ() + mazeSize; z++) {
                Block b = gameWorld.getBlockAt(x, origin.getBlockY(), z);
                walls.add(b);
//                if (isOuterWall(b)) b.setType(Material.GOLD_BLOCK);
            }
        }

        Block start = origin.add(1, 0, 1).getBlock();
        walls.remove(start);

        Set<Block> nextSteps = getNeighbors(start, true);

//        BukkitRunnable runnable = new BukkitRunnable() {
//            @Override
//            public void run() {
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
            if (!isOuterWall(nextStep)) {
                nextSteps.addAll(getNeighbors(nextStep, true));
            } else {
                if (entrance == null) {
                    entrance = nextStep;
                    entranceWall = wallOf(entrance);
                } else if (exit == null && wallOf(nextStep) != entranceWall) {
                    exit = nextStep;
                }
            }
            while (nextSteps.contains(nextStep))
                nextSteps.remove(nextStep);
            nextSteps.forEach(b -> b.setType(Material.NETHERITE_BLOCK));
        }
        constructMaze();
//            }
//        };
//        gameLoops.add(runnable);
//        runnable.runTaskTimer(MinecraftParty.getInstance(), 0L, 20L);
//    }
    }

    private int wallOf(Block b) {
        if (b.getX() - origin.getBlockX() == mazeSize) {
            return 0;
        } else if (b.getX() - origin.getBlockX() == -mazeSize) {
            return 1;
        } else if (b.getZ() - origin.getBlockZ() == mazeSize) {
            return 2;
        } else if (b.getZ() - origin.getBlockZ() == -mazeSize) {
            return 3;
        }
        return -1;
    }

    private void constructMaze() {
        for (int x = origin.getBlockX() - mazeSize; x <= origin.getBlockX() + mazeSize; x++) {
            for (int z = origin.getBlockZ() - mazeSize; z <= origin.getBlockZ() + mazeSize; z++) {
                Block b = gameWorld.getBlockAt(x, origin.getBlockY(), z);
                if (!walls.contains(b)) {
                    b.setType(Material.CALCITE);
                } else if (b.equals(entrance)) {
                    b.setType(Material.NETHERITE_BLOCK);
                } else if (b.equals(exit)) {
                    b.setType(Material.DIAMOND_BLOCK);
                } else {
                    if (isOuterWall(b))
                        b.setType(Material.GOLD_BLOCK);
                }
            }
        }
    }

    private void destroyMaze() {
        for (int x = origin.getBlockX() - mazeSize; x <= origin.getBlockX() + mazeSize; x++) {
            for (int z = origin.getBlockZ() - mazeSize; z <= origin.getBlockZ() + mazeSize; z++) {
                gameWorld.getBlockAt(x, origin.getBlockY(), z).setType(Material.AIR);
            }
        }
    }

    @Override
    public void onPlayerLeave(Player p) {

    }

    @Override
    public void onPlayerJoin(Player p) {

    }

    private Set<Block> getNeighbors(Block from, boolean shoudBeWall) {
//        Set<Block> neighborsOnOuterWall = new HashSet<>();
        Set<Block> neighbors = new HashSet<>();
        for (Vector direction : directions) {
            Block neighbor = from.getLocation().add(direction).getBlock();
            if (insideMaze(neighbor) && shoudBeWall == walls.contains(neighbor)) {
                neighbors.add(neighbor);
//                if (isOuterWall(neighbor)) neighborsOnOuterWall.add(neighbor);
            }
        }
//        while (neighborsOnOuterWall.size() > 1) {
//            Block outerWallBlock = neighborsOnOuterWall.stream().skip(rnd.nextInt(neighborsOnOuterWall.size())).findFirst().orElse(null);
//            neighbors.remove(outerWallBlock);
//            neighborsOnOuterWall.remove(outerWallBlock);
//        }
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
//        one.setType(Material.CALCITE);
        walls.remove(between);
//        between.setType(Material.CALCITE);
        walls.remove(other);
//        other.setType(Material.CALCITE);
    }

}
