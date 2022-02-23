package me.timwastaken.minecraftparty.listeners;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.minigames.AnvilStorm;
import me.timwastaken.minecraftparty.models.minigames.Lasertag;
import me.timwastaken.minecraftparty.models.minigames.MusicalChairs;
import me.timwastaken.minecraftparty.models.minigames.MlgRush;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_BLOCK_PLACEMENT)) event.setCancelled(true);
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (event.getBlockPlaced().getLocation().getBlockY() > mlgRushMinigame.getBuildHeight() || mlgRushMinigame.isNearOwnBed(event.getPlayer(), event.getBlock())) {
                event.setCancelled(true);
            } else {
                mlgRushMinigame.addPlacedBlock(event.getBlockPlaced());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame().hasFlag(MinigameFlag.NO_BLOCK_BREAKING)) event.setCancelled(true);
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (event.getBlock().getType() == mlgRushMinigame.getBedMaterial()) {
                event.setCancelled(true);
                mlgRushMinigame.bedBroken(event.getPlayer(), event.getBlock());
            } else if (!mlgRushMinigame.isPlacedBlock(event.getBlock())) {
                event.setCancelled(true);
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
                mlgRushMinigame.teleportBack(event.getPlayer(), true);
            }
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
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame && event.getEntity() instanceof Player p) {
            mlgRushMinigame.onPlayerPickupItem(event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (GameManager.getActiveMinigame() == null) return;
        ((GameEventListener) GameManager.getActiveMinigame()).onPlayerLeave(event.getPlayer());
    }

}