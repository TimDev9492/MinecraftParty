package me.timwastaken.minecraftparty.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class NotificationManager {

    public static void announceGameWinner(Player winner) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + winner.getName(), ChatColor.GRAY + "won the game", 10, 80, 10);
        });
    }

    public static void notifyPlayerOut(Player out, String reason) {
        out.playSound(out.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 2f);
        out.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You're out!", ChatColor.GRAY + reason, 10, 80, 10);
    }

}
