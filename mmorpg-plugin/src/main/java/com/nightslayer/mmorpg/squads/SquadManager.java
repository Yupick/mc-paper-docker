package com.nightslayer.mmorpg.squads;

import com.google.gson.Gson;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.economy.EconomyManager;
import com.nightslayer.mmorpg.mobs.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SquadManager {
    private final MMORPGPlugin plugin;
    private final Gson gson;
    private Connection dbConnection;
    private SquadConfig config;
    private final Map<String, SquadSession> activeSessions;
    private BukkitTask autoSaveTask;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final File dbFile;
    private final File configFile;

    public SquadManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.activeSessions = new ConcurrentHashMap<>();
        this.dbFile = new File(plugin.getDataFolder(), "squads.db");
        this.configFile = new File(plugin.getDataFolder(), "squad_config.json");
        initializeDatabase();
        loadConfig();
        startAutoSave();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Error al conectar a base de datos de escuadras: " + e.getMessage());
        }
    }

    private void createTables() {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS squads (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "captain_uuid TEXT NOT NULL, " +
                "description TEXT, " +
                "level INTEGER DEFAULT 1, " +
                "treasury_coins BIGINT DEFAULT 0, " +
                "treasury_xp BIGINT DEFAULT 0, " +
                "max_members INTEGER DEFAULT 5, " +
                "created_at TEXT NOT NULL, " +
                "disbanded_at TEXT" +
            ")",
            "CREATE TABLE IF NOT EXISTS squad_members (" +
                "id TEXT PRIMARY KEY, " +
                "squad_id TEXT NOT NULL, " +
                "player_uuid TEXT NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "joined_at TEXT NOT NULL, " +
                "contributions_coins BIGINT DEFAULT 0, " +
                "contributions_xp BIGINT DEFAULT 0, " +
                "FOREIGN KEY(squad_id) REFERENCES squads(id)" +
            ")",
            "CREATE TABLE IF NOT EXISTS squad_treasury_history (" +
                "id TEXT PRIMARY KEY, " +
                "squad_id TEXT NOT NULL, " +
                "action TEXT NOT NULL, " +
                "amount BIGINT NOT NULL, " +
                "resource_type TEXT NOT NULL, " +
                "player_name TEXT, " +
                "timestamp TEXT NOT NULL, " +
                "FOREIGN KEY(squad_id) REFERENCES squads(id)" +
            ")",
            "CREATE TABLE IF NOT EXISTS squad_log (" +
                "id TEXT PRIMARY KEY, " +
                "squad_id TEXT NOT NULL, " +
                "event_type TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "player_name TEXT, " +
                "timestamp TEXT NOT NULL, " +
                "FOREIGN KEY(squad_id) REFERENCES squads(id)" +
            ")"
        };

        try (Statement stmt = dbConnection.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
            plugin.getLogger().info("Tablas de escuadras creadas/verificadas");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear tablas: " + e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            if (!configFile.exists()) {
                // Crear config por defecto
                config = new SquadConfig();
                try (Writer writer = new FileWriter(configFile)) {
                    gson.toJson(config, writer);
                }
                plugin.getLogger().info("squad_config.json creado por defecto en " + configFile.getPath());
                return;
            }

            String content = new String(Files.readAllBytes(configFile.toPath()));
            config = gson.fromJson(content, SquadConfig.class);
            if (config == null) {
                config = new SquadConfig();
            }
            plugin.getLogger().info("Configuración de escuadras cargada desde " + configFile.getPath());
        } catch (Exception e) {
            plugin.getLogger().severe("Error al cargar config: " + e.getMessage());
            config = new SquadConfig();
        }
    }

    private void startAutoSave() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveSessions();
        }, 1200L, 1200L);
    }

    public SquadSession createSquad(Player captain, String squadName, String description) {
        if (!config.squad_system.enabled) {
            return null;
        }

        EconomyManager economyManager = plugin.getEconomyManager();
        double captainBalance = economyManager.getBalance(captain);
        if (captainBalance < config.squad_system.economy.create_cost) {
            return null;
        }

        String squadId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            economyManager.withdraw(captain, config.squad_system.economy.create_cost);

            String insertSql = "INSERT INTO squads (id, name, captain_uuid, description, level, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(insertSql)) {
                pstmt.setString(1, squadId);
                pstmt.setString(2, squadName);
                pstmt.setString(3, captain.getUniqueId().toString());
                pstmt.setString(4, description);
                pstmt.setInt(5, 1);
                pstmt.setString(6, now.format(DATE_FORMAT));
                pstmt.executeUpdate();
            }

            addSquadMember(squadId, captain, SquadRole.CAPTAIN);

            SquadSession session = new SquadSession(squadId, squadName, captain.getUniqueId().toString(), 1);
            activeSessions.put(squadId, session);

            logSquadEvent(squadId, "SQUAD_CREATED", squadName + " creada por " + captain.getName(), captain.getName());

            return session;

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear escuadra: " + e.getMessage());
            return null;
        }
    }

    public void addSquadMember(String squadId, Player player, SquadRole role) {
        try {
            String memberId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();

            String insertSql = "INSERT INTO squad_members (id, squad_id, player_uuid, player_name, role, joined_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(insertSql)) {
                pstmt.setString(1, memberId);
                pstmt.setString(2, squadId);
                pstmt.setString(3, player.getUniqueId().toString());
                pstmt.setString(4, player.getName());
                pstmt.setString(5, role.name());
                pstmt.setString(6, now.format(DATE_FORMAT));
                pstmt.executeUpdate();
            }

            SquadSession session = activeSessions.get(squadId);
            if (session != null) {
                session.addMember(player.getUniqueId().toString(), player.getName(), role);
            }

            logSquadEvent(squadId, "MEMBER_JOINED", player.getName() + " se unió como " + role.name(), player.getName());

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al agregar miembro: " + e.getMessage());
        }
    }

    public void removeSquadMember(String squadId, String playerUuid) {
        try {
            String deleteSql = "DELETE FROM squad_members WHERE squad_id = ? AND player_uuid = ?";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(deleteSql)) {
                pstmt.setString(1, squadId);
                pstmt.setString(2, playerUuid);
                pstmt.executeUpdate();
            }

            SquadSession session = activeSessions.get(squadId);
            if (session != null) {
                session.removeMember(playerUuid);
            }

            Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
            if (player != null) {
                logSquadEvent(squadId, "MEMBER_REMOVED", player.getName() + " fue removido", null);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al remover miembro: " + e.getMessage());
        }
    }

    public void depositToTreasury(String squadId, long coins, String source) {
        try {
            String updateSql = "UPDATE squads SET treasury_coins = treasury_coins + ? WHERE id = ?";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(updateSql)) {
                pstmt.setLong(1, coins);
                pstmt.setString(2, squadId);
                pstmt.executeUpdate();
            }

            String historyId = UUID.randomUUID().toString();
            String historySql = "INSERT INTO squad_treasury_history (id, squad_id, action, amount, resource_type, player_name, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(historySql)) {
                pstmt.setString(1, historyId);
                pstmt.setString(2, squadId);
                pstmt.setString(3, "DEPOSIT");
                pstmt.setLong(4, coins);
                pstmt.setString(5, "COINS");
                pstmt.setString(6, source);
                pstmt.setString(7, LocalDateTime.now().format(DATE_FORMAT));
                pstmt.executeUpdate();
            }

            SquadSession session = activeSessions.get(squadId);
            if (session != null) {
                session.addTreasuryCoins(coins);
            }

            logSquadEvent(squadId, "TREASURY_DEPOSIT", source + " depositó " + coins + " monedas", source);

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al depositar en tesorería: " + e.getMessage());
        }
    }

    public void upgradeSquadLevel(String squadId) {
        try {
            String selectSql = "SELECT level, treasury_coins FROM squads WHERE id = ?";
            int currentLevel;
            long treasuryCoins;

            try (PreparedStatement pstmt = dbConnection.prepareStatement(selectSql)) {
                pstmt.setString(1, squadId);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) return;

                currentLevel = rs.getInt("level");
                treasuryCoins = rs.getLong("treasury_coins");
            }

            long upgradeCost = (long) (config.squad_system.economy.level_up_cost_base * 
                    Math.pow(config.squad_system.economy.level_up_cost_multiplier, currentLevel - 1));

            if (treasuryCoins < upgradeCost || currentLevel >= 5) {
                return;
            }

            String updateSql = "UPDATE squads SET level = level + 1, treasury_coins = treasury_coins - ? WHERE id = ?";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(updateSql)) {
                pstmt.setLong(1, upgradeCost);
                pstmt.setString(2, squadId);
                pstmt.executeUpdate();
            }

            SquadSession session = activeSessions.get(squadId);
            if (session != null) {
                session.setLevel(currentLevel + 1);
            }

            logSquadEvent(squadId, "SQUAD_UPGRADED", "Escuadra ascendió a nivel " + (currentLevel + 1), null);

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al subir nivel: " + e.getMessage());
        }
    }

    private void logSquadEvent(String squadId, String eventType, String description, String playerName) {
        try {
            String eventId = UUID.randomUUID().toString();
            String logSql = "INSERT INTO squad_log (id, squad_id, event_type, description, player_name, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(logSql)) {
                pstmt.setString(1, eventId);
                pstmt.setString(2, squadId);
                pstmt.setString(3, eventType);
                pstmt.setString(4, description);
                pstmt.setString(5, playerName);
                pstmt.setString(6, LocalDateTime.now().format(DATE_FORMAT));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al registrar evento: " + e.getMessage());
        }
    }

    public SquadSession getSquadSession(String squadId) {
        return activeSessions.get(squadId);
    }

    public Map<String, SquadSession> getAllActiveSessions() {
        return new HashMap<>(activeSessions);
    }

    private void saveSessions() {
        if (dbConnection == null) {
            return;
        }
        
        try {
            if (dbConnection.isClosed()) {
                plugin.getLogger().warning("Conexión a base de datos cerrada, no se pueden guardar sesiones");
                return;
            }
            
            try (PreparedStatement selectStmt = dbConnection.prepareStatement(
                    "SELECT id, level, treasury_coins FROM squads WHERE disbanded_at IS NULL")) {
                
                ResultSet rs = selectStmt.executeQuery();
                while (rs.next()) {
                    String squadId = rs.getString("id");
                    SquadSession session = activeSessions.get(squadId);
                    
                    if (session != null) {
                        String updateSql = "UPDATE squads SET level = ?, treasury_coins = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, session.getLevel());
                            updateStmt.setLong(2, session.getTreasuryCoins());
                            updateStmt.setString(3, squadId);
                            updateStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error al guardar sesiones: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        if (dbConnection != null) {
            try {
                if (!dbConnection.isClosed()) {
                    saveSessions();
                    dbConnection.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error al cerrar conexión de escuadras: " + e.getMessage());
            }
        }
    }

    public SquadConfig getConfig() {
        return config;
    }
}
