package me.timwastaken.minecraftparty.models.enums;

public enum MinigameFlag {

    ZERO_FALL_DAMAGE,       // fall damage gets set to 0
    NO_FALL_DAMAGE,         // fall damage gets cancelled (no damage sound and animation)
    ZERO_DAMAGE,            // all damage gets set to 0
    NO_DAMAGE,              // all damage gets cancelled
    ZERO_PVP_DAMAGE,        // damage to player by players gets set to 0
    NO_PVP,                 // pvp damage gets cancelled
    NO_PVE,                 // damage to non-player entities by players gets cancelled
    NO_BLOCK_BREAKING,      // disables the ability to break blocks
    NO_BLOCK_PLACEMENT,     // disables the ability to place blocks
    NO_BLOCK_DROPS,         // disables items from dropping when breaking blocks
    NO_MAP_BREAKING         // lets player only destroy blocks that were placed by players

}
