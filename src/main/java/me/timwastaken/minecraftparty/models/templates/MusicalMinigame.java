package me.timwastaken.minecraftparty.models.templates;

import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;

import java.io.IOException;
import java.util.List;

public class MusicalMinigame extends Minigame {

    public MusicalMinigame(MinigameType type, List<MinigameFlag> flags) throws IOException {
        super(type, flags);
    }

}
