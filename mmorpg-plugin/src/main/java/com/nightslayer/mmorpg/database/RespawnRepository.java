package com.nightslayer.mmorpg.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.sql.*;
import java.util.*;

/**
 * RespawnRepository: Acceso a datos de zonas de respawn
 */
public class RespawnRepository {
    private DatabaseManager dbManager;
    private static final Gson gson = new Gson();

    public RespawnRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Obtiene todas las zonas de respawn templates (definiciones globales)
     */
    public Map<String, JsonObject> getAllRespawnTemplates() throws Exception {
        Map<String, JsonObject> templates = new HashMap<>();
        String query = """
                SELECT id, world, name, type, location, mob_ids, max_mobs, respawn_interval_seconds, enabled
                FROM respawn_templates
                """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                JsonObject templateObj = new JsonObject();
                templateObj.addProperty("id", rs.getString("id"));
                templateObj.addProperty("world", rs.getString("world"));
                templateObj.addProperty("name", rs.getString("name"));
                templateObj.addProperty("type", rs.getString("type"));
                templateObj.add("location", gson.fromJson(rs.getString("location"), JsonObject.class));
                templateObj.addProperty("mob_ids", rs.getString("mob_ids"));
                templateObj.addProperty("max_mobs", rs.getInt("max_mobs"));
                templateObj.addProperty("respawn_interval_seconds", rs.getInt("respawn_interval_seconds"));
                templateObj.addProperty("enabled", rs.getInt("enabled") == 1);

                templates.put(rs.getString("id"), templateObj);
            }
        }
        return templates;
    }

    /**
     * Obtiene templates de respawn por mundo
     */
    public Map<String, JsonObject> getRespawnTemplatesByWorld(String worldName) throws Exception {
        Map<String, JsonObject> templates = new HashMap<>();
        String query = "SELECT * FROM respawn_templates WHERE world = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, worldName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject templateObj = new JsonObject();
                    templateObj.addProperty("id", rs.getString("id"));
                    templateObj.addProperty("name", rs.getString("name"));
                    templateObj.addProperty("type", rs.getString("type"));
                    templateObj.add("location", gson.fromJson(rs.getString("location"), JsonObject.class));
                    templateObj.addProperty("mob_ids", rs.getString("mob_ids"));
                    templateObj.addProperty("max_mobs", rs.getInt("max_mobs"));
                    templateObj.addProperty("respawn_interval_seconds", rs.getInt("respawn_interval_seconds"));
                    templateObj.addProperty("enabled", rs.getInt("enabled") == 1);

                    templates.put(rs.getString("id"), templateObj);
                }
            }
        }
        return templates;
    }

    /**
     * Obtiene zonas de respawn activas en un mundo
     */
    public Map<String, JsonObject> getRespawnZonesByWorld(String worldName) throws Exception {
        Map<String, JsonObject> zones = new HashMap<>();
        String query = """
                SELECT id, name, type, location, mob_ids, max_mobs, respawn_interval_seconds, last_respawn, enabled
                FROM respawn_zones
                WHERE world = ? AND enabled = 1
                """;

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, worldName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject zoneObj = new JsonObject();
                    zoneObj.addProperty("id", rs.getString("id"));
                    zoneObj.addProperty("name", rs.getString("name"));
                    zoneObj.addProperty("type", rs.getString("type"));
                    zoneObj.add("location", gson.fromJson(rs.getString("location"), JsonObject.class));
                    zoneObj.addProperty("mob_ids", rs.getString("mob_ids"));
                    zoneObj.addProperty("max_mobs", rs.getInt("max_mobs"));
                    zoneObj.addProperty("respawn_interval_seconds", rs.getInt("respawn_interval_seconds"));
                    zoneObj.addProperty("last_respawn", rs.getString("last_respawn"));

                    zones.put(rs.getString("id"), zoneObj);
                }
            }
        }
        return zones;
    }

    /**
     * Obtiene una zona de respawn específica
     */
    public JsonObject getRespawnZone(String zoneId, String worldName) throws Exception {
        String query = "SELECT * FROM respawn_zones WHERE id = ? AND world = ?";

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, zoneId);
            pstmt.setString(2, worldName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject zoneObj = new JsonObject();
                    zoneObj.addProperty("id", rs.getString("id"));
                    zoneObj.addProperty("name", rs.getString("name"));
                    zoneObj.addProperty("type", rs.getString("type"));
                    zoneObj.add("location", gson.fromJson(rs.getString("location"), JsonObject.class));
                    zoneObj.addProperty("mob_ids", rs.getString("mob_ids"));
                    zoneObj.addProperty("max_mobs", rs.getInt("max_mobs"));
                    zoneObj.addProperty("respawn_interval_seconds", rs.getInt("respawn_interval_seconds"));
                    zoneObj.addProperty("enabled", rs.getInt("enabled") == 1);
                    return zoneObj;
                }
            }
        }
        return null;
    }

    /**
     * Crea una nueva zona de respawn desde template
     */
    public boolean createRespawnZone(String zoneId, String templateId, String worldName, String location, String mobIds) throws Exception {
        String query = """
                INSERT INTO respawn_zones (id, template_id, world, name, type, location, mob_ids, max_mobs, respawn_interval_seconds, enabled)
                SELECT ?, id, ?, name, type, ?, ?, max_mobs, respawn_interval_seconds, 1
                FROM respawn_templates WHERE id = ?
                """;

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, zoneId);
            pstmt.setString(2, worldName);
            pstmt.setString(3, location);
            pstmt.setString(4, mobIds);
            pstmt.setString(5, templateId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Actualiza el tiempo del último respawn
     */
    public boolean updateLastRespawn(String zoneId, String worldName) throws Exception {
        String query = """
                UPDATE respawn_zones
                SET last_respawn = CURRENT_TIMESTAMP
                WHERE id = ? AND world = ?
                """;

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, zoneId);
            pstmt.setString(2, worldName);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Actualiza configuración de zona
     */
    public boolean updateRespawnZone(String zoneId, String worldName, String location, String mobIds, int maxMobs, int respawnInterval) throws Exception {
        String query = """
                UPDATE respawn_zones
                SET location = ?, mob_ids = ?, max_mobs = ?, respawn_interval_seconds = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND world = ?
                """;

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, location);
            pstmt.setString(2, mobIds);
            pstmt.setInt(3, maxMobs);
            pstmt.setInt(4, respawnInterval);
            pstmt.setString(5, zoneId);
            pstmt.setString(6, worldName);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Desactiva una zona
     */
    public boolean disableRespawnZone(String zoneId, String worldName) throws Exception {
        String query = "UPDATE respawn_zones SET enabled = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND world = ?";

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, zoneId);
            pstmt.setString(2, worldName);

            return pstmt.executeUpdate() > 0;
        }
    }
}
