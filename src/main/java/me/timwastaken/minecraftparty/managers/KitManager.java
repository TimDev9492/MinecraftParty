package me.timwastaken.minecraftparty.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.other.InventoryKit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class KitManager {

    private static HashMap<String, InventoryKit> inventoryKits;

    public static void init() throws IOException {
        inventoryKits = loadKitsFromJson(ConfigManager.getConfig(MinigameType.DUELS.getAlias()).getString("kit_database_path"));
    }

    private static HashMap<String, InventoryKit> loadKitsFromJson(String directory) throws IOException {
        HashMap<String, InventoryKit> kits = new HashMap<>();
        File jsonDirectory = new File(directory);
        ObjectMapper mapper = new ObjectMapper();
        for (File jsonFile : jsonDirectory.listFiles()) {
            if (jsonFile.isFile()) {
                InventoryKit kit = mapper.readValue(jsonFile, InventoryKit.class);
                kits.put(kit.getAlias(), kit);
            }
        }
        return kits;
    }

    public static InventoryKit getKit(String alias) {
        return inventoryKits.get(alias);
    }

}
