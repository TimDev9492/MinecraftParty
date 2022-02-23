package me.timwastaken.minecraftparty.commands;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.managers.MusicManager;
import me.timwastaken.minecraftparty.models.templates.MusicalMinigame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class MusicCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (GameManager.getActiveMinigame() instanceof MusicalMinigame) {
            sender.sendMessage(ChatColor.RED + "You can't use this command while a musical minigame is running!");
            return true; // return true to prevent usage help
        }
        if (args.length == 0 ||(!args[0].equals("play") && args.length > 2)) {
            return false;
        } else {
            String operation = args[0];
            if (operation.equals("stop")) {
                MusicManager.stopMusic();
            } else if (operation.equals("play")) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    stringBuilder.append(args[i]).append(" ");
                }
                String fileNameToLoad = stringBuilder.toString().trim();
                if (!MusicManager.getFileNames().contains(fileNameToLoad)) {
                    sender.sendMessage(ChatColor.RED + "That song doesn't exist");
                    return false;
                } else {
                    MusicManager.playSong(fileNameToLoad);
                }
            } else if (operation.equals("toggle")) {
                if (!MusicManager.hasSongLoaded()) {
                    sender.sendMessage(ChatColor.RED + "No song is currently playing");
                    return false;
                } else {
                    MusicManager.toggleMusic();
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
        List<String> completions = null;
        if (args.length == 1) {
            completions = new ArrayList<>();
            completions.add("play");
            completions.add("toggle");
            completions.add("stop");
        }
        else if (args.length == 2) {
            if (args[0].equals("play"))
                completions = MusicManager.getFileNames();
        }
        return completions;
    }

}
