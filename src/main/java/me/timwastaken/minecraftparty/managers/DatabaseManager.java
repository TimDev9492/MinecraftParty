package me.timwastaken.minecraftparty.managers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import me.timwastaken.minecraftparty.MinecraftParty;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.enums.ItemType;
import me.timwastaken.minecraftparty.models.other.InventoryKit;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private static MongoClient client;
    private static MongoDatabase database;
    private static MongoCollection<Document> high_scores;
    private static MongoCollection<Document> inv_layouts;
    private static String USERNAME, PASSWORD, HOSTNAME;
    private static int PORT;

    public static void init() {
        USERNAME = MinecraftParty.getInstance().getConfig().getString("database.username");
        PASSWORD = MinecraftParty.getInstance().getConfig().getString("database.password");
        HOSTNAME = MinecraftParty.getInstance().getConfig().getString("database.hostname");
        PORT = MinecraftParty.getInstance().getConfig().getInt("database.port");
    }

    public static void connect() {
        client = MongoClients.create("mongodb://" + HOSTNAME + ":" + PORT);
        database = client.getDatabase("mcparty");
        high_scores = database.getCollection("high_scores");
        inv_layouts = database.getCollection("inv_layouts");
    }

    public static void disconnect() {
        client.close();
    }

//    public static HashMap<Integer, ItemType> getInvLayout(UUID playerId, MinigameType gameType) {
//        return getInvLayout(playerId, gameType, null);
//    }

    public static HashMap<Integer, ItemType> getInvLayout(UUID playerId, InventoryKit kit) {
        HashMap<Integer, ItemType> slotLayout = new HashMap<>();
        Document filter = new Document("uuid", playerId.toString());
        Document playerLayouts = inv_layouts.find(filter).first();
        if (playerLayouts == null) return null;
        Document minigameLayout = playerLayouts.get(kit.getAlias(), Document.class);
        if (minigameLayout == null) return null;
        for (Map.Entry<String, Object> slotItem : minigameLayout.entrySet()) {
            int slot = Integer.parseInt(slotItem.getKey());
            ItemType type = ItemType.valueOf((String) slotItem.getValue());
            slotLayout.put(slot, type);
        }
        return slotLayout;
    }

    public static boolean saveInvLayout(UUID playerId, InventoryKit kit, HashMap<Integer, ItemType> layout) {
        Document filter = new Document("uuid", playerId.toString());

        Document serializedLayout = new Document();
        layout.forEach((k, v) -> {
            serializedLayout.append(k.toString(), v.toString());
        });
        Document document = new Document().append(kit.getAlias(), serializedLayout);
        UpdateResult result = inv_layouts.updateOne(filter, new Document("$set", document), new UpdateOptions().upsert(true));
        return result.wasAcknowledged();
    }

}
