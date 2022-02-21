package me.timwastaken.minecraftparty.models.minigames;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.playmode.MonoMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.GameEventListener;
import me.timwastaken.minecraftparty.models.Minigame;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class JourneyToSalem extends Minigame implements GameEventListener {

    // disable player hitting
    // disable minecart hitting

    private static final MinigameType type = MinigameType.JOURNEY_TO_SALEM;
    private final ArrayList<BukkitRunnable> gameLoops;

    private String nbsDirectoryPath;
    private int songBufferSize;
    private int minTicks;
    private int maxTicks;
    private int ticksBetween;
    private int exponentialCab;
    private int spawnRadius;
    private ArrayList<File> filesLeft;
    private ArrayList<Minecart> existingMinecarts;

    private final ArrayList<UUID> ingamePlayers;
    private final HashMap<Song, String> songs;
    private final Random rnd;
    private RadioSongPlayer radio;

    public JourneyToSalem(Player... players) {
        super(type);
        super.addGameEventListeners(this);

        existingMinecarts = new ArrayList<>();
        gameLoops = new ArrayList<>();
        ingamePlayers = new ArrayList<>();
        rnd = new Random();
        songs = new HashMap<>();
        Arrays.stream(players).forEach(player -> ingamePlayers.add(player.getUniqueId()));
    }

    public void onPlayerEnterMinecart(Player p) {
        if (!ingamePlayers.contains(p.getUniqueId())) return;
        if (radio.isPlaying()) {
            playerOut(p);
        }
    }

    @Override
    public void onGameStart() {
        initNextRound();
    }

    private void initNextRound() {
        playMusic();
        spawnMinecarts();
        int randomTicks = rnd.nextInt(minTicks, maxTicks + 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                stopMusic();
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 1f));
                BukkitRunnable next = new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Throw out players
                        for (int i = ingamePlayers.size() - 1; i >= 0; i--) {
                            UUID uuid = ingamePlayers.get(i);
                            Player p = Bukkit.getPlayer(uuid);
                            if (p == null) continue;
                            if (!(p.getVehicle() instanceof Minecart)) {
                                playerOut(p);
                            }
                        }
                        killMinecarts();
                        if (!checkGameEnd()) initNextRound();
                        else endGame();
                    }
                };
                next.runTaskLater(MinecraftParty.getInstance(), ticksBetween);
                gameLoops.add(next);
            }
        }.runTaskLater(MinecraftParty.getInstance(), randomTicks);
    }

    private void playerOut(Player p) {
        if (!ingamePlayers.contains(p.getUniqueId())) return;
        ingamePlayers.remove(p.getUniqueId());
        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        p.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "You're out", ChatColor.GRAY + "No minecart for you", 10, 80, 10);
        p.setGameMode(GameMode.SPECTATOR);
    }

    private void killMinecarts() {
        existingMinecarts.forEach(Minecart::remove);
    }

    private void spawnMinecarts() {
        int minecartAmount = ingamePlayers.size() >= exponentialCab ? (int) Math.ceil(ingamePlayers.size() / 2f) : ingamePlayers.size() - 1;
        for (int i = 0; i < minecartAmount; i++) {
            double scalarRadius = Math.sqrt(Math.random());
            double angle = Math.random() * 2 * Math.PI;
            int x = (int) Math.round(spawnRadius * scalarRadius * Math.cos(angle));
            int z = (int) Math.round(spawnRadius * scalarRadius * Math.sin(angle));
            int y = 5;
            Minecart minecart = (Minecart) gameWorld.spawnEntity(origin.clone().add(x, y, z), EntityType.MINECART);
            existingMinecarts.add(minecart);
        }
    }

    private boolean checkGameEnd() {
        boolean end = ingamePlayers.size() <= 1;
        if (end) {
            Player winner = Bukkit.getPlayer(ingamePlayers.get(0));
            if (winner == null) return end;
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + winner.getName(), ChatColor.GRAY + "won the game", 10, 80, 10);
            });
        }
        return end;
    }

    private void loadSongs() {
        File nbsDirectory = new File(nbsDirectoryPath);
        filesLeft = new ArrayList<>(Arrays.asList(nbsDirectory.listFiles()));
        songs.clear();
        for (int i = 0; i < songBufferSize; i++) {
            int index = rnd.nextInt(filesLeft.size());
            File randomFile = filesLeft.get(index);
            Song song = NBSDecoder.parse(randomFile);
            String songName = song.getTitle();
            if (songName.isEmpty()) {
                songName = removeExtention(randomFile.getName());
            }
            songs.put(song, songName);
            filesLeft.remove(index);
        }
    }

    private Playlist randomPlaylist() {
        loadSongs();
        return new Playlist(songs.keySet().toArray(new Song[0]));
    }

    private void playMusic() {
        int index = radio.getPlayedSongIndex();
        if (index == songBufferSize - 1) {
            radio.setPlaylist(randomPlaylist());
            radio.playSong(0);
        } else {
            radio.playSong(index + 1);
        }
        radio.setPlaying(true);
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "Now playing: " + ChatColor.GREEN + "" + ChatColor.BOLD + songs.get(radio.getSong())));
        });
    }

    private void stopMusic() {
        radio.setPlaying(false);
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "Get into a minecart!"));
        });
    }

    @Override
    public void onGameEnd() {
        killMinecarts();
        gameLoops.forEach(BukkitRunnable::cancel);
    }

    @Override
    public void onWorldLoaded() {
        ConfigurationSection section = MinecraftParty.getInstance().getConfig().getConfigurationSection("minigames." + type.getAlias());
        nbsDirectoryPath = section.getString("nbs_directory");
        songBufferSize = section.getInt("song_buffer_size");
        minTicks = section.getInt("min_ticks");
        maxTicks = section.getInt("max_ticks");
        ticksBetween = section.getInt("ticks_between");
        exponentialCab = section.getInt("exponential_cab");
        spawnRadius = section.getInt("spawn_radius");

        Playlist playlist = randomPlaylist();
        radio = new RadioSongPlayer(playlist);
        radio.setChannelMode(new MonoMode());
        radio.setPlaying(false);
        ingamePlayers.forEach(p -> {
            radio.addPlayer(p);
            Bukkit.getPlayer(p).setGameMode(GameMode.ADVENTURE);
        });
    }

    @Override
    public void onPlayerLeave(Player p) {
        ingamePlayers.remove(p.getUniqueId());
    }

    private String removeExtention(String filePath) {
        // These first few lines the same as Justin's
        File f = new File(filePath);

        // if it's a directory, don't remove the extention
        if (f.isDirectory()) return filePath;

        String name = f.getName();

        // Now we know it's a file - don't need to do any special hidden
        // checking or contains() checking because of:
        final int lastPeriodPos = name.lastIndexOf('.');
        if (lastPeriodPos <= 0) {
            // No period after first character - return name as it was passed in
            return filePath;
        } else {
            // Remove the last period and everything after it
            File renamed = new File(f.getParent(), name.substring(0, lastPeriodPos));
            return renamed.getPath();
        }
    }

}
