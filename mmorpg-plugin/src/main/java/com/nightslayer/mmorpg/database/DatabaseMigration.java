package com.nightslayer.mmorpg.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nightslayer.mmorpg.MMORPGPlugin;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.*;

/**
 * DatabaseMigration: Migra datos JSON → SQLite en primera ejecución
 * Ejecuta en MainPlugin.onEnable() para inicializar las tablas y cargar datos históricos
 */
public class DatabaseMigration {
    private static final Gson gson = new Gson();

    /**
     * Migra todos los datos JSON → SQLite
     * Llamar en MainPlugin.onEnable() después de crear DatabaseManager
     */
    public static void migrate(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            plugin.getLogger().info("🔄 [DatabaseMigration] Iniciando migración...");
            
            // Crear tablas si no existen
            plugin.getLogger().info("🔄 [DatabaseMigration] Creando tablas...");
            createTablesSQLite(dbManager);
            plugin.getLogger().info("✅ [DatabaseMigration] Tablas creadas correctamente");
            
            // Verificar si ya hay datos en universal.db
            boolean petsHaveData = hasTableData(dbManager.getConnection(), "pets");
            boolean achievementsHaveData = hasTableData(dbManager.getConnection(), "achievements_definitions");
            boolean craftingHaveData = hasTableData(dbManager.getConnection(), "crafting_recipes");
            boolean bestiaryHaveData = hasTableData(dbManager.getConnection(), "bestiary");
            boolean enchantmentsHaveData = hasTableData(dbManager.getConnection(), "enchantments");
            boolean squadsHaveData = hasTableData(dbManager.getConnection(), "squads");
            
            if (petsHaveData && achievementsHaveData && craftingHaveData && bestiaryHaveData && enchantmentsHaveData && squadsHaveData) {
                // Todas las tablas ya tienen datos, no migrar
                plugin.getLogger().info("✓ Tablas ya contienen datos, no migrar");
                return;
            }
            
            // Migrar cada tipo de config (solo si la tabla está vacía)
            plugin.getLogger().info("🔄 Iniciando migración JSON → SQLite...");
            if (!petsHaveData) {
                migratePets(dataDirectory, dbManager, plugin);
            }
            migrateEvents(dataDirectory, dbManager, plugin);
            migrateRespawnTemplates(dataDirectory, dbManager, plugin);
            if (!enchantmentsHaveData) {
                migrateEnchantments(dataDirectory, dbManager, plugin);
            }
            if (!craftingHaveData) {
                migrateCraftingRecipes(dataDirectory, dbManager, plugin);
            }
            if (!achievementsHaveData) {
                migrateAchievements(dataDirectory, dbManager, plugin);
            }
            if (!bestiaryHaveData) {
                migrateBestiary(dataDirectory, dbManager, plugin);
            }
            if (!squadsHaveData) {
                migrateSquads(dataDirectory, dbManager, plugin);
            }
            migrateEconomy(dataDirectory, dbManager, plugin);
            
            plugin.getLogger().info("✅ Migración completada correctamente");
            
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error durante migración: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crea todas las tablas en SQLite
     */
    private static void createTablesSQLite(DatabaseManager dbManager) throws Exception {
        String[] createTableStatements = {
            "CREATE TABLE IF NOT EXISTS pets (id TEXT PRIMARY KEY, name TEXT NOT NULL, type TEXT, rarity TEXT, description TEXT, base_stats TEXT, icon TEXT, adoption_cost_coins INTEGER, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS pet_mounts (id TEXT PRIMARY KEY, name TEXT, description TEXT, stats TEXT, icon TEXT, cost_coins INTEGER)",
            "CREATE TABLE IF NOT EXISTS pet_abilities (id TEXT PRIMARY KEY, pet_id TEXT, name TEXT, effect TEXT)",
            "CREATE TABLE IF NOT EXISTS pet_settings (id INTEGER PRIMARY KEY, auto_heal_enabled INTEGER DEFAULT 1, max_pets_per_player INTEGER DEFAULT 3)",
            "CREATE TABLE IF NOT EXISTS player_pets (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT, pet_definition_id TEXT, pet_name TEXT, status TEXT, stats TEXT, learned_abilities TEXT, world TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS events (id TEXT PRIMARY KEY, name TEXT, description TEXT, start_date TEXT, end_date TEXT, enabled INTEGER, mobs_config TEXT, drops_config TEXT, zones TEXT)",
            "CREATE TABLE IF NOT EXISTS event_state (id INTEGER PRIMARY KEY AUTOINCREMENT, event_id TEXT, world TEXT, status TEXT, data TEXT, started_at TIMESTAMP, ended_at TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS respawn_templates (id TEXT PRIMARY KEY, world TEXT, name TEXT, type TEXT, location TEXT, mob_ids TEXT, max_mobs INTEGER, respawn_interval_seconds INTEGER, enabled INTEGER)",
            "CREATE TABLE IF NOT EXISTS respawn_zones (id TEXT PRIMARY KEY, template_id TEXT, world TEXT, name TEXT, type TEXT, location TEXT, mob_ids TEXT, max_mobs INTEGER, respawn_interval_seconds INTEGER, last_respawn TIMESTAMP, enabled INTEGER)",
            "CREATE TABLE IF NOT EXISTS enchantments (id TEXT PRIMARY KEY, name TEXT, type TEXT, max_level INTEGER, base_cost INTEGER, cost_per_level INTEGER, description TEXT)",
            "CREATE TABLE IF NOT EXISTS crafting_recipes (id TEXT PRIMARY KEY, name TEXT, required_level INTEGER, ingredients TEXT, result TEXT, cost_coins INTEGER, cost_xp INTEGER, enabled INTEGER)",
            "CREATE TABLE IF NOT EXISTS achievements_definitions (id TEXT PRIMARY KEY, name TEXT, description TEXT, category TEXT, points INTEGER, requirements_json TEXT, rewards_json TEXT, icon TEXT, display_order INTEGER, hidden INTEGER, broadcast_on_complete INTEGER, broadcast_message TEXT)",
            "CREATE TABLE IF NOT EXISTS bestiary (id TEXT PRIMARY KEY, name TEXT, description TEXT, mobs_json TEXT, tier_rewards_json TEXT, category_order INTEGER)",
            "CREATE TABLE IF NOT EXISTS squads (id TEXT PRIMARY KEY, name TEXT, tag TEXT, leader_uuid TEXT, level INTEGER DEFAULT 1, max_members INTEGER DEFAULT 10, members_json TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_economy (player_uuid TEXT PRIMARY KEY, balance REAL DEFAULT 0.0, last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_achievements (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT, achievement_id TEXT, completed_at TIMESTAMP, world TEXT)",
            "CREATE TABLE IF NOT EXISTS player_enchanted_items (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT, item_id TEXT, enchantment_id TEXT, level INTEGER, world TEXT)",
            "CREATE TABLE IF NOT EXISTS player_crafting_history (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT, recipe_id TEXT, crafted_at TIMESTAMP, world TEXT)"
        };
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : createTableStatements) {
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    // Tabla ya existe, es seguro ignorar
                }
            }
        }
    }

    /**
     * Verifica si una tabla tiene datos
     */
    private static boolean hasTableData(Connection conn, String tableName) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            // Tabla no existe
            return false;
        }
        return false;
    }

    /**
     * Migra mascotas de pets.json (en data/)
     */
    private static void migratePets(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            File configFile = new File(dataDirectory, "data/pets.json");
            if (!configFile.exists()) {
                plugin.getLogger().info("Archivo pets.json no encontrado, saltando migración");
                return;
            }
            
            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);
            if (!config.has("pets")) {
                plugin.getLogger().info("No se encontró campo 'pets' en pets.json");
                return;
            }
            
            JsonArray petsArray = config.getAsJsonArray("pets");
            String insertSQL = """
                    INSERT OR IGNORE INTO pets (id, name, type, rarity, description, base_stats, icon, adoption_cost_coins)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                conn.setAutoCommit(false);  // Iniciar transacción
                
                int count = 0;
                for (JsonElement elem : petsArray) {
                    JsonObject petData = elem.getAsJsonObject();
                    String petId = petData.has("id") ? petData.get("id").getAsString() : "unknown_" + count;
                    
                    pstmt.setString(1, petId);
                    pstmt.setString(2, petData.has("name") ? petData.get("name").getAsString() : petId);
                    pstmt.setString(3, petData.has("type") ? petData.get("type").getAsString() : "UNKNOWN");
                    pstmt.setString(4, petData.has("rarity") ? petData.get("rarity").getAsString() : "COMMON");
                    pstmt.setString(5, petData.has("description") ? petData.get("description").getAsString() : "");
                    pstmt.setString(6, petData.has("base_stats") ? petData.getAsJsonObject("base_stats").toString() : "{}");
                    pstmt.setString(7, petData.has("icon") ? petData.get("icon").getAsString() : "");
                    pstmt.setInt(8, petData.has("adoption_cost") ? petData.get("adoption_cost").getAsInt() : 0);
                    pstmt.addBatch();
                    count++;
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();  // Commit explícito
                plugin.getLogger().info("✓ Migraron " + results.length + " mascotas");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error migrando mascotas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migra eventos de events_config.json
     */
    private static void migrateEvents(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            // events.json usa JsonArray en vez de objeto (en data/)
            File configFile = new File(dataDirectory, "data/events.json");
            if (!configFile.exists()) return;

            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);
            if (!config.has("events")) return;

            JsonArray eventsArray = config.getAsJsonArray("events");
            String insertSQL = """
                    INSERT INTO events (id, name, description, start_date, end_date, enabled, mobs_config, drops_config, zones)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

                for (JsonElement elem : eventsArray) {
                    JsonObject eventData = elem.getAsJsonObject();
                    String eventId = eventData.has("id") ? eventData.get("id").getAsString() : UUID.randomUUID().toString();

                    pstmt.setString(1, eventId);
                    pstmt.setString(2, eventData.has("name") ? eventData.get("name").getAsString() : eventId);
                    pstmt.setString(3, eventData.has("description") ? eventData.get("description").getAsString() : "");
                    pstmt.setString(4, eventData.has("start_date") ? eventData.get("start_date").getAsString() : "");
                    pstmt.setString(5, eventData.has("end_date") ? eventData.get("end_date").getAsString() : "");
                    pstmt.setInt(6, eventData.has("enabled") && eventData.get("enabled").getAsBoolean() ? 1 : 0);

                    // Guardar listas completas como JSON strings
                    pstmt.setString(7, eventData.has("mobs") ? eventData.getAsJsonArray("mobs").toString() : "[]");
                    pstmt.setString(8, eventData.has("drops") ? eventData.getAsJsonArray("drops").toString() : "[]");
                    pstmt.setString(9, eventData.has("worlds") ? eventData.getAsJsonArray("worlds").toString() : "[]");
                    pstmt.addBatch();
                }

                int[] results = pstmt.executeBatch();
                plugin.getLogger().info("✓ Migraron " + results.length + " eventos");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error migrando eventos: " + e.getMessage());
        }
    }

    /**
     * Migra zonas de respawn de respawn_config.json
     */
    private static void migrateRespawnTemplates(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            // Buscar primero en data/, si no existe probar en carpeta padre
            File configFile = new File(dataDirectory, "respawn_config.json");
            if (!configFile.exists()) {
                configFile = new File(new File(dataDirectory).getParentFile(), "respawn_config.json");
            }
            if (!configFile.exists()) return;

            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);

            // Nuevos archivos usan "respawn_points" con un objeto; si viene "respawnZones" también lo soportamos
            Map<String, JsonObject> zonesMap = new LinkedHashMap<>();
            if (config.has("respawnZones")) {
                JsonObject zonesObj = config.getAsJsonObject("respawnZones");
                for (String zoneId : zonesObj.keySet()) {
                    zonesMap.put(zoneId, zonesObj.getAsJsonObject(zoneId));
                }
            }
            if (config.has("respawn_points")) {
                JsonObject pointsObj = config.getAsJsonObject("respawn_points");
                for (String zoneId : pointsObj.keySet()) {
                    zonesMap.put(zoneId, pointsObj.getAsJsonObject(zoneId));
                }
            }
            if (zonesMap.isEmpty()) return;
            
            String insertSQL = """
                    INSERT INTO respawn_templates (id, world, name, type, location, mob_ids, max_mobs, respawn_interval_seconds, enabled)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                for (Map.Entry<String, JsonObject> entry : zonesMap.entrySet()) {
                    String zoneId = entry.getKey();
                    JsonObject zoneData = entry.getValue();
                    pstmt.setString(1, zoneId);
                    pstmt.setString(2, zoneData.has("world") ? zoneData.get("world").getAsString() : "world");
                    pstmt.setString(3, zoneData.has("name") ? zoneData.get("name").getAsString() : zoneId);
                    pstmt.setString(4, zoneData.has("type") ? zoneData.get("type").getAsString() : "FARMEO");
                    // Si no hay objeto location pero hay x/y/z, construir uno sencillo
                    if (!zoneData.has("location") && zoneData.has("x")) {
                        JsonObject loc = new JsonObject();
                        loc.addProperty("x", zoneData.get("x").getAsDouble());
                        loc.addProperty("y", zoneData.get("y").getAsDouble());
                        loc.addProperty("z", zoneData.get("z").getAsDouble());
                        loc.addProperty("pitch", zoneData.has("pitch") ? zoneData.get("pitch").getAsFloat() : 0f);
                        loc.addProperty("yaw", zoneData.has("yaw") ? zoneData.get("yaw").getAsFloat() : 0f);
                        zoneData.add("location", loc);
                    }
                    pstmt.setString(5, zoneData.has("location") ? zoneData.getAsJsonObject("location").toString() : "{}");
                    pstmt.setString(6, zoneData.has("mobIds") ? String.join(",", 
                            gson.fromJson(zoneData.get("mobIds"), String[].class)) : "");
                    pstmt.setInt(7, zoneData.has("maxMobs") ? zoneData.get("maxMobs").getAsInt() : 10);
                    pstmt.setInt(8, zoneData.has("respawnInterval") ? zoneData.get("respawnInterval").getAsInt() : 300);
                    pstmt.setInt(9, zoneData.has("enabled") && zoneData.get("enabled").getAsBoolean() ? 1 : 0);
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                plugin.getLogger().info("✓ Migraron " + results.length + " zonas de respawn");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error migrando respawn: " + e.getMessage());
        }
    }

    /**
     * Migra recetas de crafting de crafting_config.json
     * Si no existe JSON o está vacío, genera recetas por defecto
     */
    private static void migrateCraftingRecipes(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            File configFile = new File(dataDirectory, "crafting_config.json");
            if (!configFile.exists()) {
                configFile = new File(new File(dataDirectory).getParentFile(), "crafting_config.json");
            }

            boolean hasJsonData = false;
            JsonArray recipesArray = new JsonArray();
            
            if (configFile.exists()) {
                JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);
                if (config.has("recipes")) {
                    recipesArray = config.getAsJsonArray("recipes");
                    hasJsonData = recipesArray.size() > 0;
                }
            }
            
            // Si no hay datos JSON, generar recetas por defecto
            if (!hasJsonData) {
                plugin.getLogger().info("📝 Generando recetas de crafting por defecto...");
                generateDefaultCraftingRecipes(dbManager, plugin);
                return;
            }
            
            // Migrar desde JSON
            String insertSQL = """
                    INSERT INTO crafting_recipes (id, name, required_level, ingredients, result, cost_coins, cost_xp, enabled)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                for (JsonElement elem : recipesArray) {
                    JsonObject recipeData = elem.getAsJsonObject();
                    String recipeId = recipeData.has("id") ? recipeData.get("id").getAsString() : UUID.randomUUID().toString();
                    pstmt.setString(1, recipeId);
                    pstmt.setString(2, recipeData.has("name") ? recipeData.get("name").getAsString() : recipeId);
                    pstmt.setInt(3, recipeData.has("required_level") ? recipeData.get("required_level").getAsInt() : 1);
                    pstmt.setString(4, recipeData.has("ingredients") ? recipeData.get("ingredients").toString() : "[]");
                    pstmt.setString(5, recipeData.has("result") ? recipeData.get("result").getAsString() : "minecraft:stone");
                    pstmt.setInt(6, recipeData.has("cost_coins") ? recipeData.get("cost_coins").getAsInt() : 0);
                    pstmt.setInt(7, recipeData.has("cost_xp") ? recipeData.get("cost_xp").getAsInt() : 0);
                    pstmt.setInt(8, (!recipeData.has("enabled") || recipeData.get("enabled").getAsBoolean()) ? 1 : 0);
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                plugin.getLogger().info("✅ Migradas " + results.length + " recetas de crafting desde JSON");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error migrando crafting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Genera recetas de crafting por defecto
     */
    private static void generateDefaultCraftingRecipes(DatabaseManager dbManager, MMORPGPlugin plugin) {
        String insertSQL = """
                INSERT INTO crafting_recipes (id, name, required_level, ingredients, result, cost_coins, cost_xp, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            // Receta 1: Espada de Hierro Mejorada
            pstmt.setString(1, "iron_sword_enhanced");
            pstmt.setString(2, "Espada de Hierro Mejorada");
            pstmt.setInt(3, 10);
            pstmt.setString(4, "[{\"item\":\"iron_ingot\",\"amount\":3},{\"item\":\"diamond\",\"amount\":1}]");
            pstmt.setString(5, "iron_sword");
            pstmt.setInt(6, 100);
            pstmt.setInt(7, 50);
            pstmt.setInt(8, 1);
            pstmt.addBatch();
            
            // Receta 2: Poción de Salud
            pstmt.setString(1, "health_potion_craft");
            pstmt.setString(2, "Poción de Salud");
            pstmt.setInt(3, 5);
            pstmt.setString(4, "[{\"item\":\"glass_bottle\",\"amount\":1},{\"item\":\"red_mushroom\",\"amount\":2}]");
            pstmt.setString(5, "potion");
            pstmt.setInt(6, 50);
            pstmt.setInt(7, 25);
            pstmt.setInt(8, 1);
            pstmt.addBatch();
            
            // Receta 3: Armadura Reforzada
            pstmt.setString(1, "reinforced_chestplate");
            pstmt.setString(2, "Pechera Reforzada");
            pstmt.setInt(3, 15);
            pstmt.setString(4, "[{\"item\":\"iron_chestplate\",\"amount\":1},{\"item\":\"diamond\",\"amount\":2}]");
            pstmt.setString(5, "diamond_chestplate");
            pstmt.setInt(6, 200);
            pstmt.setInt(7, 100);
            pstmt.setInt(8, 1);
            pstmt.addBatch();
            
            int[] results = pstmt.executeBatch();
            conn.commit();
            plugin.getLogger().info("✅ Generadas " + results.length + " recetas de crafting por defecto");
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error generando recetas por defecto: " + e.getMessage());
        }
    }

    /**
     * Migra logros de data/achievements.json
     */
    private static void migrateAchievements(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            File configFile = new File(dataDirectory, "data/achievements.json");
            if (!configFile.exists()) {
                plugin.getLogger().info("Archivo achievements.json no encontrado, saltando migración");
                return;
            }
            
            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);
            if (!config.has("achievements")) {
                plugin.getLogger().info("No se encontró campo 'achievements' en achievements.json");
                return;
            }
            
            JsonArray achievementsArray = config.getAsJsonArray("achievements");
            if (achievementsArray.size() == 0) {
                plugin.getLogger().info("Array de logros vacío, saltando migración");
                return;
            }
            
            String insertSQL = """
                    INSERT INTO achievements_definitions 
                    (id, name, description, category, points, requirements_json, rewards_json, 
                     icon, display_order, hidden, broadcast_on_complete, broadcast_message)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                for (JsonElement elem : achievementsArray) {
                    JsonObject achData = elem.getAsJsonObject();
                    
                    String id = achData.has("id") ? achData.get("id").getAsString() : "unknown";
                    String name = achData.has("name") ? achData.get("name").getAsString() : "";
                    String description = achData.has("description") ? achData.get("description").getAsString() : "";
                    String category = achData.has("category") ? achData.get("category").getAsString() : "GENERAL";
                    int points = achData.has("points") ? achData.get("points").getAsInt() : 0;
                    
                    // Serializar requirements y rewards como JSON strings
                    String requirementsJson = achData.has("requirements") ? achData.get("requirements").toString() : "{}";
                    String rewardsJson = achData.has("rewards") ? achData.get("rewards").toString() : "{}";
                    
                    String icon = achData.has("icon") ? achData.get("icon").getAsString() : "BOOK";
                    int displayOrder = achData.has("display_order") ? achData.get("display_order").getAsInt() : 999;
                    boolean hidden = achData.has("hidden") && achData.get("hidden").getAsBoolean();
                    boolean broadcastOnComplete = achData.has("broadcast_on_complete") && achData.get("broadcast_on_complete").getAsBoolean();
                    String broadcastMessage = achData.has("broadcast_message") ? achData.get("broadcast_message").getAsString() : "";
                    
                    pstmt.setString(1, id);
                    pstmt.setString(2, name);
                    pstmt.setString(3, description);
                    pstmt.setString(4, category);
                    pstmt.setInt(5, points);
                    pstmt.setString(6, requirementsJson);
                    pstmt.setString(7, rewardsJson);
                    pstmt.setString(8, icon);
                    pstmt.setInt(9, displayOrder);
                    pstmt.setInt(10, hidden ? 1 : 0);
                    pstmt.setInt(11, broadcastOnComplete ? 1 : 0);
                    pstmt.setString(12, broadcastMessage);
                    
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                plugin.getLogger().info("✅ Migrados " + results.length + " logros desde achievements.json");
                
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error migrando achievements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Migra bestiario de data/bestiary.json o genera por defecto
     */
    private static void migrateBestiary(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            File configFile = new File(dataDirectory, "data/bestiary.json");
            if (!configFile.exists()) {
                plugin.getLogger().info("📝 Generando bestiario por defecto...");
                generateDefaultBestiary(dbManager, plugin);
                return;
            }
            
            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);
            if (!config.has("categories") || config.getAsJsonArray("categories").size() == 0) {
                plugin.getLogger().info("📝 Generando bestiario por defecto...");
                generateDefaultBestiary(dbManager, plugin);
                return;
            }
            
            // Migrar desde JSON
            String insertSQL = """
                    INSERT INTO bestiary (id, name, description, mobs_json, tier_rewards_json, category_order)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            
            JsonArray categories = config.getAsJsonArray("categories");
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                int order = 0;
                for (JsonElement elem : categories) {
                    JsonObject catData = elem.getAsJsonObject();
                    String id = catData.has("id") ? catData.get("id").getAsString() : "category_" + order;
                    pstmt.setString(1, id);
                    pstmt.setString(2, catData.has("name") ? catData.get("name").getAsString() : id);
                    pstmt.setString(3, catData.has("description") ? catData.get("description").getAsString() : "");
                    pstmt.setString(4, catData.has("mobs") ? catData.get("mobs").toString() : "[]");
                    pstmt.setString(5, catData.has("tier_rewards") ? catData.get("tier_rewards").toString() : "{}");
                    pstmt.setInt(6, order++);
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                plugin.getLogger().info("✅ Migradas " + results.length + " categorías de bestiario desde JSON");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error migrando bestiario: " + e.getMessage());
        }
    }

    private static void generateDefaultBestiary(DatabaseManager dbManager, MMORPGPlugin plugin) {
        String insertSQL = """
                INSERT INTO bestiary (id, name, description, mobs_json, tier_rewards_json, category_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            // Categoría 1: Undead
            pstmt.setString(1, "undead");
            pstmt.setString(2, "No-muertos");
            pstmt.setString(3, "Criaturas que volvieron de la muerte");
            pstmt.setString(4, "[\"zombie\",\"skeleton\",\"zombie_villager\"]");
            pstmt.setString(5, "{\"bronze\":{\"kills\":10,\"reward_xp\":100},\"silver\":{\"kills\":50,\"reward_xp\":500}}");
            pstmt.setInt(6, 0);
            pstmt.addBatch();
            
            // Categoría 2: Hostiles
            pstmt.setString(1, "hostiles");
            pstmt.setString(2, "Criaturas Hostiles");
            pstmt.setString(3, "Monstruos agresivos del overworld");
            pstmt.setString(4, "[\"creeper\",\"spider\",\"enderman\"]");
            pstmt.setString(5, "{\"bronze\":{\"kills\":15,\"reward_xp\":150},\"silver\":{\"kills\":75,\"reward_xp\":750}}");
            pstmt.setInt(6, 1);
            pstmt.addBatch();
            
            int[] results = pstmt.executeBatch();
            conn.commit();
            plugin.getLogger().info("✅ Generadas " + results.length + " categorías de bestiario por defecto");
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error generando bestiario: " + e.getMessage());
        }
    }

    /**
     * Migra encantamientos o genera por defecto
     */
    private static void migrateEnchantments(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            File configFile = new File(dataDirectory, "data/enchantments.json");
            if (!configFile.exists()) {
                plugin.getLogger().info("📝 Generando encantamientos por defecto...");
                generateDefaultEnchantments(dbManager, plugin);
                return;
            }
            
            JsonObject config = gson.fromJson(new FileReader(configFile), JsonObject.class);
            if (!config.has("enchantments") || config.getAsJsonArray("enchantments").size() == 0) {
                plugin.getLogger().info("📝 Generando encantamientos por defecto...");
                generateDefaultEnchantments(dbManager, plugin);
                return;
            }
            
            // Migrar desde JSON
            String insertSQL = """
                    INSERT INTO enchantments (id, name, type, max_level, base_cost, cost_per_level, description)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            
            JsonArray enchantments = config.getAsJsonArray("enchantments");
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                
                for (JsonElement elem : enchantments) {
                    JsonObject enchData = elem.getAsJsonObject();
                    pstmt.setString(1, enchData.has("id") ? enchData.get("id").getAsString() : "ench_" + UUID.randomUUID());
                    pstmt.setString(2, enchData.has("name") ? enchData.get("name").getAsString() : "Unknown");
                    pstmt.setString(3, enchData.has("type") ? enchData.get("type").getAsString() : "GENERAL");
                    pstmt.setInt(4, enchData.has("max_level") ? enchData.get("max_level").getAsInt() : 1);
                    pstmt.setInt(5, enchData.has("base_cost") ? enchData.get("base_cost").getAsInt() : 100);
                    pstmt.setInt(6, enchData.has("cost_per_level") ? enchData.get("cost_per_level").getAsInt() : 50);
                    pstmt.setString(7, enchData.has("description") ? enchData.get("description").getAsString() : "");
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                conn.commit();
                plugin.getLogger().info("✅ Migrados " + results.length + " encantamientos desde JSON");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error migrando encantamientos: " + e.getMessage());
        }
    }

    private static void generateDefaultEnchantments(DatabaseManager dbManager, MMORPGPlugin plugin) {
        String insertSQL = """
                INSERT INTO enchantments (id, name, type, max_level, base_cost, cost_per_level, description)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            // Encantamiento 1: Filo Vampírico
            pstmt.setString(1, "vampiric_edge");
            pstmt.setString(2, "Filo Vampírico");
            pstmt.setString(3, "WEAPON");
            pstmt.setInt(4, 3);
            pstmt.setInt(5, 150);
            pstmt.setInt(6, 75);
            pstmt.setString(7, "Roba vida al atacar");
            pstmt.addBatch();
            
            // Encantamiento 2: Protección Arcana
            pstmt.setString(1, "arcane_protection");
            pstmt.setString(2, "Protección Arcana");
            pstmt.setString(3, "ARMOR");
            pstmt.setInt(4, 5);
            pstmt.setInt(5, 200);
            pstmt.setInt(6, 100);
            pstmt.setString(7, "Reduce daño mágico");
            pstmt.addBatch();
            
            int[] results = pstmt.executeBatch();
            conn.commit();
            plugin.getLogger().info("✅ Generados " + results.length + " encantamientos por defecto");
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error generando encantamientos: " + e.getMessage());
        }
    }

    /**
     * Migra escuadras o genera configuración vacía
     */
    private static void migrateSquads(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            File configFile = new File(dataDirectory, "squad_config.json");
            if (!configFile.exists()) {
                plugin.getLogger().info("✅ No hay escuadras para migrar (sistema dinámico)");
                return;
            }
            
            // Las escuadras se crean dinámicamente por jugadores, no se migran
            plugin.getLogger().info("✅ Sistema de escuadras listo (las escuadras se crean dinámicamente)");
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error verificando squads: " + e.getMessage());
        }
    }

    /**
     * Migra datos de economía o inicializa vacío
     */
    private static void migrateEconomy(String dataDirectory, DatabaseManager dbManager, MMORPGPlugin plugin) {
        try {
            // La economía se maneja dinámicamente por jugador
            // Solo verificamos que la tabla exista
            plugin.getLogger().info("✅ Sistema de economía inicializado (balances creados dinámicamente)");
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Error inicializando economía: " + e.getMessage());
        }
    }
}
