package me.timwastaken.minecraftparty.commands;

import me.timwastaken.minecraftparty.managers.GameManager;
import me.timwastaken.minecraftparty.managers.MusicManager;
import me.timwastaken.minecraftparty.models.templates.MusicalMinigame;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MusicCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (GameManager.getActiveMinigame() instanceof MusicalMinigame) {
            sender.sendMessage(ChatColor.RED + "You can't use this command while a musical minigame is running!");
            return true; // return true to prevent usage help
        }
        if (args.length == 0 || (!args[0].equals("play") && args.length > 2)) {
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
            } else if (operation.equals("togglemute")) {
                if (sender instanceof Player p) {
                    MusicManager.togglePlayerMute(p);
                } else
                    sender.sendMessage(ChatColor.RED + "You need to be a player to use this operation");
            } else if (operation.equals("volume")) {
                if (args.length != 2) return false;
                try {
                    byte volume = Byte.parseByte(args[1]);
                    MusicManager.setVolume(volume);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number");
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
            completions.add("play");
            completions.add("toggle");
            completions.add("stop");
            completions.add("togglemute");
            completions.add("volume");
        } else if (args.length >= 2) {
            if (args[0].equals("play")) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    stringBuilder.append(args[i]).append(" ");
                }
                String searchTerm = stringBuilder.toString().trim();
                completions = search(searchTerm, MusicManager.getFileNames());
            }
        }
        return completions;
    }

    private List<String> search(String term, List<String> strings) {
        List<String> hits = new ArrayList<>();
        strings.forEach(str -> {
            if (StringUtil.startsWithIgnoreCase(str, term)) {
                hits.add(str);
            }
        });
        return hits;
    }

}
