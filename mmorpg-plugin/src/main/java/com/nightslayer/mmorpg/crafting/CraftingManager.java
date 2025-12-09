package com.nightslayer.mmorpg.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftingManager {
    private final JavaPlugin plugin;
    private final CraftingConfig config;
    private final Map<UUID, List<CraftingSession>> activeSessions;
    private final Map<UUID, Set<String>> unlockedRecipes;
    private final String dbPath;
    private static final String TABLE_CRAFTING_HISTORY = "crafting_history";
    private static final String TABLE_UNLOCKED_RECIPES = "unlocked_recipes";

    public CraftingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = new CraftingConfig(plugin);
        this.activeSessions = new ConcurrentHashMap<>();
        this.unlockedRecipes = new ConcurrentHashMap<>();
        this.dbPath = plugin.getDataFolder() + "/minecraft_rpg.db";
        initializeDatabase();
        loadUnlockedRecipes();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            String createHistoryTable = "CREATE TABLE IF NOT EXISTS " + TABLE_CRAFTING_HISTORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "recipe_id TEXT NOT NULL," +
                    "started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "completed_at TIMESTAMP," +
                    "ingredients_used TEXT," +
                    "result_item TEXT," +
                    "result_amount INTEGER," +
                    "experience_earned INTEGER," +
                    "coins_earned INTEGER" +
                    ")";

            String createUnlockedTable = "CREATE TABLE IF NOT EXISTS " + TABLE_UNLOCKED_RECIPES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "recipe_id TEXT NOT NULL," +
                    "unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(player_uuid, recipe_id)" +
                    ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createHistoryTable);
                stmt.execute(createUnlockedTable);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database initialization error: " + e.getMessage());
        }
    }

    private void loadUnlockedRecipes() {
        try (Connection conn = getConnection()) {
            String query = "SELECT player_uuid, recipe_id FROM " + TABLE_UNLOCKED_RECIPES;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                    String recipeId = rs.getString("recipe_id");
                    unlockedRecipes.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(recipeId);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading unlocked recipes: " + e.getMessage());
        }
    }

    public boolean startCrafting(UUID playerUUID, String recipeId, CraftingStation station) {
        Recipe recipe = config.getRecipe(recipeId);
        if (recipe == null) {
            return false;
        }

        // Verificar si el jugador conoce la receta
        if (!isRecipeUnlocked(playerUUID, recipeId) && !recipe.isUnlockedByDefault()) {
            return false;
        }

        // Verificar estación requerida
        if (recipe.getRequiresStation() != null && !recipe.getRequiresStation().equals(station.getId())) {
            return false;
        }

        // Crear sesión
        String sessionId = UUID.randomUUID().toString();
        CraftingSession session = new CraftingSession(sessionId, playerUUID, recipe, station, null);
        
        activeSessions.computeIfAbsent(playerUUID, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(session);

        // Guardar en base de datos
        saveCraftingSession(playerUUID, recipeId);
        
        return true;
    }

    public boolean completeCrafting(UUID playerUUID, String sessionId) {
        List<CraftingSession> sessions = activeSessions.get(playerUUID);
        if (sessions == null) return false;

        CraftingSession session = sessions.stream()
                .filter(s -> s.getSessionId().equals(sessionId) && s.isComplete())
                .findFirst()
                .orElse(null);

        if (session == null) return false;

        // Consumir ingredientes y dar resultado
        Recipe recipe = session.getRecipe();
        session.markComplete();

        // Guardar en base de datos
        completeCraftingSession(playerUUID, recipe.getId());
        sessions.remove(session);

        return true;
    }

    public void unlockRecipe(UUID playerUUID, String recipeId) {
        if (unlockedRecipes.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(recipeId)) {
            try (Connection conn = getConnection()) {
                String query = "INSERT OR IGNORE INTO " + TABLE_UNLOCKED_RECIPES + 
                        " (player_uuid, recipe_id) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, playerUUID.toString());
                    stmt.setString(2, recipeId);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error unlocking recipe: " + e.getMessage());
            }
        }
    }

    public boolean isRecipeUnlocked(UUID playerUUID, String recipeId) {
        Set<String> recipes = unlockedRecipes.get(playerUUID);
        return recipes != null && recipes.contains(recipeId);
    }

    private void saveCraftingSession(UUID playerUUID, String recipeId) {
        try (Connection conn = getConnection()) {
            String query = "INSERT INTO " + TABLE_CRAFTING_HISTORY + 
                    " (player_uuid, recipe_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, recipeId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving crafting session: " + e.getMessage());
        }
    }

    private void completeCraftingSession(UUID playerUUID, String recipeId) {
        try (Connection conn = getConnection()) {
            String query = "UPDATE " + TABLE_CRAFTING_HISTORY + 
                    " SET completed_at = CURRENT_TIMESTAMP WHERE player_uuid = ? AND recipe_id = ? AND completed_at IS NULL LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, recipeId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error completing crafting session: " + e.getMessage());
        }
    }

    public List<CraftingSession> getActiveSessions(UUID playerUUID) {
        return activeSessions.getOrDefault(playerUUID, new ArrayList<>());
    }

    public CraftingSession getSession(UUID playerUUID, String sessionId) {
        List<CraftingSession> sessions = activeSessions.get(playerUUID);
        if (sessions == null) return null;
        return sessions.stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

    public JsonObject getCraftingStats(UUID playerUUID) {
        JsonObject stats = new JsonObject();
        stats.addProperty("player_uuid", playerUUID.toString());
        stats.addProperty("recipes_unlocked", unlockedRecipes.getOrDefault(playerUUID, new HashSet<>()).size());
        stats.addProperty("total_recipes", config.getRecipeCount());
        stats.addProperty("active_sessions", getActiveSessions(playerUUID).size());

        try (Connection conn = getConnection()) {
            String query = "SELECT COUNT(*) as total, SUM(experience_earned) as total_xp, SUM(coins_earned) as total_coins FROM " + 
                    TABLE_CRAFTING_HISTORY + " WHERE player_uuid = ? AND completed_at IS NOT NULL";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.addProperty("completed_crafts", rs.getInt("total"));
                        stats.addProperty("total_experience", rs.getLong("total_xp"));
                        stats.addProperty("total_coins", rs.getLong("total_coins"));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error getting crafting stats: " + e.getMessage());
        }

        return stats;
    }

    public JsonArray getCraftingHistory(UUID playerUUID, int limit) {
        JsonArray history = new JsonArray();
        try (Connection conn = getConnection()) {
            String query = "SELECT recipe_id, started_at, completed_at, experience_earned, coins_earned FROM " + 
                    TABLE_CRAFTING_HISTORY + " WHERE player_uuid = ? ORDER BY started_at DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setInt(2, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        JsonObject entry = new JsonObject();
                        entry.addProperty("recipe_id", rs.getString("recipe_id"));
                        entry.addProperty("started_at", rs.getString("started_at"));
                        entry.addProperty("completed_at", rs.getString("completed_at"));
                        entry.addProperty("experience_earned", rs.getInt("experience_earned"));
                        entry.addProperty("coins_earned", rs.getLong("coins_earned"));
                        history.add(entry);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error getting crafting history: " + e.getMessage());
        }
        return history;
    }

    public Recipe getRecipe(String id) {
        return config.getRecipe(id);
    }

    public Collection<Recipe> getAllRecipes() {
        return config.getAllRecipes();
    }

    public CraftingStation getStation(String id) {
        return config.getStation(id);
    }

    public Collection<CraftingStation> getAllStations() {
        return config.getAllStations();
    }

    public void shutdown() {
        activeSessions.clear();
        unlockedRecipes.clear();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
}
