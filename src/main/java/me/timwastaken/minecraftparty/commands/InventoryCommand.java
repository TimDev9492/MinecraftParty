package me.timwastaken.minecraftparty.commands;

import me.timwastaken.minecraftparty.managers.InvGuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InventoryCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player p) {
            p.openInventory(InvGuiManager.getConfigGui());
        }
        return false;
    }

}
