package com.nightslayer.mmorpg.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CraftingConfig {
    private final JavaPlugin plugin;
    private final Map<String, Recipe> recipes;
    private final Map<String, CraftingStation> stations;
    private final JsonObject configData;

    public CraftingConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.recipes = new HashMap<>();
        this.stations = new HashMap<>();
        this.configData = loadConfigFile();
        loadRecipes();
        loadStations();
    }

    private JsonObject loadConfigFile() {
        File configFile = new File(plugin.getDataFolder(), "crafting_config.json");
        if (!configFile.exists()) {
            plugin.getLogger().warning("Crafting config file not found: " + configFile.getPath());
            return new JsonObject();
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(configFile.getPath())));
            return com.google.gson.JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading crafting config: " + e.getMessage());
            return new JsonObject();
        }
    }

    private void loadRecipes() {
        if (!configData.has("recipes")) {
            plugin.getLogger().warning("No recipes found in crafting config");
            return;
        }

        JsonArray recipesArray = configData.getAsJsonArray("recipes");
        for (JsonElement element : recipesArray) {
            JsonObject recipeObj = element.getAsJsonObject();
            try {
                Recipe recipe = parseRecipe(recipeObj);
                recipes.put(recipe.getId(), recipe);
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing recipe: " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + recipes.size() + " recipes");
    }

    private Recipe parseRecipe(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String description = obj.get("description").getAsString();
        String resultItem = obj.get("result_item").getAsString();
        int resultAmount = obj.has("result_amount") ? obj.get("result_amount").getAsInt() : 1;
        String tier = obj.get("tier").getAsString();
        int xpReward = obj.get("xp_reward").getAsInt();
        int coinReward = obj.get("coin_reward").getAsInt();
        int craftingTime = obj.get("crafting_time_seconds").getAsInt();
        String station = obj.has("required_station") ? obj.get("required_station").getAsString() : null;
        boolean unlockedByDefault = obj.has("unlocked_by_default") ? obj.get("unlocked_by_default").getAsBoolean() : false;

        Recipe recipe = new Recipe(id, name, description, resultItem, tier);
        recipe.setResultAmount(resultAmount);
        recipe.setExperienceReward(xpReward);
        recipe.setCoinReward(coinReward);
        recipe.setCraftingTimeSeconds(craftingTime);
        recipe.setRequiredStation(station);
        recipe.setUnlockedByDefault(unlockedByDefault);

        // Parse ingredients
        if (obj.has("ingredients")) {
            JsonArray ingredientsArray = obj.getAsJsonArray("ingredients");
            for (JsonElement element : ingredientsArray) {
                JsonObject ingredientObj = element.getAsJsonObject();
                String material = ingredientObj.get("material").getAsString();
                int amount = ingredientObj.get("amount").getAsInt();
                String type = ingredientObj.has("type") ? ingredientObj.get("type").getAsString() : "MATERIAL";
                recipe.addIngredient(material, amount, type);
            }
        }

        return recipe;
    }

    private void loadStations() {
        if (!configData.has("crafting_stations")) {
            plugin.getLogger().warning("No crafting stations found in crafting config");
            return;
        }

        JsonArray stationsArray = configData.getAsJsonArray("crafting_stations");
        for (JsonElement element : stationsArray) {
            JsonObject stationObj = element.getAsJsonObject();
            try {
                String id = stationObj.get("id").getAsString();
                String name = stationObj.get("name").getAsString();
                String description = stationObj.get("description").getAsString();
                
                CraftingStation station = new CraftingStation(id, name, description, null, 10.0);
                stations.put(id, station);
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing station: " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + stations.size() + " crafting stations");
    }

    // Getters
    public Recipe getRecipe(String id) {
        return recipes.get(id);
    }

    public Collection<Recipe> getAllRecipes() {
        return recipes.values();
    }

    public CraftingStation getStation(String id) {
        return stations.get(id);
    }

    public Collection<CraftingStation> getAllStations() {
        return stations.values();
    }

    public int getRecipeCount() {
        return recipes.size();
    }

    public int getStationCount() {
        return stations.size();
    }
}
