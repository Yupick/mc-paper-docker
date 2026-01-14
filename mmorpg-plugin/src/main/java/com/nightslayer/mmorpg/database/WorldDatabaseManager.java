package com.nightslayer.mmorpg.database;

import com.nightslayer.mmorpg.MMORPGPlugin;

import java.io.File;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor de base de datos SQLite local por mundo (worlds/active/data/world.db)
 * Complementa a DatabaseManager (universal.db) para datos específicos del mundo activo
 */
public class WorldDatabaseManager {
    private final MMORPGPlugin plugin;
    private final ConcurrentHashMap<String, Connection> worldConnections;
    private Connection activeWorldConnection;
    private String activeWorldName;
    private File activeWorldDatabaseFile;
    
    public WorldDatabaseManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.worldConnections = new ConcurrentHashMap<>();
    }
    
    /**
     * Obtiene o crea la conexión a la BD del mundo activo
     * Resuelve el symlink worlds/active y abre worlds/active/data/world.db
     */
    public Connection getWorldConnection() {
        try {
            File worldsDir = new File(plugin.getServer().getWorldContainer(), "active");
            
            // Resolver el symlink si existe
            File actualWorldDir = worldsDir.getCanonicalFile();
            if (!actualWorldDir.exists()) {
                plugin.getLogger().warning("Directorio del mundo activo no existe: " + actualWorldDir.getAbsolutePath());
                return null;
            }
            
            String worldName = actualWorldDir.getName();
            
            // Si el mundo cambió, cerrar la conexión anterior y abrir la nueva
            if (!worldName.equals(activeWorldName)) {
                if (activeWorldConnection != null && !activeWorldConnection.isClosed()) {
                    activeWorldConnection.close();
                }
                activeWorldName = worldName;
                activeWorldConnection = openWorldDatabase(actualWorldDir);
            }
            
            return activeWorldConnection;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error obteniendo conexión del mundo activo", e);
            return null;
        }
    }
    
    /**
     * Abre la BD de un mundo específico (worlds/<mundo>/data/world.db)
     */
    private Connection openWorldDatabase(File worldDir) {
        try {
            File dataDir = new File(worldDir, "data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            activeWorldDatabaseFile = new File(dataDir, "world.db");
            Class.forName("org.sqlite.JDBC");
            
            String url = "jdbc:sqlite:" + activeWorldDatabaseFile.getAbsolutePath();
            Connection conn = DriverManager.getConnection(url);
            
            plugin.getLogger().info("Conexión a BD del mundo activo: " + activeWorldDatabaseFile.getAbsolutePath());
            
            createWorldTables(conn);
            return conn;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error abriendo BD del mundo", e);
            return null;
        }
    }
    
    /**
     * Crea las tablas locales del mundo si no existen
     * Tablas específicas del mundo: kills, invasiones locales, logs, etc.
     */
    private void createWorldTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Tabla de kills del mundo
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS world_kills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    mob_id TEXT NOT NULL,
                    mob_name TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    location_x REAL,
                    location_y REAL,
                    location_z REAL,
                    loot TEXT
                )
            """);
            
            // Tabla de eventos del mundo
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS world_events_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    data TEXT
                )
            """);
            
            // Tabla de invasiones locales del mundo
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS world_invasion_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invasion_id TEXT NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER,
                    status TEXT NOT NULL,
                    mobs_spawned INTEGER,
                    mobs_killed INTEGER,
                    data TEXT
                )
            """);
            
            // Índices para mejor rendimiento
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_world_kills_player ON world_kills(player_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_world_events_timestamp ON world_events_log(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_world_invasion_id ON world_invasion_log(invasion_id)");
        }
    }
    
    /**
     * Ejecuta una sentencia de actualización en la BD del mundo activo
     */
    public int executeWorldUpdate(String sql, Object... params) {
        Connection conn = getWorldConnection();
        if (conn == null) return 0;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error ejecutando UPDATE en BD del mundo", e);
            return 0;
        }
    }
    
    /**
     * Ejecuta una consulta en la BD del mundo activo
     */
    public ResultSet executeWorldQuery(String sql, Object... params) {
        Connection conn = getWorldConnection();
        if (conn == null) return null;
        
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error ejecutando SELECT en BD del mundo", e);
            return null;
        }
    }
    
    /**
     * Cierra todas las conexiones de mundos
     */
    public void closeAll() {
        try {
            if (activeWorldConnection != null && !activeWorldConnection.isClosed()) {
                activeWorldConnection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error cerrando conexión del mundo", e);
        }
        
        for (Connection conn : worldConnections.values()) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error cerrando conexión de mundo", e);
            }
        }
        worldConnections.clear();
    }
    
    public String getActiveWorldName() {
        return activeWorldName;
    }
    
    public File getActiveWorldDatabaseFile() {
        return activeWorldDatabaseFile;
    }
    
    public boolean isWorldDatabaseAvailable() {
        return getWorldConnection() != null;
    }
}
