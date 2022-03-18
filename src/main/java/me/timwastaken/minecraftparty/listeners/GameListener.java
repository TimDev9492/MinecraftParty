package me.timwastaken.minecraftparty.listeners;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.minigames.AnvilStorm;
import me.timwastaken.minecraftparty.models.minigames.Duels;
import me.timwastaken.minecraftparty.models.minigames.Lasertag;
import me.timwastaken.minecraftparty.models.minigames.MazeRunner;
import me.timwastaken.minecraftparty.models.minigames.MlgRush;
import me.timwastaken.minecraftparty.models.minigames.MusicalChairs;
import me.timwastaken.minecraftparty.models.minigames.OneInTheChamber;
import me.timwastaken.minecraftparty.models.minigames.dragonescape.DragonEscape;
import me.timwastaken.minecraftparty.models.minigames.hotpotato.HotPotato;
import me.timwastaken.minecraftparty.models.minigames.kingofthehill.KingOfTheHill;
import me.timwastaken.minecraftparty.models.minigames.redlightgreenlight.RedLightGreenLight;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;

public class GameListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (GameManager.getActiveMinigame() != null) {
            if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_BLOCK_INTERACTION) && event.getClickedBlock() != null) event.setCancelled(true);
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (GameManager.getActiveMinigame() instanceof Lasertag lasertagMinigame && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getItem() != null && event.getItem().getType() == lasertagMinigame.getGunMaterial()) {
                lasertagMinigame.onPlayerShootGun(event.getPlayer());
            }
        } else if (GameManager.getActiveMinigame() instanceof RedLightGreenLight redLightGreenLightMinigame) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock().getType() == Material.STONE_BUTTON) {
                    redLightGreenLightMinigame.onPlayerPickupStone(event);
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getItem() != null) {
                if (event.getItem().getType() == Material.STONE_BUTTON && event.getPlayer().getCooldown(Material.STONE_BUTTON) == 0) {
                    event.getPlayer().dropItem(false);
                    event.getPlayer().updateInventory();
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_DAMAGE)) event.setCancelled(true);
        else if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.ZERO_DAMAGE)) event.setDamage(0);
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_FALL_DAMAGE)) event.setCancelled(true);
            else if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.ZERO_FALL_DAMAGE)) event.setDamage(0);
        }
        if (GameManager.getActiveMinigame() instanceof Duels duelsMinigame && event.getEntity() instanceof Player p) {
            boolean isPlayerCause = false;
            if (event instanceof EntityDamageByEntityEvent enByEnEv) {
                if (enByEnEv.getDamager() instanceof Player) {
                    isPlayerCause = true;
                }
            }
            if (!isPlayerCause && p.getHealth() - event.getFinalDamage() <= 0) {
                event.setDamage(0);
                duelsMinigame.onPlayerKill(null, p);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_BLOCK_PLACEMENT)) event.setCancelled(true);
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (event.getBlockPlaced().getLocation().getBlockY() > mlgRushMinigame.getBuildHeight() || mlgRushMinigame.isNearOwnBed(event.getPlayer(), event.getBlock())) {
                event.setCancelled(true);
            }
        } else if (GameManager.getActiveMinigame() instanceof Duels duelsMinigame) {
            if (event.getBlockPlaced().getLocation().getBlockY() > duelsMinigame.getBuildHeight()) {
                event.setCancelled(true);
            }
        } else if (GameManager.getActiveMinigame() instanceof DragonEscape dragonEscapeMinigame) {
            if (event.getBlockPlaced().getType() == Material.NETHERITE_BLOCK) {
                event.setCancelled(true);
                dragonEscapeMinigame.placeRandomModule(event.getBlockPlaced().getLocation());
            }
        }
        if (!event.isCancelled()) GameManager.getActiveMinigame().addPlacedBlock(event.getBlockPlaced());
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        GameManager.getActiveMinigame().addPlacedBlock(event.getBlock());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        GameManager.getActiveMinigame().addPlacedBlock(event.getToBlock());
    }

    @EventHandler
    public void onPlayerBucketEmptly(PlayerBucketEmptyEvent event) {
        GameManager.getActiveMinigame().addPlacedBlock(event.getBlock());
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        GameManager.getActiveMinigame().removePlacedBlock(event.getBlock());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_BLOCK_BREAKING)) event.setCancelled(true);
        else if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_BLOCK_DROPS)) event.setDropItems(false);
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (event.getBlock().getType() == mlgRushMinigame.getBedMaterial()) {
                event.setCancelled(true);
                mlgRushMinigame.bedBroken(event.getPlayer(), event.getBlock());
            }
        }
        if (!event.isCancelled()) {
            if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_MAP_BREAKING) && !GameManager.getActiveMinigame().isPlacedBlock(event.getBlock())) {
                event.setCancelled(true);
            } else {
                GameManager.getActiveMinigame().removePlacedBlock(event.getBlock());
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame() instanceof MlgRush) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.FREEZE_PLAYERS_UNTIL_START) && !GameManager.getActiveMinigame().hasStarted()) {
            Location from = event.getFrom().clone();
            Location to = event.getTo().clone();
            from.setY(0);
            to.setY(0);
            if (from.distance(to) != 0) event.setCancelled(true);
        }
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (mlgRushMinigame.isFighting(event.getPlayer()) && event.getTo().getBlockY() <= mlgRushMinigame.getDeathY()) {
                mlgRushMinigame.teleportBack(event.getPlayer());
            }
        } else if (GameManager.getActiveMinigame() instanceof MazeRunner mazeRunnerMinigame) {
            Location playerLoc = event.getTo().clone();
            Location exitBlock = mazeRunnerMinigame.getExitBlock().getLocation().clone().add(0.5, 0, 0.5);
            playerLoc.setY(0);
            exitBlock.setY(0);
            if (playerLoc.distance(exitBlock) <= 0.5)
                mazeRunnerMinigame.onPlayerExitMaze(event.getPlayer());
        } else if (GameManager.getActiveMinigame() instanceof KingOfTheHill kingOfTheHillMinigame) {
            kingOfTheHillMinigame.onPlayerMove(event.getPlayer());
        } else if (GameManager.getActiveMinigame() instanceof RedLightGreenLight redLightGreenLightMinigame && redLightGreenLightMinigame.hasStarted()) {
            redLightGreenLightMinigame.onPlayerMove(event);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (event.getDamager() instanceof Player) {
            if (event.getEntity() instanceof Player) {
                if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_PVP)) event.setCancelled(true);
                else if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.ZERO_PVP_DAMAGE)) event.setDamage(0);
            } else {
                if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_PVE)) event.setCancelled(true);
            }
        }

        if (GameManager.getActiveMinigame() instanceof AnvilStorm anvilStormMinigame) {
            if (event.getEntity() instanceof Player p && event.getDamager() instanceof FallingBlock fallingBlock) {
                if (fallingBlock.getBlockData().getMaterial() == Material.ANVIL) {
                    event.setCancelled(true);
                    anvilStormMinigame.onAnvilHitPlayer(p);
                }
            }
        } else if (GameManager.getActiveMinigame() instanceof Lasertag lasertagMinigame && event.getEntity() instanceof Player p && event.getDamager() instanceof Player damager) {
            if (!lasertagMinigame.isRunning()) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(lasertagMinigame.getHitDamage());
            if (p.getHealth() - lasertagMinigame.getHitDamage() <= 0) {
                event.setDamage(0);
                lasertagMinigame.onPlayerDeath(p);
                lasertagMinigame.givePointToPlayer(damager);
            }
        } else if (GameManager.getActiveMinigame() instanceof OneInTheChamber oneInTheChamberMinigame && event.getEntity() instanceof Player p && event.getDamager() instanceof Player damager) {
            if (!oneInTheChamberMinigame.isRunning()) {
                event.setCancelled(true);
                return;
            }
            if (p.getHealth() - event.getFinalDamage() <= 0) {
                event.setDamage(0);
                oneInTheChamberMinigame.onPlayerDeath(p);
                oneInTheChamberMinigame.givePointToPlayer(damager);
            }
        } else if (GameManager.getActiveMinigame() instanceof Duels duelsMinigame && event.getEntity() instanceof Player p && event.getDamager() instanceof Player damager) {
            if (p.getHealth() - event.getFinalDamage() <= 0) {
                event.setDamage(0);
                duelsMinigame.onPlayerKill(damager, p);
            }
        } else if (GameManager.getActiveMinigame() instanceof HotPotato hotPotatoMinigame && event.getEntity() instanceof Player takeHit && event.getDamager() instanceof Player hit) {
            hotPotatoMinigame.onPlayerHitPlayer(hit, takeHit);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame() instanceof AnvilStorm) {
            if (Arrays.asList(
                    Material.ANVIL,
                    Material.CHIPPED_ANVIL,
                    Material.DAMAGED_ANVIL
            ).contains(event.getTo())) {
                event.setCancelled(true);
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.playSound(event.getBlock().getLocation(), Sound.BLOCK_ANVIL_LAND, 0.2f, 1f);
                });
            }
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame() instanceof MusicalChairs musicalChairsMinigame && event.getEntered() instanceof Player p && event.getVehicle() instanceof Minecart) {
            musicalChairsMinigame.onPlayerEnterMinecart(p);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        ((GameEventListener) GameManager.getActiveMinigame()).onPlayerLeave(event.getPlayer());
        ScoreboardSystem.removePlayerScoreboards(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (GameManager.getActiveMinigame() != null)
            ((GameEventListener) GameManager.getActiveMinigame()).onPlayerJoin(event.getPlayer());
        ScoreboardSystem.refreshScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitBlock() != null && event.getEntity() instanceof Arrow arrow) arrow.remove();
        if (GameManager.getActiveMinigame() instanceof OneInTheChamber oneInTheChamberMinigame && event.getHitEntity() instanceof Player hitPlayer && event.getEntity().getShooter() instanceof Player shootingPlayer) {
            event.setCancelled(true);
            if (!hitPlayer.equals(shootingPlayer)) {
                oneInTheChamberMinigame.onPlayerDeath(hitPlayer);
                oneInTheChamberMinigame.givePointToPlayer(shootingPlayer);
                oneInTheChamberMinigame.cloneArrow(event.getEntity());
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (GameManager.getActiveMinigame() instanceof OneInTheChamber oneInTheChamberMinigame && event.getEntity() instanceof Arrow arrow) {
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (GameManager.getActiveMinigame() instanceof DragonEscape dragonEscapeMinigame && (event.getChunk().getX() < -2 || event.getChunk().getX() > 2) && event.getWorld().getName().equals(dragonEscapeMinigame.getWorldName())) {
            dragonEscapeMinigame.onChunkGenerate(event);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_INVENTORY_CHANGE)) event.setCancelled(true);
        if (GameManager.getActiveMinigame() instanceof RedLightGreenLight redLightGreenLightMinigame && event.getItemDrop().getItemStack().getType() == Material.STONE_BUTTON) {
            redLightGreenLightMinigame.onPlayerThrowStone(event);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (GameManager.getActiveMinigame() instanceof RedLightGreenLight redLightGreenLightMinigame && event.getEntity() instanceof Player) {
            if (event.getItem().getItemStack().getType() == Material.STONE_BUTTON) redLightGreenLightMinigame.onPlayerPickupStone(event);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (GameManager.getActiveMinigame() != null) GameManager.getActiveMinigame().addSpawnedEntity(event.getEntity());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_INVENTORY_CHANGE)) event.setCancelled(true);
    }

}