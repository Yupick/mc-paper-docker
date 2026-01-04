package com.nightslayer.mmorpg.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.sql.*;
import java.util.*;

/**
 * PetRepository: Acceso a datos de mascotas desde SQLite
 */
public class PetRepository {
    private DatabaseManager dbManager;
    private static final Gson gson = new Gson();

    public PetRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Obtiene todas las mascotas definidas
     */
    public Map<String, JsonObject> getAllPets() throws Exception {
        Map<String, JsonObject> pets = new HashMap<>();
        String query = "SELECT id, name, type, rarity, description, base_stats, icon, adoption_cost_coins FROM pets";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                JsonObject petObj = new JsonObject();
                petObj.addProperty("id", rs.getString("id"));  // ← Agregar ID
                petObj.addProperty("name", rs.getString("name"));
                petObj.addProperty("type", rs.getString("type"));
                petObj.addProperty("rarity", rs.getString("rarity"));
                petObj.addProperty("description", rs.getString("description"));
                petObj.add("base_stats", gson.fromJson(rs.getString("base_stats"), JsonObject.class));
                petObj.addProperty("icon", rs.getString("icon"));
                petObj.addProperty("adoption_cost_coins", rs.getInt("adoption_cost_coins"));

                pets.put(rs.getString("id"), petObj);
            }
        }
        return pets;
    }

    /**
     * Obtiene una mascota específica por ID
     */
    public JsonObject getPet(String petId) throws Exception {
        String query = "SELECT * FROM pets WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, petId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject petObj = new JsonObject();
                    petObj.addProperty("id", rs.getString("id"));
                    petObj.addProperty("name", rs.getString("name"));
                    petObj.addProperty("type", rs.getString("type"));
                    petObj.addProperty("rarity", rs.getString("rarity"));
                    petObj.addProperty("description", rs.getString("description"));
                    petObj.add("base_stats", gson.fromJson(rs.getString("base_stats"), JsonObject.class));
                    petObj.addProperty("icon", rs.getString("icon"));
                    petObj.addProperty("adoption_cost_coins", rs.getInt("adoption_cost_coins"));
                    return petObj;
                }
            }
        }
        return null;
    }

    /**
     * Obtiene mascotas de un jugador (instancias adoptadas)
     */
    public List<JsonObject> getPlayerPets(String playerUUID, String worldName) throws Exception {
        List<JsonObject> playerPets = new ArrayList<>();
        String query = """
                SELECT pp.id, pp.pet_definition_id, pp.pet_name, pp.status, pp.stats, pp.learned_abilities
                FROM player_pets pp
                WHERE pp.player_uuid = ?
                """;

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, playerUUID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject petInstance = new JsonObject();
                    petInstance.addProperty("instance_id", rs.getInt("id"));
                    petInstance.addProperty("pet_definition_id", rs.getString("pet_definition_id"));
                    petInstance.addProperty("pet_name", rs.getString("pet_name"));
                    petInstance.addProperty("status", rs.getString("status"));
                    petInstance.add("stats", gson.fromJson(rs.getString("stats"), JsonObject.class));
                    petInstance.addProperty("learned_abilities", rs.getString("learned_abilities"));

                    playerPets.add(petInstance);
                }
            }
        }
        return playerPets;
    }

    /**
     * Adopta una mascota para un jugador
     */
    public boolean adoptPet(String playerUUID, String petDefinitionId, String petName, String worldName) throws Exception {
        String query = """
                INSERT INTO player_pets (player_uuid, pet_definition_id, pet_name, status, stats, learned_abilities)
                VALUES (?, ?, ?, 'ACTIVE', '{"health": 20, "experience": 0, "level": 1}', '[]')
                """;

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, playerUUID);
            pstmt.setString(2, petDefinitionId);
            pstmt.setString(3, petName);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Actualiza las estadísticas de una mascota
     */
    public boolean updatePetStats(int petInstanceId, String stats, String worldName) throws Exception {
        String query = "UPDATE player_pets SET stats = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = dbManager.getWorldConnection(worldName);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, stats);
            pstmt.setInt(2, petInstanceId);

            return pstmt.executeUpdate() > 0;
        }
    }
}
