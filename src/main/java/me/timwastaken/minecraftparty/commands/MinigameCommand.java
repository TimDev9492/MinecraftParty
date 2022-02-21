package me.timwastaken.minecraftparty.commands;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.models.MinigameType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinigameCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args.length > 2) {
            return false;
        } else {
            String operation = args[0];
            if (operation.equals("stop")) {
                GameManager.stopMinigame();
            } else if (operation.equals("load")) {
                MinigameType typeToLoad = MinigameType.fromAlias(args[1]);
                if (typeToLoad == null) {
                    sender.sendMessage(ChatColor.RED + "That minigame type doesn't exist");
                    return false;
                } else {
                    if (GameManager.getActiveMinigame() != null) {
                        GameManager.stopMinigame();
                    }
                    GameManager.loadMinigame(typeToLoad, Bukkit.getOnlinePlayers().toArray(new Player[0]));
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Unsupported operation");
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("load");
            completions.add("stop");
        }
        else if (args.length == 2) {
            if (args[0].equals("load"))
            Arrays.stream(MinigameType.values()).forEach(type -> completions.add(type.toString()));
        }
        return completions;
    }

}
