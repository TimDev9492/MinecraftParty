package me.timwastaken.minecraftparty.managers;

import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.minigames.AnvilStorm;
import me.timwastaken.minecraftparty.models.minigames.Approximate;
import me.timwastaken.minecraftparty.models.minigames.Duels;
import me.timwastaken.minecraftparty.models.minigames.Lasertag;
import me.timwastaken.minecraftparty.models.minigames.MazeRunner;
import me.timwastaken.minecraftparty.models.minigames.MlgRush;
import me.timwastaken.minecraftparty.models.minigames.MusicalChairs;
import me.timwastaken.minecraftparty.models.minigames.OneInTheChamber;
import me.timwastaken.minecraftparty.models.minigames.dragonescape.DragonEscape;
import me.timwastaken.minecraftparty.models.minigames.hotpotato.HotPotato;
import me.timwastaken.minecraftparty.models.minigames.kingofthehill.KingOfTheHill;
import me.timwastaken.minecraftparty.models.minigames.redlightgreenlight.RedLightGreenLight;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;

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
        try {
            boolean successful = true;
            switch (type) {
                case ANVIL_STORM -> {
                    activeMinigame = new AnvilStorm(players);
                }
                case MLG_RUSH -> {
                    activeMinigame = new MlgRush(players);
                }
                case MUSICAL_CHAIRS -> {
                    activeMinigame = new MusicalChairs(players);
                }
                case LASERTAG -> {
                    activeMinigame = new Lasertag(players);
                }
                case APPROXIMATE -> {
                    activeMinigame = new Approximate(players);
                }
                case DUELS -> {
                    activeMinigame = new Duels(players);
                }
                case ONE_IN_THE_CHAMBER -> {
                    activeMinigame = new OneInTheChamber(players);
                }
                case MAZE_RUNNER -> {
                    activeMinigame = new MazeRunner(players);
                }
                case KING_OF_THE_HILLS -> {
                    activeMinigame = new KingOfTheHill(players);
                }
                case DRAGON_ESCAPE -> {
                    activeMinigame = new DragonEscape(players);
                }
                case RED_LIGHT_GREEN_LIGHT -> {
                    activeMinigame = new RedLightGreenLight(players);
                }
                case HOT_POTATO -> {
                    activeMinigame = new HotPotato(players);
                }
                default -> successful = false;
            }
            if (successful) {
                activeMinigame.loadWorld();
                activeMinigame.startCountdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public static void forceStopMinigame() {
        activeMinigame.forceStop();
    }
}
