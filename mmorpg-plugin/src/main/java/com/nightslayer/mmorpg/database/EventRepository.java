package com.nightslayer.mmorpg.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.*;
import java.util.*;

/**
 * EventRepository: Acceso a definiciones y estado de eventos
 */
public class EventRepository {
    private DatabaseManager dbManager;
    private static final Gson gson = new Gson();

    public EventRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Obtiene todas las definiciones de eventos
     */
    public Map<String, JsonObject> getAllEvents() throws Exception {
        Map<String, JsonObject> events = new HashMap<>();
        String query = """
                SELECT id, name, description, start_date, end_date, enabled, mobs_config, drops_config, zones
                FROM events
                """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                JsonObject eventObj = new JsonObject();
                eventObj.addProperty("id", rs.getString("id"));
                eventObj.addProperty("name", rs.getString("name"));
                eventObj.addProperty("description", rs.getString("description"));
                eventObj.addProperty("start_date", rs.getString("start_date"));
                eventObj.addProperty("end_date", rs.getString("end_date"));
                eventObj.addProperty("enabled", rs.getInt("enabled") == 1);
                
                // Parsear como arrays porque los guardamos así en la migración
                String mobsConfig = rs.getString("mobs_config");
                String dropsConfig = rs.getString("drops_config");
                String zonesConfig = rs.getString("zones");
                
                eventObj.add("mobs", mobsConfig != null && !mobsConfig.isEmpty() ? 
                    gson.fromJson(mobsConfig, JsonArray.class) : new JsonArray());
                eventObj.add("drops", dropsConfig != null && !dropsConfig.isEmpty() ? 
                    gson.fromJson(dropsConfig, JsonArray.class) : new JsonArray());
                eventObj.add("worlds", zonesConfig != null && !zonesConfig.isEmpty() ? 
                    gson.fromJson(zonesConfig, JsonArray.class) : new JsonArray());

                events.put(rs.getString("id"), eventObj);
            }
        }
        return events;
    }

    /**
     * Obtiene un evento específico
     */
    public JsonObject getEvent(String eventId) throws Exception {
        String query = "SELECT * FROM events WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, eventId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject eventObj = new JsonObject();
                    eventObj.addProperty("id", rs.getString("id"));
                    eventObj.addProperty("name", rs.getString("name"));
                    eventObj.addProperty("description", rs.getString("description"));
                    eventObj.addProperty("start_date", rs.getString("start_date"));
                    eventObj.addProperty("end_date", rs.getString("end_date"));
                    eventObj.addProperty("enabled", rs.getInt("enabled") == 1);
                    eventObj.add("mobs", gson.fromJson(rs.getString("mobs_config"), JsonObject.class));
                    eventObj.add("drops", gson.fromJson(rs.getString("drops_config"), JsonObject.class));
                    eventObj.add("zones", gson.fromJson(rs.getString("zones"), JsonObject.class));
                    return eventObj;
                }
            }
        }
        return null;
    }

    /**
     * Obtiene el estado de un evento en un mundo
     */
    public JsonObject getEventState(String eventId, String worldName) throws Exception {
        String query = """
                SELECT id, event_id, status, data, started_at, ended_at
                FROM event_state
                WHERE event_id = ? AND world = ?
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, eventId);
            pstmt.setString(2, worldName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject stateObj = new JsonObject();
                    stateObj.addProperty("id", rs.getInt("id"));
                    stateObj.addProperty("event_id", rs.getString("event_id"));
                    stateObj.addProperty("status", rs.getString("status"));
                    stateObj.add("data", gson.fromJson(rs.getString("data"), JsonObject.class));
                    stateObj.addProperty("started_at", rs.getString("started_at"));
                    stateObj.addProperty("ended_at", rs.getString("ended_at"));
                    return stateObj;
                }
            }
        }
        return null;
    }

    /**
     * Inicia un evento (crea estado en event_state)
     */
    public boolean startEvent(String eventId, String worldName) throws Exception {
        String query = """
                INSERT INTO event_state (event_id, world, status, data, started_at)
                VALUES (?, ?, 'ACTIVE', '{}', CURRENT_TIMESTAMP)
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, eventId);
            pstmt.setString(2, worldName);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Finaliza un evento
     */
    public boolean endEvent(String eventId, String worldName) throws Exception {
        String query = """
                UPDATE event_state
                SET status = 'ENDED', ended_at = CURRENT_TIMESTAMP
                WHERE event_id = ? AND world = ?
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, eventId);
            pstmt.setString(2, worldName);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Actualiza estado dinámico del evento (participantes, mobs, etc)
     */
    public boolean updateEventState(String eventId, String worldName, String stateData) throws Exception {
        String query = """
                UPDATE event_state
                SET data = ?, updated_at = CURRENT_TIMESTAMP
                WHERE event_id = ? AND world = ?
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, stateData);
            pstmt.setString(2, eventId);
            pstmt.setString(3, worldName);

            return pstmt.executeUpdate() > 0;
        }
    }
}
