package com.nightslayer.mmorpg.enchanting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantmentManager {
    private final JavaPlugin plugin;
    private final Map<String, RPGEnchantment> enchantments;
    private final Map<UUID, List<EnchantmentSession>> activeSessions;
    private final Map<UUID, List<EnchantedItem>> playerEnchantedItems;
    private final JsonObject configData;
    private final Connection dbConnection;
    private static final String TABLE_ENCHANTMENT_HISTORY = "enchantment_history";
    private static final String TABLE_ENCHANTED_ITEMS = "enchanted_items";

    public EnchantmentManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enchantments = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.playerEnchantedItems = new ConcurrentHashMap<>();
        // Usar conexión de DatabaseManager en lugar de crear una propia
        com.nightslayer.mmorpg.MMORPGPlugin mmorpgPlugin = (com.nightslayer.mmorpg.MMORPGPlugin) plugin;
        this.dbConnection = mmorpgPlugin.getDatabaseManager().getConnection();
        this.configData = loadConfigFile();
        loadEnchantments();
        initializeDatabase();
    }

    private JsonObject loadConfigFile() {
        File configFile = new File(plugin.getDataFolder(), "enchanting_config.json");
        if (!configFile.exists()) {
            plugin.getLogger().warning("Enchanting config file not found: " + configFile.getPath());
            return new JsonObject();
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(configFile.getPath())));
            return com.google.gson.JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading enchanting config: " + e.getMessage());
            return new JsonObject();
        }
    }

    private void loadEnchantments() {
        if (!configData.has("enchantments")) {
            plugin.getLogger().warning("No enchantments found in config");
            return;
        }

        JsonArray enchantmentsArray = configData.getAsJsonArray("enchantments");
        for (JsonElement element : enchantmentsArray) {
            JsonObject enchObj = element.getAsJsonObject();
            try {
                RPGEnchantment enchantment = parseEnchantment(enchObj);
                enchantments.put(enchantment.getId(), enchantment);
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing enchantment: " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + enchantments.size() + " enchantments");
    }

    private RPGEnchantment parseEnchantment(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String name = obj.get("name").getAsString();
        String description = obj.get("description").getAsString();
        int maxLevel = obj.get("max_level").getAsInt();
        String rarity = obj.get("rarity").getAsString();

        // Parse applicable items
        Set<String> applicableItems = new HashSet<>();
        JsonArray itemsArray = obj.getAsJsonArray("applicable_items");
        for (JsonElement e : itemsArray) {
            applicableItems.add(e.getAsString());
        }

        // Parse incompatibilities
        Set<String> incompatible = new HashSet<>();
        JsonArray incompatArray = obj.getAsJsonArray("incompatible_with");
        for (JsonElement e : incompatArray) {
            incompatible.add(e.getAsString());
        }

        // Parse effects
        Map<Integer, Map<String, Double>> effects = new HashMap<>();
        JsonObject effectsObj = obj.getAsJsonObject("effects");
        for (String levelStr : effectsObj.keySet()) {
            int level = Integer.parseInt(levelStr);
            Map<String, Double> levelEffects = new HashMap<>();
            JsonObject levelEffectsObj = effectsObj.getAsJsonObject(levelStr);
            for (String effectKey : levelEffectsObj.keySet()) {
                levelEffects.put(effectKey, levelEffectsObj.get(effectKey).getAsDouble());
            }
            effects.put(level, levelEffects);
        }

        // Parse rarity by level
        Map<Integer, String> rarityByLevel = new HashMap<>();
        if (obj.has("rarity_by_level")) {
            JsonObject rarityObj = obj.getAsJsonObject("rarity_by_level");
            for (String levelStr : rarityObj.keySet()) {
                rarityByLevel.put(Integer.parseInt(levelStr), rarityObj.get(levelStr).getAsString());
            }
        }

        return new RPGEnchantment(id, name, description, maxLevel, applicableItems, 
                                 incompatible, effects, rarity, rarityByLevel);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection()) {
            String createHistoryTable = "CREATE TABLE IF NOT EXISTS " + TABLE_ENCHANTMENT_HISTORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "enchantment_id TEXT NOT NULL," +
                    "item_type TEXT NOT NULL," +
                    "level INTEGER," +
                    "cost_xp INTEGER," +
                    "cost_coins INTEGER," +
                    "status TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "completed_at TIMESTAMP" +
                    ")";

            String createItemsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_ENCHANTED_ITEMS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_uuid TEXT NOT NULL," +
                    "item_data TEXT NOT NULL," +
                    "enchantments_json TEXT NOT NULL," +
                    "experience_invested INTEGER," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createHistoryTable);
                stmt.execute(createItemsTable);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database initialization error: " + e.getMessage());
        }
    }

    public boolean applyEnchantment(UUID playerUUID, EnchantedItem item, String enchantmentId, 
                                   int targetLevel, int costXP, int costCoins) {
        RPGEnchantment enchantment = enchantments.get(enchantmentId);
        if (enchantment == null || targetLevel > enchantment.getMaxLevel()) {
            return false;
        }

        // Verificar incompatibilidades
        if (!item.canAddEnchantment(enchantmentId, enchantment.getIncompatibleWith())) {
            return false;
        }

        // Crear sesión
        String sessionId = UUID.randomUUID().toString();
        EnchantmentSession session = new EnchantmentSession(sessionId, playerUUID, item, 
                                                           enchantment, targetLevel, costXP, costCoins, 30);

        activeSessions.computeIfAbsent(playerUUID, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(session);

        // Guardar en BD
        saveEnchantmentSession(playerUUID, enchantmentId, item.getItem().getType().toString(), targetLevel, costXP, costCoins);

        return true;
    }

    public boolean completeEnchantment(UUID playerUUID, String sessionId) {
        List<EnchantmentSession> sessions = activeSessions.get(playerUUID);
        if (sessions == null) return false;

        EnchantmentSession session = sessions.stream()
                .filter(s -> s.getSessionId().equals(sessionId) && s.isComplete())
                .findFirst()
                .orElse(null);

        if (session == null) return false;

        // Aplicar encantamiento
        session.getItem().addEnchantment(session.getEnchantment().getId(), session.getTargetLevel());
        session.complete();

        // Guardar item
        playerEnchantedItems.computeIfAbsent(playerUUID, k -> new ArrayList<>())
                .add(session.getItem());

        completeEnchantmentSession(playerUUID, session.getEnchantment().getId(), "COMPLETED");
        sessions.remove(session);

        return true;
    }

    private void saveEnchantmentSession(UUID playerUUID, String enchantmentId, String itemType, 
                                       int level, int costXP, int costCoins) {
        try (Connection conn = getConnection()) {
            String query = "INSERT INTO " + TABLE_ENCHANTMENT_HISTORY + 
                    " (player_uuid, enchantment_id, item_type, level, cost_xp, cost_coins, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, enchantmentId);
                stmt.setString(3, itemType);
                stmt.setInt(4, level);
                stmt.setInt(5, costXP);
                stmt.setInt(6, costCoins);
                stmt.setString(7, "IN_PROGRESS");
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving enchantment session: " + e.getMessage());
        }
    }

    private void completeEnchantmentSession(UUID playerUUID, String enchantmentId, String status) {
        try (Connection conn = getConnection()) {
            String query = "UPDATE " + TABLE_ENCHANTMENT_HISTORY + 
                    " SET completed_at = CURRENT_TIMESTAMP, status = ? WHERE player_uuid = ? AND enchantment_id = ? AND status = 'IN_PROGRESS' LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, status);
                stmt.setString(2, playerUUID.toString());
                stmt.setString(3, enchantmentId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error completing enchantment session: " + e.getMessage());
        }
    }

    public List<EnchantmentSession> getActiveSessions(UUID playerUUID) {
        return activeSessions.getOrDefault(playerUUID, new ArrayList<>());
    }

    public RPGEnchantment getEnchantment(String id) {
        return enchantments.get(id);
    }

    public Collection<RPGEnchantment> getAllEnchantments() {
        return enchantments.values();
    }

    public Collection<RPGEnchantment> getEnchantmentsForItem(String itemType) {
        return enchantments.values().stream()
                .filter(e -> e.canApplyTo(itemType))
                .toList();
    }

    public int getEnchantmentCount() {
        return enchantments.size();
    }

    public void shutdown() {
        activeSessions.clear();
        playerEnchantedItems.clear();
    }

    private Connection getConnection() throws SQLException {
        return dbConnection;
    }
}
