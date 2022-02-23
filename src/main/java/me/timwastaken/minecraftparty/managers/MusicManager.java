package me.timwastaken.minecraftparty.managers;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import me.timwastaken.minecraftparty.MinecraftParty;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicManager {

    private static List<File> musicFiles;
    private static List<String> fileNames;
    private static boolean IS_PLAYING = false;

    private static RadioSongPlayer radioPlayer;

    public static void init() {
        // load all the files into a list of files
        String nbsDirectory = MinecraftParty.getInstance().getConfig().getString("music.nbs_directory");
        List<String> exclude = MinecraftParty.getInstance().getConfig().getStringList("music.exclude");
        loadSongFiles(nbsDirectory, exclude);
    }

    private static void loadSongFiles(String directory, List<String> excludedFiles) {
        File nbsDirectory = new File(directory);
        musicFiles = new ArrayList<>();
        fileNames = new ArrayList<>();
        for (File f : nbsDirectory.listFiles()) {
            if (!excludedFiles.contains(f.getName()) && f.isFile()) {
                musicFiles.add(f);
                fileNames.add(f.getName());
            }
        }
    }

    public static List<File> getMusicFiles() {
        return musicFiles;
    }

    public static List<File> getCopyOfFiles() {
        return new ArrayList<>(musicFiles);
    }

    public static List<String> getFileNames() {
        return fileNames;
    }

    public static void stopMusic() {
        radioPlayer.setPlaying(false);
        IS_PLAYING = false;
    }

    public static void playSong(String fileNameToLoad) {
        File songFile = songFileFromName(fileNameToLoad);
        if (songFile != null) {
            Song song = NBSDecoder.parse(songFile);
            if (isPlaying() || hasSongLoaded()) radioPlayer.setPlaying(false);
            radioPlayer = new RadioSongPlayer(song);
            Bukkit.getOnlinePlayers().forEach(radioPlayer::addPlayer);
            radioPlayer.setPlaying(true);
            IS_PLAYING = true;
        }
    }

    private static File songFileFromName(String name) {
        return musicFiles.stream().filter(file -> file.getName().equals(name)).findFirst().orElse(null);
    }

    public static boolean isPlaying() {
        return IS_PLAYING;
    }

    public static void toggleMusic() {
        IS_PLAYING = !IS_PLAYING;
        radioPlayer.setPlaying(IS_PLAYING);
    }

    public static boolean hasSongLoaded() {
        return radioPlayer != null && radioPlayer.getSong() != null;
    }

}
