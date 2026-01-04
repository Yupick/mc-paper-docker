package com.nightslayer.mmorpg.repositories;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightslayer.mmorpg.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository para gestionar recetas de crafteo en SQLite
 */
public class CraftingRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public CraftingRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Inserta o actualiza una receta de crafteo
     */
    public void upsertRecipe(
            String id,
            String name,
            int requiredLevel,
            String ingredientsJson,
            String result,
            int costCoins,
            int costXp,
            boolean enabled
    ) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO crafting_recipes 
            (id, name, required_level, ingredients, result, cost_coins, cost_xp, enabled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setInt(3, requiredLevel);
            ps.setString(4, ingredientsJson);
            ps.setString(5, result);
            ps.setInt(6, costCoins);
            ps.setInt(7, costXp);
            ps.setInt(8, enabled ? 1 : 0);
            ps.executeUpdate();
        }
    }

    /**
     * Obtiene todas las recetas de crafteo desde SQLite
     */
    public List<JsonObject> getAllRecipes() {
        List<JsonObject> recipes = new ArrayList<>();
        String sql = """
            SELECT id, name, required_level, ingredients, result, cost_coins, cost_xp, enabled
            FROM crafting_recipes
            WHERE enabled = 1
            ORDER BY required_level ASC
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonObject recipe = new JsonObject();
                recipe.addProperty("id", rs.getString("id"));
                recipe.addProperty("name", rs.getString("name"));
                recipe.addProperty("required_level", rs.getInt("required_level"));
                
                // Parse ingredients JSON
                String ingredientsJson = rs.getString("ingredients");
                if (ingredientsJson != null && !ingredientsJson.isEmpty()) {
                    try {
                        recipe.add("ingredients", JsonParser.parseString(ingredientsJson));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error parsing ingredients JSON for recipe " + rs.getString("id"), e);
                    }
                }
                
                recipe.addProperty("result", rs.getString("result"));
                recipe.addProperty("cost_coins", rs.getInt("cost_coins"));
                recipe.addProperty("cost_xp", rs.getInt("cost_xp"));
                recipe.addProperty("enabled", rs.getInt("enabled") == 1);
                
                recipes.add(recipe);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cargar recetas desde SQLite", e);
        }

        return recipes;
    }

    /**
     * Obtiene una receta por su ID
     */
    public JsonObject getRecipeById(String id) throws SQLException {
        String sql = """
            SELECT id, name, required_level, ingredients, result, cost_coins, cost_xp, enabled
            FROM crafting_recipes
            WHERE id = ?
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JsonObject recipe = new JsonObject();
                    recipe.addProperty("id", rs.getString("id"));
                    recipe.addProperty("name", rs.getString("name"));
                    recipe.addProperty("required_level", rs.getInt("required_level"));
                    
                    String ingredientsJson = rs.getString("ingredients");
                    if (ingredientsJson != null && !ingredientsJson.isEmpty()) {
                        try {
                            recipe.add("ingredients", JsonParser.parseString(ingredientsJson));
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error parsing ingredients JSON", e);
                        }
                    }
                    
                    recipe.addProperty("result", rs.getString("result"));
                    recipe.addProperty("cost_coins", rs.getInt("cost_coins"));
                    recipe.addProperty("cost_xp", rs.getInt("cost_xp"));
                    recipe.addProperty("enabled", rs.getInt("enabled") == 1);
                    
                    return recipe;
                }
            }
        }
        
        return null;
    }

    /**
     * Cuenta el número de recetas en la BD
     */
    public int countRecipes() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM crafting_recipes WHERE enabled = 1";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        
        return 0;
    }

    /**
     * Inserta recetas por defecto si la tabla está vacía
     */
    public void insertDefaultRecipes() throws SQLException {
        int count = countRecipes();
        if (count > 0) {
            return; // Ya hay recetas
        }

        // Receta ejemplo: Espada de hierro mejorada
        upsertRecipe(
            "iron_sword_enhanced",
            "Espada de Hierro Mejorada",
            10,
            "[{\"item\":\"iron_ingot\",\"amount\":3},{\"item\":\"diamond\",\"amount\":1}]",
            "iron_sword",
            100,
            50,
            true
        );

        // Receta ejemplo: Poción de salud
        upsertRecipe(
            "health_potion_craft",
            "Poción de Salud",
            5,
            "[{\"item\":\"glass_bottle\",\"amount\":1},{\"item\":\"red_mushroom\",\"amount\":2}]",
            "potion",
            50,
            25,
            true
        );

        logger.info("✅ Insertadas 2 recetas por defecto en crafting_recipes");
    }
}
