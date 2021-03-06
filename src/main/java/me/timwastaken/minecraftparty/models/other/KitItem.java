package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class KitItem {

    @JsonProperty("type")
    private String type;
    @JsonProperty("material")
    private String material;
    @JsonProperty("amount")
    private int amount;
    @JsonProperty("enchantments")
    private ArrayList<KitItemEnchantment> enchantments;

    public String getType() {
        return type;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public ArrayList<KitItemEnchantment> getEnchantments() {
        return enchantments;
    }

    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(Material.valueOf(material), amount);
        ItemMeta meta = stack.getItemMeta();
        for (KitItemEnchantment enchantment : enchantments) {
            meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(enchantment.getType())), enchantment.getLevel(), true);
        }
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

}
