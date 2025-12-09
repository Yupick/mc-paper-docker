package com.nightslayer.mmorpg.dungeons;

import com.google.gson.*;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.mobs.MobManager;
import com.nightslayer.mmorpg.economy.EconomyManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sistema de gestión de mazmorras dinámicas con generación procedural
 */
public class DungeonManager {
    private final MMORPGPlugin plugin;
    private final MobManager mobManager;
    private final EconomyManager economy;
    private final File configFile;
    private final Map<String, DungeonConfig> dungeonConfigs;
    private final Map<String, DungeonSession> activeSessions;
    
    private static final String CREATE_DUNGEONS_TABLE = 
        "CREATE TABLE IF NOT EXISTS dungeon_history (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "session_id TEXT NOT NULL UNIQUE, " +
        "dungeon_id TEXT NOT NULL, " +
        "dungeon_name TEXT NOT NULL, " +
        "started_at TEXT NOT NULL, " +
        "completed_at TEXT, " +
        "status TEXT DEFAULT 'ACTIVE', " +
        "player_count INTEGER DEFAULT 0, " +
        "total_mobs_killed INTEGER DEFAULT 0, " +
        "duration_seconds INTEGER, " +
        "completion_rate REAL DEFAULT 0.0, " +
        "world TEXT NOT NULL" +
        ")";
    
    private static final String CREATE_DUNGEON_PARTICIPANTS_TABLE =
        "CREATE TABLE IF NOT EXISTS dungeon_participants (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "session_id TEXT NOT NULL, " +
        "player_uuid TEXT NOT NULL, " +
        "player_name TEXT NOT NULL, " +
        "kills INTEGER DEFAULT 0, " +
        "damage_dealt LONG DEFAULT 0, " +
        "rewards_xp INTEGER DEFAULT 0, " +
        "rewards_coins INTEGER DEFAULT 0, " +
        "FOREIGN KEY (session_id) REFERENCES dungeon_history(session_id)" +
        ")";
    
    public DungeonManager(MMORPGPlugin plugin, MobManager mobManager, EconomyManager economy) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.economy = economy;
        this.configFile = new File(plugin.getDataFolder(), "dungeons_config.json");
        this.dungeonConfigs = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        
        initDatabase();
        loadConfig();
    }
    
    private void initDatabase() {
        try (Connection conn = plugin.getDatabase().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_DUNGEONS_TABLE);
            stmt.execute(CREATE_DUNGEON_PARTICIPANTS_TABLE);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error inicializando tablas de dungeons: " + e.getMessage());
        }
    }
    
    /**
     * Carga la configuración de mazmorras desde JSON
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
            return;
        }
        
        try (Reader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray dungeonsArray = root.getAsJsonArray("dungeons");
            
            for (JsonElement element : dungeonsArray) {
                DungeonConfig config = parseDungeonConfig(element.getAsJsonObject());
                dungeonConfigs.put(config.getId(), config);
            }
            
            plugin.getLogger().info("Cargadas " + dungeonConfigs.size() + " mazmorras");
        } catch (IOException e) {
            plugin.getLogger().severe("Error cargando dungeons_config.json: " + e.getMessage());
            createDefaultConfig();
        }
    }
    
    /**
     * Crea configuración por defecto
     */
    private void createDefaultConfig() {
        JsonObject root = new JsonObject();
        JsonArray dungeons = new JsonArray();
        
        // Mazmorra: Catacumbas Perdidas
        JsonObject catacumbas = new JsonObject();
        catacumbas.addProperty("id", "catacombs");
        catacumbas.addProperty("name", "Catacumbas Perdidas");
        catacumbas.addProperty("description", "Ruinas antiguas llenas de esqueletos y no-muertos");
        catacumbas.addProperty("minLevel", 10);
        catacumbas.addProperty("maxLevel", 30);
        catacumbas.addProperty("minPlayers", 1);
        catacumbas.addProperty("maxPlayers", 4);
        catacumbas.addProperty("estimatedDuration", 30);
        catacumbas.addProperty("enabled", true);
        
        JsonArray cataRooms = new JsonArray();
        JsonObject mainChamber = new JsonObject();
        mainChamber.addProperty("id", "chamber_main");
        mainChamber.addProperty("type", "CHAMBER");
        mainChamber.addProperty("width", 30);
        mainChamber.addProperty("height", 30);
        JsonArray mainMobs = new JsonArray();
        mainMobs.add("skeleton");
        mainMobs.add("zombie");
        mainMobs.add("wither_skeleton");
        mainChamber.add("possibleMobs", mainMobs);
        mainChamber.addProperty("mobCount", 8);
        mainChamber.addProperty("hasTreasure", false);
        mainChamber.addProperty("hasBoss", false);
        cataRooms.add(mainChamber);
        
        JsonObject bossArena = new JsonObject();
        bossArena.addProperty("id", "arena_boss");
        bossArena.addProperty("type", "BOSS_ARENA");
        bossArena.addProperty("width", 40);
        bossArena.addProperty("height", 40);
        JsonArray bossMobs = new JsonArray();
        bossMobs.add("necromancer");
        bossArena.add("possibleMobs", bossMobs);
        bossArena.addProperty("mobCount", 1);
        bossArena.addProperty("hasTreasure", true);
        bossArena.addProperty("hasBoss", true);
        cataRooms.add(bossArena);
        
        catacumbas.add("roomTemplates", cataRooms);
        
        JsonObject cataRewards = new JsonObject();
        cataRewards.addProperty("baseXp", 500);
        cataRewards.addProperty("baseCoin", 200);
        JsonArray treasureItems = new JsonArray();
        treasureItems.add("Iron Sword RPG");
        treasureItems.add("Gold Boots");
        cataRewards.add("treasureItems", treasureItems);
        cataRewards.addProperty("treasureDropChance", 0.3);
        cataRewards.addProperty("bossXpBonus", 1000);
        cataRewards.addProperty("bossCoinBonus", 500);
        catacumbas.add("rewards", cataRewards);
        
        dungeons.add(catacumbas);
        
        // Mazmorra: Torre del Mago
        JsonObject tower = new JsonObject();
        tower.addProperty("id", "wizard_tower");
        tower.addProperty("name", "Torre del Mago Oscuro");
        tower.addProperty("description", "Una torre mágica llena de hechiceros y constructos");
        tower.addProperty("minLevel", 25);
        tower.addProperty("maxLevel", 50);
        tower.addProperty("minPlayers", 2);
        tower.addProperty("maxPlayers", 4);
        tower.addProperty("estimatedDuration", 45);
        tower.addProperty("enabled", true);
        
        JsonArray towerRooms = new JsonArray();
        JsonObject towerChamber = new JsonObject();
        towerChamber.addProperty("id", "chamber_tower");
        towerChamber.addProperty("type", "CHAMBER");
        towerChamber.addProperty("width", 25);
        towerChamber.addProperty("height", 25);
        JsonArray towerMobs = new JsonArray();
        towerMobs.add("mage");
        towerMobs.add("wizard");
        towerChamber.add("possibleMobs", towerMobs);
        towerChamber.addProperty("mobCount", 6);
        towerChamber.addProperty("hasTreasure", false);
        towerChamber.addProperty("hasBoss", false);
        towerRooms.add(towerChamber);
        
        tower.add("roomTemplates", towerRooms);
        
        JsonObject towerRewards = new JsonObject();
        towerRewards.addProperty("baseXp", 800);
        towerRewards.addProperty("baseCoin", 400);
        JsonArray towerTreasure = new JsonArray();
        towerTreasure.add("Mage Robe");
        towerTreasure.add("Staff of Power");
        towerRewards.add("treasureItems", towerTreasure);
        towerRewards.addProperty("treasureDropChance", 0.4);
        towerRewards.addProperty("bossXpBonus", 1500);
        towerRewards.addProperty("bossCoinBonus", 700);
        tower.add("rewards", towerRewards);
        
        dungeons.add(tower);
        
        root.add("dungeons", dungeons);
        
        try (Writer writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
            plugin.getLogger().info("Creado dungeons_config.json por defecto con 2 mazmorras");
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando dungeons_config.json: " + e.getMessage());
        }
    }
    
    /**
     * Parsea una configuración de mazmorra desde JSON
     */
    private DungeonConfig parseDungeonConfig(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        String description = json.get("description").getAsString();
        int minLevel = json.get("minLevel").getAsInt();
        int maxLevel = json.get("maxLevel").getAsInt();
        int minPlayers = json.get("minPlayers").getAsInt();
        int maxPlayers = json.get("maxPlayers").getAsInt();
        int estimatedDuration = json.get("estimatedDuration").getAsInt();
        boolean enabled = json.get("enabled").getAsBoolean();
        
        List<DungeonConfig.DungeonRoom> rooms = new ArrayList<>();
        if (json.has("roomTemplates")) {
            for (JsonElement roomEl : json.getAsJsonArray("roomTemplates")) {
                JsonObject roomObj = roomEl.getAsJsonObject();
                List<String> mobs = new ArrayList<>();
                for (JsonElement mobEl : roomObj.getAsJsonArray("possibleMobs")) {
                    mobs.add(mobEl.getAsString());
                }
                
                rooms.add(new DungeonConfig.DungeonRoom(
                    roomObj.get("id").getAsString(),
                    roomObj.get("type").getAsString(),
                    roomObj.get("width").getAsInt(),
                    roomObj.get("height").getAsInt(),
                    mobs,
                    roomObj.get("mobCount").getAsInt(),
                    roomObj.get("hasTreasure").getAsBoolean(),
                    roomObj.get("hasBoss").getAsBoolean()
                ));
            }
        }
        
        DungeonConfig.DungeonRewards rewards = null;
        if (json.has("rewards")) {
            JsonObject rewardsObj = json.getAsJsonObject("rewards");
            List<String> treasureItems = new ArrayList<>();
            if (rewardsObj.has("treasureItems")) {
                for (JsonElement itemEl : rewardsObj.getAsJsonArray("treasureItems")) {
                    treasureItems.add(itemEl.getAsString());
                }
            }
            
            rewards = new DungeonConfig.DungeonRewards(
                rewardsObj.get("baseXp").getAsInt(),
                rewardsObj.get("baseCoin").getAsInt(),
                treasureItems,
                rewardsObj.get("treasureDropChance").getAsDouble(),
                rewardsObj.get("bossXpBonus").getAsInt(),
                rewardsObj.get("bossCoinBonus").getAsInt()
            );
        }
        
        return new DungeonConfig(id, name, description, minLevel, maxLevel, 
                                minPlayers, maxPlayers, estimatedDuration, rooms, rewards, enabled);
    }
    
    /**
     * Inicia una nueva sesión de mazmorra
     */
    public DungeonSession startDungeon(String dungeonId, World world) {
        DungeonConfig config = dungeonConfigs.get(dungeonId);
        if (config == null || !config.isEnabled()) {
            plugin.getLogger().warning("Mazmorra no encontrada o deshabilitada: " + dungeonId);
            return null;
        }
        
        String sessionId = UUID.randomUUID().toString();
        DungeonSession session = new DungeonSession(sessionId, dungeonId, config.getName(), world);
        
        // Generar salas proceduralmente
        generateDungeonLayout(session, config);
        
        activeSessions.put(sessionId, session);
        saveSessionStart(session);
        
        Bukkit.broadcastMessage("§6§l[MAZMORRA] §e" + config.getName() + " §7iniciada!");
        plugin.getLogger().info("Sesión de mazmorra iniciada: " + sessionId);
        
        return session;
    }
    
    /**
     * Genera el layout de salas de forma procedural
     */
    private void generateDungeonLayout(DungeonSession session, DungeonConfig config) {
        // Generar 3-5 salas según la configuración
        int roomCount = 3 + ThreadLocalRandom.current().nextInt(3);
        World world = session.getWorld();
        Location baseLocation = world.getSpawnLocation();
        
        for (int i = 0; i < roomCount; i++) {
            DungeonConfig.DungeonRoom template = config.getRoomTemplates()
                .get(i % config.getRoomTemplates().size());
            
            // Crear ubicación para la sala (offset en X y Z)
            Location roomOrigin = baseLocation.clone();
            roomOrigin.add(i * 60, 0, 0); // 60 bloques de separación
            
            DungeonSession.GeneratedRoom generatedRoom = new DungeonSession.GeneratedRoom(
                i, template.getType(), roomOrigin, template.getWidth(), template.getHeight()
            );
            
            // Generar spawns de mobs
            for (int j = 0; j < template.getMobCount(); j++) {
                String mobId = template.getPossibleMobs()
                    .get(ThreadLocalRandom.current().nextInt(template.getPossibleMobs().size()));
                
                Location spawnLoc = roomOrigin.clone()
                    .add(ThreadLocalRandom.current().nextDouble() * template.getWidth(),
                         1,
                         ThreadLocalRandom.current().nextDouble() * template.getHeight());
                
                generatedRoom.addMobSpawn(mobId, spawnLoc);
            }
            
            // Agregar ubicación de tesoro si la sala lo permite
            if (template.hasTreasure()) {
                Location treasureLoc = roomOrigin.clone()
                    .add(template.getWidth() / 2, 1, template.getHeight() / 2);
                generatedRoom.addTreasureLocation(treasureLoc);
            }
            
            session.getRooms().add(generatedRoom);
        }
    }
    
    /**
     * Completa una sesión de mazmorra
     */
    public void completeDungeon(String sessionId, boolean success) {
        DungeonSession session = activeSessions.remove(sessionId);
        if (session == null) return;
        
        if (success) {
            session.complete();
        } else {
            session.fail();
        }
        
        // Guardar en BD
        saveSessionEnd(session);
        
        Bukkit.broadcastMessage("§6§l[MAZMORRA] §e" + session.getDungeonName() + 
            (success ? " §7completada!" : " §cfracasada!"));
    }
    
    /**
     * Obtiene todas las mazmorras disponibles
     */
    public Map<String, DungeonConfig> getDungeonConfigs() {
        return new HashMap<>(dungeonConfigs);
    }
    
    /**
     * Obtiene una sesión activa
     */
    public DungeonSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Obtiene el historial de mazmorras
     */
    public List<Map<String, Object>> getDungeonHistory(int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        String query = "SELECT * FROM dungeon_history ORDER BY started_at DESC LIMIT ?";
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> dungeon = new HashMap<>();
                dungeon.put("sessionId", rs.getString("session_id"));
                dungeon.put("dungeonId", rs.getString("dungeon_id"));
                dungeon.put("dungeonName", rs.getString("dungeon_name"));
                dungeon.put("startedAt", rs.getString("started_at"));
                dungeon.put("completedAt", rs.getString("completed_at"));
                dungeon.put("status", rs.getString("status"));
                dungeon.put("playerCount", rs.getInt("player_count"));
                dungeon.put("totalMobsKilled", rs.getInt("total_mobs_killed"));
                dungeon.put("durationSeconds", rs.getInt("duration_seconds"));
                dungeon.put("completionRate", rs.getDouble("completion_rate"));
                dungeon.put("world", rs.getString("world"));
                history.add(dungeon);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error obteniendo historial de dungeons: " + e.getMessage());
        }
        
        return history;
    }
    
    private void saveSessionStart(DungeonSession session) {
        String sql = "INSERT INTO dungeon_history (session_id, dungeon_id, dungeon_name, started_at, world) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getSessionId());
            stmt.setString(2, session.getDungeonId());
            stmt.setString(3, session.getDungeonName());
            stmt.setString(4, session.getStartedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stmt.setString(5, session.getWorld().getName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando inicio de dungeon: " + e.getMessage());
        }
    }
    
    private void saveSessionEnd(DungeonSession session) {
        String sql = "UPDATE dungeon_history SET completed_at = ?, status = ?, player_count = ?, " +
                    "total_mobs_killed = ?, duration_seconds = ? WHERE session_id = ?";
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stmt.setString(2, session.getStatus());
            stmt.setInt(3, session.getPlayerCount());
            stmt.setInt(4, session.getTotalMobsKilled());
            
            long durationSeconds = 0;
            if (session.getCompletedAt() != null) {
                durationSeconds = java.time.temporal.ChronoUnit.SECONDS
                    .between(session.getStartedAt(), session.getCompletedAt());
            }
            stmt.setLong(5, durationSeconds);
            stmt.setString(6, session.getSessionId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando fin de dungeon: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        // Finalizar sesiones activas
        for (String sessionId : new ArrayList<>(activeSessions.keySet())) {
            completeDungeon(sessionId, false);
        }
    }
}
