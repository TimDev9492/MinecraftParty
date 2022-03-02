package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public class Approximate extends Minigame implements GameEventListener {

    private static MinigameType type = MinigameType.APPROXIMATE;

    public Approximate(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_BLOCK_PLACEMENT, MinigameFlag.NO_BLOCK_BREAKING));
        super.addGameEventListeners(this); // required for your events to work
    }

    @Override
    public void onGameStart() {
        // gets executed when the game launches
    }

    @Override
    public void onGameEnd() {
        // gets executed when the game ends
    }

    @Override
    public void onWorldLoaded() {
        // gets executed as soon as your world is loaded

        // use this to load desired config options that influence the behavior of your plugin
        // example below:
        int value1 = getConfig().getInt("value1"); // must exist in config.yml under minigames.your_plugin_alias
        // ...
    }

    @Override
    public void onPlayerLeave(Player p) {
        // gets executed when a player quits the server during your game
    }

    @Override
    public void onPlayerJoin(Player p) {

    }

}
