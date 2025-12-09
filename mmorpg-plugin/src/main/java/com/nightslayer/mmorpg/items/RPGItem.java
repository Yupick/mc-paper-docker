package com.nightslayer.mmorpg.items;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Representa un item RPG con atributos y rareza
 */
public class RPGItem {
    private final String id;
    private final String name;
    private final Material material;
    private final Rarity rarity;
    private final Map<String, Double> attributes;
    private final List<ItemManager.EnchantmentData> enchantments;
    
    public RPGItem(String id, String name, Material material, Rarity rarity,
                   Map<String, Double> attributes, List<ItemManager.EnchantmentData> enchantments) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.rarity = rarity;
        this.attributes = attributes;
        this.enchantments = enchantments;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public Rarity getRarity() {
        return rarity;
    }
    
    public Map<String, Double> getAttributes() {
        return attributes;
    }
    
    public List<ItemManager.EnchantmentData> getEnchantments() {
        return enchantments;
    }
}
