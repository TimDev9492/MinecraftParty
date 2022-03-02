package me.timwastaken.minecraftparty.models.interfaces;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public interface GameEventListener {

    void onGameStart();
    void onGameEnd();
    void onWorldLoaded();
    void onPlayerLeave(Player p);
    void onPlayerJoin(Player p);

}
