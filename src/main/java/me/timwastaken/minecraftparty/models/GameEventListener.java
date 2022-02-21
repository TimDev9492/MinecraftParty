package me.timwastaken.minecraftparty.models;

import org.bukkit.entity.Player;

public interface GameEventListener {

    void onGameStart();
    void onGameEnd();
    void onWorldLoaded();
    void onPlayerLeave(Player p);

}
