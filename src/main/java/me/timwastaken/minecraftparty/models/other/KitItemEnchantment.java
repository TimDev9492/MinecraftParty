package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KitItemEnchantment {

    @JsonProperty("type")
    private String type;
    @JsonProperty("level")
    private int level;

    public String getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

}
