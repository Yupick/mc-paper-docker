package com.nightslayer.mmorpg.repositories;

import com.nightslayer.mmorpg.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository para gestionar economía de jugadores en SQLite
 */
public class EconomyRepository {
    private final DatabaseManager databaseManager;
    private final Logger logger;

    public EconomyRepository(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Obtiene el balance de un jugador
     */
    public double getBalance(UUID playerUuid) {
        String sql = "SELECT balance FROM player_economy WHERE player_uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener balance del jugador", e);
        }

        return 0.0;
    }

    /**
     * Establece el balance de un jugador
     */
    public void setBalance(UUID playerUuid, double balance) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO player_economy 
            (player_uuid, balance, last_updated)
            VALUES (?, ?, CURRENT_TIMESTAMP)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate();
        }
    }

    /**
     * Añade o quita dinero del balance de un jugador
     */
    public void updateBalance(UUID playerUuid, double amount) throws SQLException {
        String sql = """
            INSERT INTO player_economy (player_uuid, balance, last_updated)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(player_uuid) DO UPDATE SET
                balance = balance + ?,
                last_updated = CURRENT_TIMESTAMP
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, amount);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        }
    }

    /**
     * Cuenta el número de jugadores en la BD
     */
    public int countPlayers() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM player_economy";
        
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
