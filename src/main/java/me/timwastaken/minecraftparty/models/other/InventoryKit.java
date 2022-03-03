package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonProperty;
import me.timwastaken.minecraftparty.models.enums.ItemType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;

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

    public HashMap<ItemType, ItemStack> toItemMap() {
        HashMap<ItemType, ItemStack> itemMap = new HashMap<>();
        for (KitItem item : items) {
            ItemStack stack = new ItemStack(Material.valueOf(item.getMaterial()), item.getAmount());
            ItemMeta meta = stack.getItemMeta();
            for (KitItemEnchantment enchantment : item.getEnchantments()) {
                meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(enchantment.getType())), enchantment.getLevel(), true);
            }
            stack.setItemMeta(meta);
            itemMap.put(ItemType.valueOf(item.getType()), stack);
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

}
