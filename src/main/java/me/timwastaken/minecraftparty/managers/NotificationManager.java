package me.timwastaken.minecraftparty.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class NotificationManager {

    public static void announceGameWinners(Player... winners) {
        StringBuilder playerNames = new StringBuilder();
        Arrays.stream(winners).forEach(winner -> playerNames.append(winner.getName()).append(", "));
        String playerString = playerNames.toString().trim();
        playerString = playerString.substring(0, playerString.length() - 1);
        String finalPlayerString = playerString;
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + finalPlayerString, ChatColor.GRAY + "won the game", 10, 80, 10);
        });
    }

    public static void notifyPlayerOut(Player out, String reason) {
        out.playSound(out.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 2f);
        out.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You're out!", ChatColor.GRAY + reason, 10, 80, 10);
    }

    public static void announceStoppingGame(Player... players) {
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, 1f, 1f);
            player.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Stopping Minigame", ChatColor.RED + "Just wait...", 10, 80, 10);
        }
    }

}
