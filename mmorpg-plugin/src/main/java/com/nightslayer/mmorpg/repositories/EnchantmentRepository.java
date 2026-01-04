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
 * Repository para gestionar encantamientos en SQLite
 */
public class EnchantmentRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public EnchantmentRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Obtiene todos los encantamientos
     */
    public List<JsonObject> getAllEnchantments() {
        List<JsonObject> enchantments = new ArrayList<>();
        String sql = "SELECT * FROM enchantments ORDER BY name ASC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonObject enchantment = new JsonObject();
                enchantment.addProperty("id", rs.getString("id"));
                enchantment.addProperty("name", rs.getString("name"));
                enchantment.addProperty("type", rs.getString("type"));
                enchantment.addProperty("max_level", rs.getInt("max_level"));
                enchantment.addProperty("base_cost", rs.getInt("base_cost"));
                enchantment.addProperty("cost_per_level", rs.getInt("cost_per_level"));
                enchantment.addProperty("description", rs.getString("description"));
                
                enchantments.add(enchantment);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cargar encantamientos desde SQLite", e);
        }

        return enchantments;
    }

    /**
     * Cuenta el número de encantamientos en la BD
     */
    public int countEnchantments() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM enchantments";
        
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
     * Inserta o actualiza un encantamiento
     */
    public void upsertEnchantment(String id, String name, String type, int maxLevel, int baseCost, int costPerLevel, String description) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO enchantments 
            (id, name, type, max_level, base_cost, cost_per_level, description)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setInt(4, maxLevel);
            ps.setInt(5, baseCost);
            ps.setInt(6, costPerLevel);
            ps.setString(7, description);
            ps.executeUpdate();
        }
    }
}
