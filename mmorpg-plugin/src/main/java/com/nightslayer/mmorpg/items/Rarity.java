package com.nightslayer.mmorpg.items;

/**
 * Representa una rareza de item con multiplicadores
 */
public class Rarity {
    private final String name;
    private final String color;
    private final double dropMultiplier;
    private final double attributeMultiplier;
    private final double dropChance;
    private final String description;
    
    public Rarity(String name, String color, double dropMultiplier, 
                  double attributeMultiplier, double dropChance, String description) {
        this.name = name;
        this.color = color;
        this.dropMultiplier = dropMultiplier;
        this.attributeMultiplier = attributeMultiplier;
        this.dropChance = dropChance;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getColor() {
        return color;
    }
    
    public double getDropMultiplier() {
        return dropMultiplier;
    }
    
    public double getAttributeMultiplier() {
        return attributeMultiplier;
    }
    
    public double getDropChance() {
        return dropChance;
    }
    
    public String getDescription() {
        return description;
    }
}
