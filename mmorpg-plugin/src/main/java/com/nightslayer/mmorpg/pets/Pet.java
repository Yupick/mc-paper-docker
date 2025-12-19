package com.nightslayer.mmorpg.pets;

import java.util.List;
import java.util.Map;

public class Pet {
    private String id;
    private String name;
    private PetType type;
    private String rarity;
    private String description;
    private Map<String, Double> baseStats;
    private List<EvolutionLevel> evolutionLevels;
    private List<String> foodPreferences;
    private int adoptionCost;
    private double feedRestoreHealth;

    public static class EvolutionLevel {
        private int level;
        private String name;
        private int requiredXp;
        private double statsMultiplier;
        private List<String> abilities;

        public EvolutionLevel(int level, String name, int requiredXp, double statsMultiplier, List<String> abilities) {
            this.level = level;
            this.name = name;
            this.requiredXp = requiredXp;
            this.statsMultiplier = statsMultiplier;
            this.abilities = abilities;
        }

        public int getLevel() { return level; }
        public String getName() { return name; }
        public int getRequiredXp() { return requiredXp; }
        public double getStatsMultiplier() { return statsMultiplier; }
        public List<String> getAbilities() { return abilities; }
    }

    public Pet(String id, String name, PetType type, String rarity, String description,
               Map<String, Double> baseStats, List<EvolutionLevel> evolutionLevels,
               List<String> foodPreferences, int adoptionCost, double feedRestoreHealth) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.rarity = rarity;
        this.description = description;
        this.baseStats = baseStats;
        this.evolutionLevels = evolutionLevels;
        this.foodPreferences = foodPreferences;
        this.adoptionCost = adoptionCost;
        this.feedRestoreHealth = feedRestoreHealth;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public PetType getType() { return type; }
    public String getRarity() { return rarity; }
    public String getDescription() { return description; }
    public Map<String, Double> getBaseStats() { return baseStats; }
    public List<EvolutionLevel> getEvolutionLevels() { return evolutionLevels; }
    public List<String> getFoodPreferences() { return foodPreferences; }
    public int getAdoptionCost() { return adoptionCost; }
    public double getFeedRestoreHealth() { return feedRestoreHealth; }

    public double getStat(String statName) {
        return baseStats.getOrDefault(statName, 0.0);
    }

    public EvolutionLevel getEvolutionLevel(int level) {
        for (EvolutionLevel evol : evolutionLevels) {
            if (evol.getLevel() == level) {
                return evol;
            }
        }
        return evolutionLevels.get(0); // Default to first level
    }

    public int getMaxEvolutionLevel() {
        return evolutionLevels.size();
    }
}
