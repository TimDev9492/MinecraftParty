package me.timwastaken.minecraftparty.managers;

import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class ScoreboardSystem {

    private static HashMap<UUID, Scoreboard> playerScoreboards;
    private static final int MAX_LINES = 15;

    public static void init() {
        playerScoreboards = new HashMap<>();
    }

    public static void addPlayerScoreboard(Player... players) {
        Arrays.stream(players).forEach(p -> addPlayerScoreboard(p.getUniqueId()));
    }

    private static void addPlayerScoreboard(UUID uuid) {
        playerScoreboards.put(uuid, Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public static void refreshScoreboard(Player p) {
        p.setScoreboard(getUpdatedScoreboard(p.getUniqueId()));
    }

    private static Scoreboard getUpdatedScoreboard(UUID uuid) {
        if (!playerScoreboards.containsKey(uuid)) {
            addPlayerScoreboard(uuid);
        }
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return null;
        Scoreboard playerScoreboard = playerScoreboards.get(uuid);
        Objective sideboard = playerScoreboard.getObjective("mcp_sideboard");
        if (sideboard != null) sideboard.unregister();
        sideboard = playerScoreboard.registerNewObjective("mcp_sideboard", "dummy", ChatColor.YELLOW + "" + ChatColor.BOLD + "Minecraft Party");
        sideboard.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score = MAX_LINES;
        sideboard.getScore("").setScore(score--);
        sideboard.getScore(ChatColor.GRAY + "Minigame").setScore(score--);
        Minigame activeMinigame = GameManager.getActiveMinigame();
        sideboard.getScore(activeMinigame == null ? ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "None" : activeMinigame.getDisplayName()).setScore(score--);
        if (activeMinigame == null) {
            sideboard.getScore(" ").setScore(score--);
            for (Map.Entry<UUID, Integer> scoreEntry : ScoreSystem.getSortedScores().entrySet()) {
                if (score == 2) break;
                Player current = Bukkit.getPlayer(scoreEntry.getKey());
                if (current == null) continue;
                sideboard.getScore(ChatColor.GOLD + "" + ChatColor.BOLD + scoreEntry.getValue() + " " + ChatColor.RESET + ChatColor.GRAY + current.getName()).setScore(score--);
            }
            sideboard.getScore("  ").setScore(score--);
            sideboard.getScore(ChatColor.YELLOW + "" + ChatColor.ITALIC + "Your score: " + ChatColor.RESET + ScoreSystem.getPlayerScore(p)).setScore(score--);
        } else {
            List<String> text = activeMinigame.getScoreboardList();
            if (text != null) {
                sideboard.getScore(" ").setScore(score--);
                for (String line : activeMinigame.getScoreboardList()) {
                    if (score == 2) break;
                    sideboard.getScore(line).setScore(score--);
                }
                String personalLine = activeMinigame.getPersonalLine(p);
                if (personalLine != null) {
                    sideboard.getScore("  ").setScore(score--);
                    sideboard.getScore(activeMinigame.getPersonalLine(p)).setScore(score--);
                }
            }
        }
        return getPlayerScoreboard(p);
    }

    public static Scoreboard getPlayerScoreboard(Player p) {
        return playerScoreboards.get(p.getUniqueId());
    }

    public static void refreshScoreboards() {
        playerScoreboards.keySet().forEach(id -> refreshScoreboard(Bukkit.getPlayer(id)));
    }

}
