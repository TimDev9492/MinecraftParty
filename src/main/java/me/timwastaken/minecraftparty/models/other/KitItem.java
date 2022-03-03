package me.timwastaken.minecraftparty.models.other;

import com.fasterxml.jackson.annotation.JsonProperty;

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

}
