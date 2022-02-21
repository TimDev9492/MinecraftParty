package me.timwastaken.minecraftparty.managers;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.Minigame;
import me.timwastaken.minecraftparty.models.MinigameType;
import me.timwastaken.minecraftparty.models.minigames.AnvilStorm;
import me.timwastaken.minecraftparty.models.minigames.JourneyToSalem;
import me.timwastaken.minecraftparty.models.minigames.MlgRush;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameManager {

    private static Minigame activeMinigame;
    private static String defaultWorldName;
    private static World defaultWorld;
    private static Location origin;

    public static void init() {
        defaultWorldName = MinecraftParty.getInstance().getConfig().getString("default_world.name");
        defaultWorld = Bukkit.getWorld(defaultWorldName);
        setOrigin(defaultWorld);
    }

    private static void setOrigin(World world) {
        if (world != null) {
            origin = new Location(world,
                    MinecraftParty.getInstance().getConfig().getInt("default_world.origin.x") + 0.5,
                    MinecraftParty.getInstance().getConfig().getInt("default_world.origin.y"),
                    MinecraftParty.getInstance().getConfig().getInt("default_world.origin.z") + 0.5);
        }
    }

    public static String getDefaultWorldName() {
        return defaultWorldName;
    }

    public static World getDefaultWorld() {
        return defaultWorld;
    }

    public static void setDefaultWorld(World defaultWorld) {
        GameManager.defaultWorld = defaultWorld;
        setOrigin(defaultWorld);
    }

    public static Minigame getActiveMinigame() {
        return activeMinigame;
    }

    public static void loadMinigame(MinigameType type, Player... players) {
        boolean successful = true;
        switch (type) {
            case ANVIL_STORM -> {
                activeMinigame = new AnvilStorm(players);
            }
            case MLG_RUSH -> {
                activeMinigame = new MlgRush(players);
            }
            case JOURNEY_TO_SALEM -> {
                activeMinigame = new JourneyToSalem(players);
            }
            default -> successful = false;
        }
        if (successful) {
            activeMinigame.loadWorld();
            activeMinigame.startCountdown();
        }
    }

    public static boolean isRunning() {
        return activeMinigame.isRunning();
    }

    public static void stopMinigame() {
//        activeMinigame.abort();
        activeMinigame.endGame();
    }

    public static Location getOrigin() {
        return origin;
    }

    public static void clearMinigame() {
        activeMinigame = null;
    }

}
