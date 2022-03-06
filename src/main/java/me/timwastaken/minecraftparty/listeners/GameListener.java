package me.timwastaken.minecraftparty.listeners;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.minigames.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.Arrays;

public class GameListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (GameManager.getActiveMinigame() instanceof Lasertag lasertagMinigame && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getItem() != null && event.getItem().getType() == lasertagMinigame.getGunMaterial()) {
                lasertagMinigame.onPlayerShootGun(event.getPlayer());
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
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (mlgRushMinigame.isFighting(event.getPlayer()) && event.getTo().getBlockY() <= mlgRushMinigame.getDeathY()) {
                mlgRushMinigame.teleportBack(event.getPlayer());
            }
        } else if (GameManager.getActiveMinigame() instanceof MazeRunner mazeRunnerMinigame) {
            if (event.getPlayer().getLocation().distance(mazeRunnerMinigame.getExitBlock().getLocation().clone().add(0.5, 0, 0.5)) <= 0.5)
                mazeRunnerMinigame.onPlayerExitMaze(event.getPlayer());
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

}