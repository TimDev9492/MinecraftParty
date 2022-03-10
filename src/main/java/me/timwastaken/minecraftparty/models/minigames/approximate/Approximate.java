package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.managers.NotificationManager;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Approximate extends Minigame implements GameEventListener {

    private static MinigameType type = MinigameType.APPROXIMATE;

//    private final int rounds;
//    private final List<?> questions;
    private final ArrayList<Player> participants;

    public Approximate(Player... players) throws IOException {
        super(type, List.of(
                MinigameFlag.NO_BLOCK_PLACEMENT,
                MinigameFlag.NO_BLOCK_BREAKING,
                MinigameFlag.NO_PVP,
                MinigameFlag.NO_DAMAGE
        ));
        super.addGameEventListeners(this);

//        // game parameters
//        this.rounds = getConfig().getInt("rounds");
//        this.questions = getConfig().getList("questions");
        this.participants = new ArrayList<>();
//
        participants.addAll(Arrays.asList(players));
    }

    @Override
    public void onGameStart() {
        for (Player player : participants) {
            NotificationManager.notifyPlayerOut(player, "Hello World!");
        }
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
//        int value1 = getConfig().getInt("value1"); // must exist in config.yml under minigames.your_plugin_alias
        // ...
    }

    @Override
    public void onPlayerLeave(Player p) {
//        ingamePlayers.remove(p.getUniqueId());
//        if (ingamePlayers.size() < 2) {
//            onGameEnd();
//        }
    }

    @Override
    public void onPlayerJoin(Player p) {

    }

}
