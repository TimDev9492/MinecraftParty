package me.timwastaken.minecraftparty.models.templates;

import me.timwastaken.minecraftparty.managers.DatabaseManager;
import me.timwastaken.minecraftparty.models.enums.ItemType;
import me.timwastaken.minecraftparty.models.enums.MinigameFlag;
import me.timwastaken.minecraftparty.models.enums.MinigameType;
import me.timwastaken.minecraftparty.models.other.InventoryKit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class InvLayoutBasedMinigame extends Minigame {

    private ConcurrentHashMap<UUID, HashMap<Integer, ItemType>> playerInventoryLayouts;
    private HashMap<Integer, ItemType> fallback;
    private ConcurrentHashMap<ItemType, ItemStack> itemMap;
    private ConcurrentHashMap<ItemStack, ItemType> reversedItemMap;
    private InventoryKit currentKit;
    private Player[] players;

    public void loadKit(InventoryKit kit) {
        currentKit = kit;
        setFallback(kit.getFallback());
        setItemMap(kit.toItemMap());
    }

    private void setFallback(HashMap<Integer, ItemType> fallback) {
        this.fallback = fallback;

        playerInventoryLayouts = new ConcurrentHashMap<>();
        for (Player p : players) {
            playerInventoryLayouts.put(p.getUniqueId(), loadInventoryLayout(p.getUniqueId()));
        }
    }

    private void setItemMap(HashMap<ItemType, ItemStack> itemMap) {
        this.itemMap = new ConcurrentHashMap<>(itemMap);
        this.reversedItemMap = computeReversedItemMap(itemMap);
    }

    public InvLayoutBasedMinigame(MinigameType type, List<MinigameFlag> flags, Player... players) throws IOException {
        super(type, flags);
        this.players = players;
    }

    private ConcurrentHashMap<ItemStack, ItemType> computeReversedItemMap(HashMap<ItemType, ItemStack> itemMap) {
        ConcurrentHashMap<ItemStack, ItemType> reversed = new ConcurrentHashMap<>();
        for (ItemType type : itemMap.keySet()) {
            reversed.put(itemMap.get(type), type);
        }
        return reversed;
    }

    private HashMap<Integer, ItemType> loadInventoryLayout(UUID id) {
        HashMap<Integer, ItemType> layout = DatabaseManager.getInvLayout(id, currentKit);
        return layout == null ? fallback : layout;
    }

    public HashMap<Integer, ItemType> getPlayerInventoryLayout(Player p) {
        return getPlayerInventoryLayout(p.getUniqueId());
    }

    public HashMap<Integer, ItemType> getPlayerInventoryLayout(UUID id) {
        return playerInventoryLayouts.containsKey(id) ? playerInventoryLayouts.get(id) : fallback;
    }

    public ConcurrentHashMap<ItemType, ItemStack> getItemMap() {
        return itemMap;
    }

    public void resetInventory(Player p) {
        p.getInventory().clear();
        HashMap<Integer, ItemType> playerInv = getPlayerInventoryLayout(p.getUniqueId());
        for (int slot : playerInv.keySet()) {
            p.getInventory().setItem(slot, getItemStackFromType(playerInv.get(slot)));
        }
        p.getInventory().setItem(EquipmentSlot.HEAD, currentKit.getHelmet());
        p.getInventory().setItem(EquipmentSlot.CHEST, currentKit.getChestplate());
        p.getInventory().setItem(EquipmentSlot.LEGS, currentKit.getLeggings());
        p.getInventory().setItem(EquipmentSlot.FEET, currentKit.getBoots());
    }

    public void resetInventory(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        resetInventory(p);
    }

    private ItemStack getItemStackFromType(ItemType itemType) {
        return itemMap.get(itemType);
    }

    protected void updateInvLayout(Player p) {
        HashMap<Integer, ItemType> layout = new HashMap<>();
        for (int i = 0; i < p.getInventory().getContents().length; i++) {
            ItemStack current = p.getInventory().getItem(i);
            if (current == null) continue;
            for (Map.Entry<ItemStack, ItemType> entry : reversedItemMap.entrySet()) {
                ItemStack stack = entry.getKey();
                ItemType connectedType = entry.getValue();
                if (!layout.containsValue(connectedType) && areSimilar(current, stack)) {
                    layout.put(i, connectedType);
//                    System.out.println("updated " + p.getName() + ": " + i + " -> " + connectedType);
                }
            }
        }
        if (!layout.isEmpty()) {
            playerInventoryLayouts.put(p.getUniqueId(), layout);
        }
    }

    private boolean areSimilar(ItemStack stack1, ItemStack stack2) {
        if (stack1.getType() != stack2.getType()) return false;
        return stack1.getItemMeta().getDisplayName().equals(stack2.getItemMeta().getDisplayName());
    }

    public boolean saveLayoutToDatabase(Player p, InventoryKit kit) {
        return DatabaseManager.saveInvLayout(p.getUniqueId(), kit, playerInventoryLayouts.get(p.getUniqueId()));
    }

    public void saveLayoutsToDatabase() {
        playerInventoryLayouts.forEach((uuid, map) -> DatabaseManager.saveInvLayout(uuid, currentKit, map));
    }

}