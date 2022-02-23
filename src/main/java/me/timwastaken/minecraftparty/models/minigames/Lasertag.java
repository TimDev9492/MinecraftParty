package me.timwastaken.minecraftparty.models.minigames;

import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.entity.Player;

import java.util.List;

public class Lasertag extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.LASERTAG;

    public Lasertag(Player... players) {
        super(type, List.of(MinigameFlag.ZERO_DAMAGE, MinigameFlag.NO_BLOCK_BREAKING, MinigameFlag.NO_BLOCK_PLACEMENT));
        super.addGameEventListeners(this);
    }

    @Override
    public void onGameStart() {

    }

    @Override
    public void onGameEnd() {

    }

    @Override
    public void onWorldLoaded() {

    }

    @Override
    public void onPlayerLeave(Player p) {

    }

}
