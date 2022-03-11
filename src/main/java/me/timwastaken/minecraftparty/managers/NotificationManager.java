package me.timwastaken.minecraftparty.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;

public class NotificationManager {

    public static void announceGameWinners(Player... winners) {
        String finalPlayerString = "Nobody";
        if (winners != null && winners.length > 0 && winners[0] != null) {
            StringBuilder playerNames = new StringBuilder();
            Arrays.stream(winners).forEach(winner -> playerNames.append(winner.getName()).append(", "));
            String playerString = playerNames.toString().trim();
            playerString = playerString.substring(0, playerString.length() - 1);
            finalPlayerString = playerString;
        }
        String finalPlayerString1 = finalPlayerString;
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + finalPlayerString1, ChatColor.GRAY + "won the game", 10, 80, 10);
        });
    }

    public static void notifyPlayerOut(Player out, String reason) {
        out.playSound(out.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 2f);
        out.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You're out!", ChatColor.GRAY + reason, 10, 80, 10);
    }

    public static void announceStoppingGame(Player... players) {
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, 1f, 1f);
            player.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Stopped Minigame", ChatColor.RED + "Game got killed", 10, 80, 10);
        }
    }

    public static void announceRedLight(Player... players) {
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 0.5f);
            player.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Red Light", "", 0, 40, 20);
        }
    }

    public static void announceYellowLight(Player... players) {
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
            player.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + "Turning Around...", "", 0, 40, 20);
        }
    }

    public static void announceGreenLight(Player... players) {
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 2f);
            player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Green Light", "", 0, 40, 20);
        }
    }

}
