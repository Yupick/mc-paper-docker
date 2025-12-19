package com.nightslayer.mmorpg.enchanting;

import java.util.*;

public class RPGEnchantment {
    private final String id;
    private final String name;
    private final String description;
    private final int maxLevel;
    private final Set<String> applicableItems;
    private final Set<String> incompatibleWith;
    private final Map<Integer, Map<String, Double>> effects;
    private final String rarity;
    private final Map<Integer, String> rarityByLevel;

    public RPGEnchantment(String id, String name, String description, int maxLevel,
                         Set<String> applicableItems, Set<String> incompatibleWith,
                         Map<Integer, Map<String, Double>> effects, String rarity,
                         Map<Integer, String> rarityByLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.applicableItems = applicableItems;
        this.incompatibleWith = incompatibleWith;
        this.effects = effects;
        this.rarity = rarity;
        this.rarityByLevel = rarityByLevel;
    }

    public boolean canApplyTo(String itemType) {
        return applicableItems.contains(itemType);
    }

    public boolean isIncompatibleWith(String enchantmentId) {
        return incompatibleWith.contains(enchantmentId);
    }

    public Map<String, Double> getEffectsForLevel(int level) {
        if (level < 1 || level > maxLevel) {
            return new HashMap<>();
        }
        return effects.getOrDefault(level, new HashMap<>());
    }

    public String getRarityForLevel(int level) {
        return rarityByLevel.getOrDefault(level, rarity);
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMaxLevel() { return maxLevel; }
    public Set<String> getApplicableItems() { return applicableItems; }
    public Set<String> getIncompatibleWith() { return incompatibleWith; }
    public String getRarity() { return rarity; }
}
