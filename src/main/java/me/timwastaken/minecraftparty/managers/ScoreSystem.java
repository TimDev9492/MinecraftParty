package me.timwastaken.minecraftparty.managers;

import me.timwastaken.minecraftparty.MinecraftParty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class ScoreSystem {

    private static final Map<UUID, Integer> playerScores = new HashMap<>();

    public static Map<UUID, Integer> getSortedScores() {
        return MinecraftParty.sortMap(playerScores);
    }

    public static void addPlayers(Player... players) {
        for (Player player : players) {
            Bukkit.broadcastMessage("Added " + player.getName() + " to the score system");
            playerScores.put(player.getUniqueId(), 0);
        }
    }

    public static void addPlayerScore(Player p, int delta) {
        if (!playerScores.containsKey(p.getUniqueId())) return;
        playerScores.put(p.getUniqueId(), Math.max(playerScores.get(p.getUniqueId()) + delta, 0));
    }

    public static void setPlayerScore(Player p, int score) {
        if (!playerScores.containsKey(p.getUniqueId())) return;
        playerScores.put(p.getUniqueId(), score);
    }

    public static int getPlayerScore(Player p) {
        if (!playerScores.containsKey(p.getUniqueId())) return -1;
        return playerScores.get(p.getUniqueId());
    }

}
