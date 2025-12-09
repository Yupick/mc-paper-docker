package com.nightslayer.mmorpg.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Recipe {
    private String id;
    private String name;
    private String description;
    private String resultItem;
    private int resultAmount;
    private String tier;
    private List<RecipeIngredient> ingredients;
    private int experienceReward;
    private long coinReward;
    private int craftingTimeSeconds;
    private String requiresStation;
    private boolean unlockedByDefault;

    // Constructor simple
    public Recipe(String id, String name, String description, String resultItem, String tier) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.resultItem = resultItem;
        this.resultAmount = 1;
        this.tier = tier;
        this.ingredients = new ArrayList<>();
        this.experienceReward = 0;
        this.coinReward = 0;
        this.craftingTimeSeconds = 60;
        this.requiresStation = null;
        this.unlockedByDefault = false;
    }

    // Constructor completo
    public Recipe(String id, String name, String description, String resultItem, int resultAmount,
                  String tier, List<RecipeIngredient> ingredients, int experienceReward,
                  long coinReward, int craftingTimeSeconds, String requiresStation,
                  boolean unlockedByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.resultItem = resultItem;
        this.resultAmount = resultAmount;
        this.tier = tier;
        this.ingredients = ingredients;
        this.experienceReward = experienceReward;
        this.coinReward = coinReward;
        this.craftingTimeSeconds = craftingTimeSeconds;
        this.requiresStation = requiresStation;
        this.unlockedByDefault = unlockedByDefault;
    }

    public static class RecipeIngredient {
        public String material;
        public int amount;
        public String type; // MATERIAL o MOB_DROP

        public RecipeIngredient(String material, int amount, String type) {
            this.material = material;
            this.amount = amount;
            this.type = type;
        }

        public String getMaterial() { return material; }
        public int getAmount() { return amount; }
        public String getType() { return type; }
    }

    // Setters para builder pattern
    public void addIngredient(String material, int amount, String type) {
        ingredients.add(new RecipeIngredient(material, amount, type));
    }

    public void setExperienceReward(int xp) { this.experienceReward = xp; }
    public void setCoinReward(long coins) { this.coinReward = coins; }
    public void setCraftingTimeSeconds(int seconds) { this.craftingTimeSeconds = seconds; }
    public void setRequiredStation(String station) { this.requiresStation = station; }
    public void setUnlockedByDefault(boolean unlocked) { this.unlockedByDefault = unlocked; }
    public void setResultAmount(int amount) { this.resultAmount = amount; }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getResultItem() { return resultItem; }
    public int getResultAmount() { return resultAmount; }
    public String getTier() { return tier; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public int getExperienceReward() { return experienceReward; }
    public long getCoinReward() { return coinReward; }
    public int getCraftingTimeSeconds() { return craftingTimeSeconds; }
    public String getRequiresStation() { return requiresStation; }
    public boolean isUnlockedByDefault() { return unlockedByDefault; }

    public boolean canCraft(Map<String, Integer> inventory) {
        for (RecipeIngredient ingredient : ingredients) {
            int available = inventory.getOrDefault(ingredient.material, 0);
            if (available < ingredient.amount) {
                return false;
            }
        }
        return true;
    }

    public void consumeIngredients(Map<String, Integer> inventory) {
        for (RecipeIngredient ingredient : ingredients) {
            int current = inventory.getOrDefault(ingredient.material, 0);
            inventory.put(ingredient.material, current - ingredient.amount);
        }
    }

    public void addResult(Map<String, Integer> inventory) {
        int current = inventory.getOrDefault(resultItem, 0);
        inventory.put(resultItem, current + resultAmount);
    }
}
