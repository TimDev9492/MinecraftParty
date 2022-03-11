package me.timwastaken.minecraftparty.models.minigames.dragonescape;

import me.timwastaken.minecraftparty.managers.ScoreboardSystem;
import me.timwastaken.minecraftparty.managers.StructureManager;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.interfaces.GameEventListener;
import me.timwastaken.minecraftparty.models.other.MinigameUtils;
import me.timwastaken.minecraftparty.models.templates.Minigame;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DragonEscape extends Minigame implements GameEventListener {

    private static final MinigameType type = MinigameType.DRAGON_ESCAPE;

    private final Random rnd;
    private final ArrayList<Player> participants;
    private Team grayedOut;

    private Location moduleOrigin;
    private Vector moduleSize;

    public DragonEscape(Player... players) throws IOException {
        super(type, List.of(MinigameFlag.NO_PVP, MinigameFlag.NO_MAP_BREAKING, MinigameFlag.NO_DAMAGE));
        super.addGameEventListeners(this);
        rnd = new Random();
        participants = new ArrayList<>(Arrays.asList(players));
    }

    @Override
    public void onGameStart() {

    }

    @Override
    public void onGameEnd() {

    }

    @Override
    public void onWorldLoaded() {
        gameWorld.setAutoSave(false);
        moduleOrigin = new Location(gameWorld,
                getConfig().getInt("module_origin.x"),
                getConfig().getInt("module_origin.y"),
                getConfig().getInt("module_origin.z")
        );
        moduleSize = new Vector(
                getConfig().getInt("module_size.x"),
                getConfig().getInt("module_size.y"),
                getConfig().getInt("module_size.z")
        );
        int yaw = getConfig().getInt("origin.yaw");
        // generate modules in spawn chunks
        for (int chunkX = -2; chunkX <= 2; chunkX++) {
            generateChunk(gameWorld.getChunkAt(chunkX, moduleOrigin.getChunk().getZ()));
        }

        grayedOut = MinigameUtils.getScoreboardTeam("mcparty_grayed_out");
        grayedOut.setCanSeeFriendlyInvisibles(true);
        Location spawn = origin.clone();
        spawn.setYaw(yaw);
        participants.forEach(p -> {
            grayedOut.addEntry(p.getName());
//            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            p.teleport(spawn);
        });
    }

    @Override
    public void onPlayerLeave(Player p) {
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(potionEffect -> p.removePotionEffect(potionEffect.getType()));
        participants.remove(p);
        grayedOut.removeEntry(p.getName());
        ScoreboardSystem.refreshScoreboards();
    }

    @Override
    public void onPlayerJoin(Player p) {
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(origin);
    }

    public void onChunkGenerate(ChunkLoadEvent event) {
        generateChunk(event.getChunk());
    }

    private void generateChunk(Chunk generationChunk) {
        int chunkZ = moduleOrigin.getChunk().getZ();
        if (generationChunk.getZ() == chunkZ && generationChunk.getX() >= moduleOrigin.getChunk().getX()) {
            int chunkOriginX = generationChunk.getX() * 16;
            int nextModuleX = (int) Math.ceil(chunkOriginX / moduleSize.getX()) * moduleSize.getBlockX();
            Location nextModuleStart = new Location(gameWorld, nextModuleX, moduleOrigin.getBlockY(), moduleOrigin.getBlockZ());
            if (nextModuleStart.getChunk().equals(generationChunk)) {
                placeRandomModule(nextModuleStart);
//                nextModuleStart.getBlock().setType(Material.GOLD_BLOCK);
            }
        }
    }

    public void placeRandomModule(Location moduleStart) {
        File structureFile = StructureManager.getStructureFiles().get(rnd.nextInt(StructureManager.getStructureFiles().size()));
        try {
            StructureManager.placeStructure(structureFile, moduleStart, false, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWorldName() {
        if (gameWorld == null) return type.getAlias();
        return gameWorld.getName();
    }

}
