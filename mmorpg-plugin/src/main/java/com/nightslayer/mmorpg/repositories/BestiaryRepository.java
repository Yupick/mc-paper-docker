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
 * Repository para gestionar el bestiario en SQLite
 */
public class BestiaryRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public BestiaryRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Obtiene todas las categorías del bestiario
     */
    public List<JsonObject> getAllCategories() {
        List<JsonObject> categories = new ArrayList<>();
        String sql = "SELECT * FROM bestiary ORDER BY category_order ASC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonObject category = new JsonObject();
                category.addProperty("id", rs.getString("id"));
                category.addProperty("name", rs.getString("name"));
                category.addProperty("description", rs.getString("description"));
                
                String mobsJson = rs.getString("mobs_json");
                if (mobsJson != null) {
                    try {
                        category.add("mobs", JsonParser.parseString(mobsJson));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error parsing mobs JSON", e);
                    }
                }
                
                String rewardsJson = rs.getString("tier_rewards_json");
                if (rewardsJson != null) {
                    try {
                        category.add("tier_rewards", JsonParser.parseString(rewardsJson));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error parsing rewards JSON", e);
                    }
                }
                
                categories.add(category);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cargar bestiario desde SQLite", e);
        }

        return categories;
    }

    /**
     * Cuenta el número de categorías en la BD
     */
    public int countCategories() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM bestiary";
        
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
     * Inserta o actualiza una categoría del bestiario
     */
    public void upsertCategory(String id, String name, String description, String mobsJson, String tierRewardsJson, int order) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO bestiary 
            (id, name, description, mobs_json, tier_rewards_json, category_order)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, mobsJson);
            ps.setString(5, tierRewardsJson);
            ps.setInt(6, order);
            ps.executeUpdate();
        }
    }
}
