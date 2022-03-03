package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.timwastaken.minecraftparty.models.enums.ItemType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InventoryKit {

    public String getAlias() {
        return alias;
    }

    @JsonProperty("alias")
    private String alias;
    @JsonProperty("name")
    private String name;
    @JsonProperty("items")
    private ArrayList<KitItem> items;
    @JsonProperty("armor")
    private ArrayList<KitArmorPiece> armor;

    public HashMap<ItemType, ItemStack> toItemMap() {
        HashMap<ItemType, ItemStack> itemMap = new HashMap<>();
        for (KitItem item : items) {
            itemMap.put(ItemType.valueOf(item.getType()), item.toItemStack());
        }
        return itemMap;
    }

    public HashMap<Integer, ItemType> getFallback() {
        HashMap<Integer, ItemType> fallback = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            fallback.put(i, ItemType.valueOf(items.get(i).getType()));
        }
        return fallback;
    }

    public List<ItemStack> getArmor() {
        ArrayList<ItemStack> armorItems = new ArrayList<>();
        for (KitArmorPiece armorPiece : armor) {
            armorItems.add(armorPiece.toItemStack());
        }
        return armorItems;
    }

    public ItemStack getHelmet() {
        return armor.get(0).toItemStack();
    }

    public ItemStack getChestplate() {
        return armor.get(1).toItemStack();
    }

    public ItemStack getLeggings() {
        return armor.get(2).toItemStack();
    }

    public ItemStack getBoots() {
        return armor.get(3).toItemStack();
    }

}
