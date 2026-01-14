package com.nightslayer.mmorpg.database;

import com.nightslayer.mmorpg.MMORPGPlugin;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Gestor de base de datos SQLite para persistencia de datos
 */
public class DatabaseManager {
    private final MMORPGPlugin plugin;
    private Connection connection;
    private final ExecutorService executor;
    private final File databaseFile;
    
    public DatabaseManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(2);

        // Usar base de datos universal en config/data/universal.db (compartida)
        File pluginDir = plugin.getDataFolder(); // /server/plugins/MMORPGPlugin
        File serverDir = pluginDir.getParentFile().getParentFile(); // /server
        File configDir = new File(serverDir, "config");
        File dataDir = new File(configDir, "data");
        this.databaseFile = new File(dataDir, "universal.db");
        
        initializeDatabase();
    }
    
    /**
     * Inicializa la conexión a la base de datos
     */
    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            if (!databaseFile.exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            plugin.getLogger().info("Conexión a base de datos SQLite establecida: " + databaseFile.getAbsolutePath());
            plugin.getLogger().info("DEBUG: Archivo BD existe=" + databaseFile.exists() + ", size=" + databaseFile.length());
            
            createTables();
            
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error inicializando base de datos", e);
        }
    }
    
    /**
     * Crea las tablas necesarias si no existen
     */
    private void createTables() {
        plugin.getLogger().info("DEBUG: Iniciando createTables(), connection=" + (connection != null ? "OK" : "NULL"));
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                class_type TEXT,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                health REAL,
                max_health REAL,
                mana REAL,
                max_mana REAL,
                skill_points INTEGER DEFAULT 0,
                created_at INTEGER,
                last_login INTEGER
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_abilities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                ability_id TEXT NOT NULL,
                level INTEGER DEFAULT 1,
                last_used INTEGER DEFAULT 0,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid),
                UNIQUE(player_uuid, ability_id)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS quests (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                difficulty TEXT,
                min_level INTEGER,
                npc_giver_id TEXT,
                exp_reward INTEGER,
                money_reward REAL,
                skill_points_reward INTEGER,
                created_at INTEGER
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS quest_objectives (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                quest_id TEXT NOT NULL,
                objective_id TEXT NOT NULL,
                type TEXT NOT NULL,
                target TEXT,
                amount INTEGER,
                FOREIGN KEY (quest_id) REFERENCES quests(id),
                UNIQUE(quest_id, objective_id)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                quest_id TEXT NOT NULL,
                status TEXT NOT NULL,
                accepted_at INTEGER,
                completed_at INTEGER,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid),
                FOREIGN KEY (quest_id) REFERENCES quests(id),
                UNIQUE(player_uuid, quest_id)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_quest_progress (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                quest_id TEXT NOT NULL,
                objective_id TEXT NOT NULL,
                progress INTEGER DEFAULT 0,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid),
                FOREIGN KEY (quest_id) REFERENCES quests(id),
                UNIQUE(player_uuid, quest_id, objective_id)
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS npcs (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                world TEXT NOT NULL,
                x REAL NOT NULL,
                y REAL NOT NULL,
                z REAL NOT NULL,
                yaw REAL,
                pitch REAL,
                quest_id TEXT,
                dialogue TEXT,
                created_at INTEGER
            )
        """);
        
        executeUpdate("""
            CREATE TABLE IF NOT EXISTS economy_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                transaction_type TEXT NOT NULL,
                amount REAL NOT NULL,
                balance_after REAL NOT NULL,
                description TEXT,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid)
            )
        """);
        
        // Crear índices para mejorar rendimiento
        executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_class ON players(class_type)");
        executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_level ON players(level)");
        executeUpdate("CREATE INDEX IF NOT EXISTS idx_quest_difficulty ON quests(difficulty)");
        executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_quests_status ON player_quests(player_uuid, status)");
        executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_player ON economy_transactions(player_uuid)");
        
        plugin.getLogger().info("Tablas de base de datos creadas/verificadas");
    }
    
    /**
     * Ejecuta una consulta UPDATE/INSERT/DELETE de forma síncrona
     */
    public int executeUpdate(String sql, Object... params) {
        try {
            plugin.getLogger().info("DEBUG executeUpdate: sql=" + sql.substring(0, Math.min(50, sql.length())) + "..., params=" + params.length);
            
            if (params.length == 0) {
                // Sin parámetros - usar Statement directo para DDL (CREATE, ALTER, DROP, etc)
                try (Statement stmt = connection.createStatement()) {
                    boolean result = stmt.execute(sql);
                    plugin.getLogger().info("DEBUG: Execute result=" + result);
                    // Debug log para CREATE TABLE
                    if (sql.contains("CREATE TABLE")) {
                        plugin.getLogger().info("DEBUG: Ejecutado CREATE TABLE exitosamente");
                    }
                    return 0; // DDL no retorna row count significativo
                }
            } else {
                // Con parámetros - usar PreparedStatement
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                    return stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error ejecutando update: " + sql, e);
            return 0;
        }
    }
    
    /**
     * Ejecuta una consulta UPDATE/INSERT/DELETE de forma asíncrona
     */
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeUpdate(sql, params), executor);
    }
    
    /**
     * Ejecuta una consulta SELECT y devuelve el ResultSet
     */
    public ResultSet executeQuery(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error ejecutando query: " + sql, e);
            return null;
        }
    }
    
    /**
     * Ejecuta una consulta SELECT de forma asíncrona
     */
    public CompletableFuture<ResultSet> executeQueryAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> executeQuery(sql, params), executor);
    }
    
    /**
     * Obtiene la conexión a la base de datos
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error verificando conexión", e);
        }
        return connection;
    }
    
    /**
     * Cierra la conexión a la base de datos
     */
    public void close() {
        executor.shutdown();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexión a base de datos cerrada");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error cerrando base de datos", e);
        }
    }
    
    /**
     * Realiza backup de la base de datos
     */
    public void backup() {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        File backupFile = new File(backupDir, "rpgdata_" + timestamp + ".db");
        
        try {
            java.nio.file.Files.copy(
                databaseFile.toPath(),
                backupFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            plugin.getLogger().info("Backup de base de datos creado: " + backupFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creando backup de base de datos", e);
        }
    }
    
    /**
     * Obtiene estadísticas de la base de datos
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Estadísticas de Base de Datos ===\n");
        
        try {
            ResultSet rs = executeQuery("SELECT COUNT(*) FROM players");
            if (rs != null && rs.next()) {
                stats.append("Jugadores registrados: ").append(rs.getInt(1)).append("\n");
            }
            
            rs = executeQuery("SELECT COUNT(*) FROM quests");
            if (rs != null && rs.next()) {
                stats.append("Quests totales: ").append(rs.getInt(1)).append("\n");
            }
            
            rs = executeQuery("SELECT COUNT(*) FROM player_quests WHERE status = 'ACTIVE'");
            if (rs != null && rs.next()) {
                stats.append("Quests activas: ").append(rs.getInt(1)).append("\n");
            }
            
            rs = executeQuery("SELECT COUNT(*) FROM player_quests WHERE status = 'COMPLETED'");
            if (rs != null && rs.next()) {
                stats.append("Quests completadas: ").append(rs.getInt(1)).append("\n");
            }
            
            rs = executeQuery("SELECT COUNT(*) FROM npcs");
            if (rs != null && rs.next()) {
                stats.append("NPCs registrados: ").append(rs.getInt(1)).append("\n");
            }
            
            stats.append("Tamaño de BD: ").append(databaseFile.length() / 1024).append(" KB\n");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error obteniendo estadísticas", e);
        }
        
        return stats.toString();
    }

    /**
     * Obtiene conexión a base de datos específica de un mundo
     * Por ahora retorna la misma conexión universal
     */
    public Connection getWorldConnection(String worldName) {
        // TODO: Implementar BDs separadas por mundo si es necesario
        return connection;
    }
}
