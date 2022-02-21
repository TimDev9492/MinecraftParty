package me.timwastaken.minecraftparty.commands;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.models.minigames.MlgRush;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            if (GameManager.getActiveMinigame() instanceof MlgRush mlgRushMinigame) {
                if (mlgRushMinigame.saveLayoutToDatabase(p)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    p.sendMessage(ChatColor.GREEN + "Inventory layout saved successfully!");
                } else {
                    p.sendMessage(ChatColor.RED + "Inventory update failed");
                }
                return true;
            }
            p.sendMessage(ChatColor.RED + "No minigame to update your inventory!");
            return false;
        } else
            sender.sendMessage("You need to be a player to execute this command");
        return false;
    }

}
