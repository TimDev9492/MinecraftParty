package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class KitArmorPiece {

    @JsonProperty("material")
    private String material;
    @JsonProperty("enchantments")
    private ArrayList<KitItemEnchantment> enchantments;

    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(Material.valueOf(material));
        ItemMeta meta = stack.getItemMeta();
        for (KitItemEnchantment enchantment : enchantments) {
            meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(enchantment.getType())), enchantment.getLevel(), true);
        }
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        return stack;
    }

}
