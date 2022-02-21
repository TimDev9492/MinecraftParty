package me.timwastaken.minecraftparty.listeners;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.models.GameEventListener;
import me.timwastaken.minecraftparty.models.minigames.AnvilStorm;
import me.timwastaken.minecraftparty.models.minigames.JourneyToSalem;
import me.timwastaken.minecraftparty.models.minigames.MlgRush;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    }

    @EventHandler
    public void onPlayerTakeDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && GameManager.getActiveMinigame() instanceof MlgRush) {
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (event.getBlockPlaced().getLocation().getBlockY() > mlgRushMinigame.getBuildHeight() || mlgRushMinigame.isNearOwnBed(event.getPlayer(), event.getBlock())) {
                event.setCancelled(true);
            } else {
                mlgRushMinigame.addPlacedBlock(event.getBlockPlaced());
            }
        }
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
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
    public void onPlayerClickBed(PlayerBedEnterEvent event) {
        if (GameManager.getActiveMinigame() instanceof MlgRush) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
            if (mlgRushMinigame.isFighting(event.getPlayer()) && event.getTo().getBlockY() <= mlgRushMinigame.getDeathY()) {
                mlgRushMinigame.teleportBack(event.getPlayer(), true);
            }
        }
    }

    @EventHandler
    public void onAnvilHitPlayer(EntityDamageByEntityEvent event) {
        if (GameManager.getActiveMinigame() instanceof AnvilStorm anvilStormMinigame) {
            if (event.getEntity() instanceof Player p && event.getDamager() instanceof FallingBlock fallingBlock) {
                if (fallingBlock.getBlockData().getMaterial() == Material.ANVIL) {
                    event.setCancelled(true);
                    anvilStormMinigame.onAnvilHitPlayer(p);
                }
            }
        }
    }

    @EventHandler
    public void onAnvilLanded(EntityChangeBlockEvent event) {
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
    public void onPlayerEnterMinecart(VehicleEnterEvent event) {
        if (GameManager.getActiveMinigame() instanceof JourneyToSalem journeyToSalemMinigame && event.getEntered() instanceof Player p && event.getVehicle() instanceof Minecart) {
            journeyToSalemMinigame.onPlayerEnterMinecart(p);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame && event.getEntity() instanceof Player p) {
            mlgRushMinigame.onPlayerPickupItem(event);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (GameManager.getActiveMinigame() != null)
            ((GameEventListener) GameManager.getActiveMinigame()).onPlayerLeave(event.getPlayer());
    }

}