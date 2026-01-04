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
 * Repository para gestionar escuadras/guilds en SQLite
 */
public class SquadRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public SquadRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Obtiene todas las escuadras
     */
    public List<JsonObject> getAllSquads() {
        List<JsonObject> squads = new ArrayList<>();
        String sql = "SELECT * FROM squads ORDER BY created_at DESC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JsonObject squad = new JsonObject();
                squad.addProperty("id", rs.getString("id"));
                squad.addProperty("name", rs.getString("name"));
                squad.addProperty("tag", rs.getString("tag"));
                squad.addProperty("leader_uuid", rs.getString("leader_uuid"));
                squad.addProperty("level", rs.getInt("level"));
                squad.addProperty("max_members", rs.getInt("max_members"));
                
                String membersJson = rs.getString("members_json");
                if (membersJson != null) {
                    try {
                        squad.add("members", JsonParser.parseString(membersJson));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error parsing members JSON", e);
                    }
                }
                
                squads.add(squad);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cargar escuadras desde SQLite", e);
        }

        return squads;
    }

    /**
     * Cuenta el número de escuadras en la BD
     */
    public int countSquads() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM squads";
        
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
     * Inserta o actualiza una escuadra
     */
    public void upsertSquad(String id, String name, String tag, String leaderUuid, int level, int maxMembers, String membersJson) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO squads 
            (id, name, tag, leader_uuid, level, max_members, members_json)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, tag);
            ps.setString(4, leaderUuid);
            ps.setInt(5, level);
            ps.setInt(6, maxMembers);
            ps.setString(7, membersJson);
            ps.executeUpdate();
        }
    }
}
