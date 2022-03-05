package me.timwastaken.minecraftparty.models.templates;

import me.timwastaken.minecraftparty.managers.MusicManager;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public class MusicalMinigame extends Minigame {

    public MusicalMinigame(MinigameType type, List<MinigameFlag> flags, Player... players) throws IOException {
        super(type, flags);
        MusicManager.setVolume((byte) 100);
        MusicManager.addPlayers(players);
        MusicManager.stopMusic();
    }

}
