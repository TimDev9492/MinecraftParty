package me.timwastaken.minecraftparty.models.minigames.redlightgreenlight;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;

public class RedLightGreenLight extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.RED_LIGHT_GREEN_LIGHT;

    private Random rnd;
    private ArrayList<Player> participants;
    private Set<UUID> winners;
    private Set<Player> vulnerable;
    private ItemStack stone;

    private Vector[] glassWall;
    private Vector[] fieldArea;
    private double stoneDensity;
    private int finishLineX;

    private BlockFace[] stoneDirections;

    private int[] greenTicks;
    private int[] yellowTicks;
    private int[] redTicks;

    boolean isRedLight = false;

    public RedLightGreenLight(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.ZERO_DAMAGE, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT));
        super.addGameEventListeners(this);
        rnd = new Random();
        participants = new ArrayList<>(Arrays.asList(players));
        winners = new HashSet<>();
        vulnerable = new HashSet<>(participants);

        stoneDirections = new BlockFace[]{
                BlockFace.NORTH,
                BlockFace.SOUTH,
                BlockFace.EAST,
                BlockFace.WEST
        };

        stone = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = stone.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.ITALIC + "Stone");
        stone.setItemMeta(meta);
    }

    private void startCycle() {
        setGreenLight();
        addTask(new BukkitRunnable() {
            @Override
            public void run() {
                setYellowLight();
                addTask(new BukkitRunnable() {
                    @Override
                    public void run() {
                        setRedLight();
                        addTask(new BukkitRunnable() {
                            @Override
                            public void run() {
                                startCycle();
                            }
                        }, redTicks[0] + rnd.nextInt(redTicks[1] - redTicks[0] + 1));
                    }
                }, yellowTicks[0] + rnd.nextInt(yellowTicks[1] - yellowTicks[0] + 1));
            }
        }, greenTicks[0] + rnd.nextInt(greenTicks[1] - greenTicks[0] + 1));
    }

    @Override
    public void onGameStart() {
        for (int x = glassWall[0].getBlockX(); x <= glassWall[1].getBlockX(); x++) {
            for (int y = glassWall[0].getBlockY(); y <= glassWall[1].getBlockY(); y++) {
                for (int z = glassWall[0].getBlockZ(); z <= glassWall[1].getBlockZ(); z++) {
                    Block b = gameWorld.getBlockAt(x, y, z);
                    if (b.getType() == Material.GLASS) b.setType(Material.AIR);
                }
            }
        }
        startCycle();
    }

    private void setRedLight() {
        isRedLight = true;
        NotificationManager.announceRedLight(gameWorld.getPlayers().toArray(new Player[0]));
    }

    private void setYellowLight() {
        NotificationManager.announceYellowLight(gameWorld.getPlayers().toArray(new Player[0]));
    }

    private void setGreenLight() {
        isRedLight = false;
        NotificationManager.announceGreenLight(gameWorld.getPlayers().toArray(new Player[0]));
    }

    private void finishPlayer(Player p) {
        vulnerable.remove(p);
        winners.add(p.getUniqueId());
        gameWorld.getPlayers().forEach(inWorld -> {
            inWorld.sendMessage(ChatColor.GRAY + "" + ChatColor.BOLD + p.getName() + ChatColor.GREEN + " reached the finish line");
            inWorld.playSound(inWorld.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        });

        checkGameEnd();
    }

    private void checkGameEnd() {
        if (participants.size() - winners.size() <= 1) {
            NotificationManager.announceGameWinners(Bukkit.getPlayer(winners.iterator().next()));
            endGame();
        }
    }

    @Override
    public void onGameEnd() {
        resetMap();
    }

    @Override
    public void onWorldLoaded() {
        gameWorld.setAutoSave(false);
        glassWall = new Vector[]{
                new Vector(
                        getConfig().getInt("glass_wall.x1"),
                        getConfig().getInt("glass_wall.y1"),
                        getConfig().getInt("glass_wall.z1")
                ),
                new Vector(
                        getConfig().getInt("glass_wall.x2"),
                        getConfig().getInt("glass_wall.y2"),
                        getConfig().getInt("glass_wall.z2")
                )
        };
        fieldArea = new Vector[]{
                new Vector(
                        getConfig().getInt("field_area.x1"),
                        getConfig().getInt("field_area.y1"),
                        getConfig().getInt("field_area.z1")
                ),
                new Vector(
                        getConfig().getInt("field_area.x2"),
                        getConfig().getInt("field_area.y2"),
                        getConfig().getInt("field_area.z2")
                )
        };
        stoneDensity = getConfig().getDouble("stone_density");
        finishLineX = getConfig().getInt("finish_line_x");
        greenTicks = new int[]{getConfig().getInt("green_phase.min"), getConfig().getInt("green_phase.max")};
        yellowTicks = new int[]{getConfig().getInt("yellow_phase.min"), getConfig().getInt("yellow_phase.max")};
        redTicks = new int[]{getConfig().getInt("red_phase.min"), getConfig().getInt("red_phase.max")};
        int yaw = getConfig().getInt("origin.yaw");
        Location spawn = origin.clone();
        spawn.setYaw(yaw);
        participants.forEach(p -> {
            p.teleport(spawn);
        });

        generateThrowableStones();
    }

    private void generateThrowableStones() {
        for (int x = fieldArea[0].getBlockX(); x <= fieldArea[1].getBlockX(); x++) {
            for (int y = fieldArea[0].getBlockY(); y <= fieldArea[1].getBlockY(); y++) {
                for (int z = fieldArea[0].getBlockZ(); z <= fieldArea[1].getBlockZ(); z++) {
                    Block b = gameWorld.getBlockAt(x, y, z);
                    if (rnd.nextDouble() < stoneDensity && b.getType().isAir()) {
                        placeThrowableStone(b);
                    }
                }
            }
        }
    }

    private void placeThrowableStone(Block b) {
        b.setType(Material.STONE_BUTTON);
        FaceAttachable faceAttachable = (FaceAttachable) b.getBlockData();
        faceAttachable.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
        b.setBlockData(faceAttachable);
        Directional directional = (Directional) b.getBlockData();
        directional.setFacing(stoneDirections[rnd.nextInt(stoneDirections.length)]);
        b.setBlockData(directional);
        addPlacedBlock(b);
    }

    @Override
    public void onPlayerLeave(Player p) {
        participants.remove(p);
        vulnerable.remove(p);
        winners.remove(p.getUniqueId());
        p.getInventory().clear();
        ScoreboardSystem.refreshScoreboards();
    }

    @Override
    public void onPlayerJoin(Player p) {
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(origin);
    }

    public void onPlayerMove(PlayerMoveEvent event) {
        if (winners.contains(event.getPlayer().getUniqueId())) {
            if (event.getTo().getX() < finishLineX) {
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_CAT_HISS, 1f, 1f);
                event.getPlayer().setVelocity(new Vector(0.6, 0.6, event.getPlayer().getVelocity().getZ()));
            }
        } else if (event.getTo().getX() > finishLineX) {
            finishPlayer(event.getPlayer());
        }
        if (isRedLight && vulnerable.contains(event.getPlayer())) {
            Location from = event.getFrom().clone();
            Location to = event.getTo().clone();
            from.setY(0);
            to.setY(0);
            double dist = from.distanceSquared(to);
            if (dist > 0) {
                vulnerable.remove(event.getPlayer());
                double progress = from.getX() - origin.getX();
                event.getPlayer().setVelocity(new Vector(-progress * 0.05, 1, event.getPlayer().getVelocity().getZ()));
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
                final Player flying = event.getPlayer();
                addGameLoop(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (flying.isOnGround()) {
                            flying.setVelocity(new Vector(0, 0, 0));
                            vulnerable.add(flying);
                            this.cancel();
                        }
                    }
                }, 1L, 1L);
            }
        }
    }

    public void onPlayerPickupStone(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.getClickedBlock().setType(Material.AIR);
        event.getPlayer().getInventory().addItem(stone);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
    }

    public void onPlayerThrowStone(PlayerDropItemEvent event) {
        final Item stoneItem = event.getItemDrop();
        if (!isSpawnedEntity(stoneItem)) addSpawnedEntity(stoneItem);
        stoneItem.setMetadata("thrownBy", new FixedMetadataValue(MinecraftParty.getInstance(), event.getPlayer().getUniqueId().toString()));
        stoneItem.setVelocity(event.getPlayer().getEyeLocation().getDirection().multiply(2));
        stoneItem.setPickupDelay(1);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);
        event.getPlayer().setCooldown(Material.STONE_BUTTON, 60);
        addGameLoop(new BukkitRunnable() {
            @Override
            public void run() {
                if (stoneItem.isOnGround()) {
                    stoneItem.remove();
                    if (stoneItem.getLocation().getBlock().getType().isAir()) {
                        placeThrowableStone(stoneItem.getLocation().getBlock());
                        this.cancel();
                    }
                }
            }
        }, 0L, 1L);
    }

    public void onPlayerPickupStone(EntityPickupItemEvent event) {
        if (event.getItem().hasMetadata("thrownBy")) {
            Player thrower = Bukkit.getPlayer(UUID.fromString(event.getItem().getMetadata("thrownBy").get(0).asString()));
            Player p = (Player) event.getEntity();

            if (thrower == null)
                p.setVelocity(p.getVelocity().add(new Vector(1, 0, 0)));
            else
                thrower.attack(p);
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

}
