package com.nightslayer.mmorpg.repositories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
 * Repository para gestionar las definiciones de logros en SQLite
 */
public class AchievementRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public AchievementRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Inserta o actualiza una definición de logro
     */
    public void upsertAchievement(
            String id,
            String name,
            String description,
            String category,
            int points,
            String requirementsJson,
            String rewardsJson,
            String icon,
            int displayOrder,
            boolean hidden,
            boolean broadcastOnComplete,
            String broadcastMessage
    ) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO achievements_definitions 
            (id, name, description, category, points, requirements_json, rewards_json, icon, display_order, hidden, broadcast_on_complete, broadcast_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, category);
            ps.setInt(5, points);
            ps.setString(6, requirementsJson);
            ps.setString(7, rewardsJson);
            ps.setString(8, icon);
            ps.setInt(9, displayOrder);
            ps.setInt(10, hidden ? 1 : 0);
            ps.setInt(11, broadcastOnComplete ? 1 : 0);
            ps.setString(12, broadcastMessage);
            ps.executeUpdate();
        }
    }

    /**
     * Obtiene todas las definiciones de logros desde SQLite
     */
    public List<JsonObject> getAllAchievements() {
        List<JsonObject> achievements = new ArrayList<>();
        String sql = """
            SELECT id, name, description, category, points, requirements_json, rewards_json, 
                   icon, display_order, hidden, broadcast_on_complete, broadcast_message
            FROM achievements_definitions
            ORDER BY display_order ASC
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonObject achievement = new JsonObject();
                achievement.addProperty("id", rs.getString("id"));
                achievement.addProperty("name", rs.getString("name"));
                achievement.addProperty("description", rs.getString("description"));
                achievement.addProperty("category", rs.getString("category"));
                achievement.addProperty("points", rs.getInt("points"));
                
                // Parse JSON fields
                String requirementsJson = rs.getString("requirements_json");
                if (requirementsJson != null) {
                    achievement.add("requirements", com.google.gson.JsonParser.parseString(requirementsJson));
                }
                
                String rewardsJson = rs.getString("rewards_json");
                if (rewardsJson != null) {
                    achievement.add("rewards", com.google.gson.JsonParser.parseString(rewardsJson));
                }
                
                achievement.addProperty("icon", rs.getString("icon"));
                achievement.addProperty("display_order", rs.getInt("display_order"));
                achievement.addProperty("hidden", rs.getInt("hidden") == 1);
                achievement.addProperty("broadcast_on_complete", rs.getInt("broadcast_on_complete") == 1);
                achievement.addProperty("broadcast_message", rs.getString("broadcast_message"));
                
                achievements.add(achievement);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cargar logros desde SQLite", e);
        }

        return achievements;
    }

    /**
     * Obtiene un logro por su ID
     */
    public JsonObject getAchievementById(String id) throws SQLException {
        String sql = """
            SELECT id, name, description, category, points, requirements_json, rewards_json,
                   icon, display_order, hidden, broadcast_on_complete, broadcast_message
            FROM achievements_definitions
            WHERE id = ?
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JsonObject achievement = new JsonObject();
                    achievement.addProperty("id", rs.getString("id"));
                    achievement.addProperty("name", rs.getString("name"));
                    achievement.addProperty("description", rs.getString("description"));
                    achievement.addProperty("category", rs.getString("category"));
                    achievement.addProperty("points", rs.getInt("points"));
                    
                    String requirementsJson = rs.getString("requirements_json");
                    if (requirementsJson != null) {
                        achievement.add("requirements", com.google.gson.JsonParser.parseString(requirementsJson));
                    }
                    
                    String rewardsJson = rs.getString("rewards_json");
                    if (rewardsJson != null) {
                        achievement.add("rewards", com.google.gson.JsonParser.parseString(rewardsJson));
                    }
                    
                    achievement.addProperty("icon", rs.getString("icon"));
                    achievement.addProperty("display_order", rs.getInt("display_order"));
                    achievement.addProperty("hidden", rs.getInt("hidden") == 1);
                    achievement.addProperty("broadcast_on_complete", rs.getInt("broadcast_on_complete") == 1);
                    achievement.addProperty("broadcast_message", rs.getString("broadcast_message"));
                    
                    return achievement;
                }
            }
        }
        
        return null;
    }

    /**
     * Cuenta el número de logros en la BD
     */
    public int countAchievements() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM achievements_definitions";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        
        return 0;
    }
}
