package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

public class KitArmorPiece {

    @JsonProperty("material")
    private String material;
    @JsonProperty("color")
    private String color;
    @JsonProperty("enchantments")
    private ArrayList<KitItemEnchantment> enchantments;

    @JsonIgnore
    private static final List<Material> leatherMats = List.of(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS);

    public ItemStack toItemStack() {
        Material mat = Material.valueOf(material);
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        for (KitItemEnchantment enchantment : enchantments) {
            meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(enchantment.getType())), enchantment.getLevel(), true);
        }
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);
        if (leatherMats.contains(mat)) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) stack.getItemMeta();
            leatherMeta.setColor(Color.fromBGR(Integer.decode(color)));
            stack.setItemMeta(leatherMeta);
        }
        return stack;
    }

}
