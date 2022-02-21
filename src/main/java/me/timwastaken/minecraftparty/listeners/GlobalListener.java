package me.timwastaken.minecraftparty.listeners;

import me.timwastaken.minecraftparty.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class GlobalListener implements Listener {

    @EventHandler
    public void onWorldLoaded(WorldLoadEvent event) {
        if (event.getWorld().getName().equals(GameManager.getDefaultWorldName())) {
            GameManager.setDefaultWorld(event.getWorld());
        }
    }

    @EventHandler
    public void onPlayerLoseHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (event.getFoodLevel() <= 20) {
                event.setCancelled(true);
                p.setFoodLevel(25);
            }
        }
    }

}
