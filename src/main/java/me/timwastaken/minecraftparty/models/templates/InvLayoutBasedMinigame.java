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
    private final Player[] players;

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
    }

    public InvLayoutBasedMinigame(MinigameType type, List<MinigameFlag> flags, Player... players) throws IOException {
        super(type, flags);
        this.players = players;
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

}