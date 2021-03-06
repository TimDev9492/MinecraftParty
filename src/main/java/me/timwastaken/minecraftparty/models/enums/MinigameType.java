package me.timwastaken.minecraftparty.models.enums;

import me.timwastaken.minecraftparty.MinecraftParty;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.Arrays;

public enum MinigameType {

    // requires a <alias>.yml config file in the resources directory with the following values:
    //
    // - origin -> x, y, z (world spawn position)
    // - world_name -> name of the world directory
    // - reward -> reward value a player gets

    ANVIL_STORM(ChatColor.GOLD + "" + ChatColor.BOLD + "Anvil Storm", "anvil_storm"),
    MLG_RUSH(ChatColor.GOLD + "" + ChatColor.BOLD + "MLG Rush", "mlg_rush"),
    MUSICAL_CHAIRS(ChatColor.GOLD + "" + ChatColor.BOLD + "Musical Chairs", "musical_chairs"),
    LASERTAG(ChatColor.GOLD + "" + ChatColor.BOLD + "Lasertag", "lasertag"),
    APPROXIMATE(ChatColor.GOLD + "" + ChatColor.BOLD + "Approximate", "approximate"),
    DUELS(ChatColor.GOLD + "" + ChatColor.BOLD + "Duels", "duels"),
    ONE_IN_THE_CHAMBER(ChatColor.GOLD + "" + ChatColor.BOLD + "One In The Chamber", "one_in_the_chamber"),
    MAZE_RUNNER(ChatColor.GOLD + "" + ChatColor.BOLD + "Maze Runner", "maze_runner"),
    KING_OF_THE_HILLS(ChatColor.GOLD + "" + ChatColor.BOLD + "King Of The Hill", "king_of_the_hill"),
    DRAGON_ESCAPE(ChatColor.GOLD + "" + ChatColor.BOLD + "Dragon Escape", "dragon_escape"),
    RED_LIGHT_GREEN_LIGHT(ChatColor.GOLD + "" + ChatColor.BOLD + "Red Light Green Light", "red_light_green_light"),
    HOT_POTATO(ChatColor.GOLD + "" + ChatColor.BOLD + "Hot Potato", "hot_potato");

    private final String displayName;
    private final String alias;

    // need to get passed through feedConfig()
    private String worldName;
    private Vector origin;
    private int reward;
    private int countdown;

    MinigameType(String displayName, String alias) {
        this.displayName = displayName;
        this.alias = alias;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAlias() {
        return alias;
    }

    public int getReward() {
        return reward;
    }

    @Override
    public String toString() {
        return alias;
    }

    public static MinigameType fromAlias(String alias) {
        return Arrays.stream(MinigameType.values()).filter(type -> type.getAlias().equals(alias)).findAny().orElse(null);
    }

    public String getWorldName() {
        return worldName;
    }

    public Vector getOrigin() {
        return origin;
    }

    public void feedConfig(FileConfiguration typeConfig) {
        this.worldName = typeConfig.getString("world_name");
        this.origin = new Vector(
                typeConfig.getInt("origin.x") + 0.5,
                typeConfig.getInt("origin.y"),
                typeConfig.getInt("origin.z") + 0.5
        );
        this.reward = typeConfig.getInt("reward");
        this.countdown = typeConfig.getInt("countdown", 5);
    }

    public int getCountdown() {
        return countdown;
    }

}
