package me.timwastaken.minecraftparty.models.other;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;

public class MinigameUtils {

    private static Random rnd = new Random();

    public static Team getScoreboardTeam(String name) {
        Team team;
        if ((team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name)) != null) {
            team.unregister();
        }
        team = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(name);
        return team;
    }

    public static UUID[] getNewFightingPair(HashMap<UUID, Integer> gamesPlayed) {
        UUID[] playerPair = new UUID[2];
        int minGamesPlayed = -1;
        int secondMin = -1;
        for (Map.Entry<UUID, Integer> entry : gamesPlayed.entrySet()) {
            if (minGamesPlayed == -1) minGamesPlayed = entry.getValue();
            else if (entry.getValue() < minGamesPlayed) {
                minGamesPlayed = entry.getValue();
            } else if (secondMin == -1 || entry.getValue() < secondMin) {
                secondMin = entry.getValue();
            }
        }
        ArrayList<UUID> possiblePlayers = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : gamesPlayed.entrySet()) {
            if (entry.getValue() == minGamesPlayed) possiblePlayers.add(entry.getKey());
        }
        if (possiblePlayers.size() == 1) {
            playerPair[0] = possiblePlayers.get(0);
            possiblePlayers.clear();
            for (Map.Entry<UUID, Integer> entry : gamesPlayed.entrySet()) {
                if (entry.getValue() == secondMin) possiblePlayers.add(entry.getKey());
            }
            playerPair[1] = possiblePlayers.get(rnd.nextInt(possiblePlayers.size()));
        } else {
            int randomIndex = rnd.nextInt(possiblePlayers.size());
            playerPair[0] = possiblePlayers.get(randomIndex);
            possiblePlayers.remove(randomIndex);
            randomIndex = rnd.nextInt(possiblePlayers.size());
            playerPair[1] = possiblePlayers.get(randomIndex);
        }
        return playerPair;
    }

    public static Vector uniformCircleDistribution(Vector center, double radius) {
        double scalarRad = Math.sqrt(Math.random());
        double angle = Math.random() * 2 * Math.PI;
        Vector circleOffset = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius).multiply(scalarRad);
        return center.clone().add(circleOffset);
    }

}
