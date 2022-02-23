package me.timwastaken.minecraftparty.models.enums;

import me.timwastaken.minecraftparty.MinecraftParty;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.Arrays;

public enum MinigameType {

    // requires the config to have the section minigames.<alias> with the following arguments:
    //
    // - origin -> x, y, z (world spawn position)
    // - world_name -> name of the world directory
    // - reward -> reward value a player gets

    ANVIL_STORM(ChatColor.GOLD + "" + ChatColor.BOLD + "Anvil Storm", "anvil_storm"),
    MLG_RUSH(ChatColor.GOLD + "" + ChatColor.BOLD + "MLG Rush", "mlg_rush"),
    MUSICAL_CHAIRS(ChatColor.GOLD + "" + ChatColor.BOLD + "Musical Chairs", "musical_chairs"),
    LASERTAG(ChatColor.GOLD + "" + ChatColor.BOLD + "Lasertag", "lasertag");

    private final String displayName;
    private final String alias;
    private final String worldName;
    private final Vector origin;
    private final int reward;

    MinigameType(String displayName, String alias) {
        this.displayName = displayName;
        this.alias = alias;
        this.worldName = MinecraftParty.getInstance().getConfig().getString("minigames." + alias + ".world_name");
        this.origin = new Vector(
                MinecraftParty.getInstance().getConfig().getInt("minigames." + alias + ".origin.x") + 0.5,
                MinecraftParty.getInstance().getConfig().getInt("minigames." + alias + ".origin.y"),
                MinecraftParty.getInstance().getConfig().getInt("minigames." + alias + ".origin.z") + 0.5
        );
        this.reward = MinecraftParty.getInstance().getConfig().getInt("minigames." + alias + ".reward");
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

}
