package com.nightslayer.mmorpg.events;

import com.google.gson.*;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.mobs.MobManager;
import com.nightslayer.mmorpg.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Player;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de gestión de eventos temáticos (Halloween, Navidad, etc.)
 * con mobs especiales, drops exclusivos y zonas temporales
 */
public class EventManager {
    private final MMORPGPlugin plugin;
    private final File configFile;
    private final Map<String, EventConfig> eventConfigs;
    private final Map<String, EventSession> activeSessions;
    private final Map<UUID, Integer> eventCurrency; // Monedas de evento por jugador
    private BukkitTask autoCheckTask;
    
    private static final String CREATE_EVENTS_TABLE = 
        "CREATE TABLE IF NOT EXISTS event_history (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "event_id TEXT NOT NULL, " +
        "event_name TEXT NOT NULL, " +
        "started_at TEXT NOT NULL, " +
        "ended_at TEXT, " +
        "participants INTEGER DEFAULT 0, " +
        "total_kills INTEGER DEFAULT 0, " +
        "status TEXT DEFAULT 'ACTIVE', " +
        "world TEXT NOT NULL" +
        ")";
    
    private static final String CREATE_EVENT_PARTICIPANTS_TABLE =
        "CREATE TABLE IF NOT EXISTS event_participants (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "event_history_id INTEGER NOT NULL, " +
        "player_uuid TEXT NOT NULL, " +
        "player_name TEXT NOT NULL, " +
        "kills INTEGER DEFAULT 0, " +
        "event_coins_earned INTEGER DEFAULT 0, " +
        "FOREIGN KEY (event_history_id) REFERENCES event_history(id)" +
        ")";
    
    public EventManager(MMORPGPlugin plugin, MobManager mobManager, EconomyManager economy) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "events_config.json");
        this.eventConfigs = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.eventCurrency = new ConcurrentHashMap<>();
        
        initDatabase();
        loadConfig();
        startAutoEventChecker();
    }
    
    private void initDatabase() {
        try (Connection conn = plugin.getDatabase().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_EVENTS_TABLE);
            stmt.execute(CREATE_EVENT_PARTICIPANTS_TABLE);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error inicializando tablas de eventos: " + e.getMessage());
        }
    }
    
    /**
     * Carga la configuración de eventos desde JSON
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
            return;
        }
        
        try (Reader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray eventsArray = root.getAsJsonArray("events");
            
            for (JsonElement element : eventsArray) {
                EventConfig config = parseEventConfig(element.getAsJsonObject());
                eventConfigs.put(config.getId(), config);
            }
            
            plugin.getLogger().info("Cargados " + eventConfigs.size() + " eventos temáticos");
        } catch (IOException e) {
            plugin.getLogger().severe("Error cargando events_config.json: " + e.getMessage());
            createDefaultConfig();
        }
    }
    
    /**
     * Crea configuración por defecto con eventos de ejemplo
     */
    private void createDefaultConfig() {
        JsonObject root = new JsonObject();
        JsonArray events = new JsonArray();
        
        // Evento Halloween
        JsonObject halloween = new JsonObject();
        halloween.addProperty("id", "halloween_2025");
        halloween.addProperty("name", "Halloween Nightmare");
        halloween.addProperty("description", "Los espíritus malévolos han invadido el mundo!");
        halloween.addProperty("startDate", "2025-10-25T00:00:00");
        halloween.addProperty("endDate", "2025-11-01T23:59:59");
        halloween.addProperty("enabled", true);
        halloween.addProperty("autoStart", true);
        
        JsonArray halloweenMobs = new JsonArray();
        JsonObject ghostMob = new JsonObject();
        ghostMob.addProperty("mobId", "ghost");
        ghostMob.addProperty("spawnChance", 0.3);
        ghostMob.addProperty("spawnRadius", 50);
        halloweenMobs.add(ghostMob);
        
        JsonObject pumpkinKing = new JsonObject();
        pumpkinKing.addProperty("mobId", "pumpkin_king");
        pumpkinKing.addProperty("spawnChance", 0.05);
        pumpkinKing.addProperty("spawnRadius", 100);
        halloweenMobs.add(pumpkinKing);
        halloween.add("customMobs", halloweenMobs);
        
        JsonArray halloweenDrops = new JsonArray();
        JsonObject candyDrop = new JsonObject();
        candyDrop.addProperty("itemName", "Halloween Candy");
        candyDrop.addProperty("dropChance", 0.5);
        candyDrop.addProperty("minAmount", 1);
        candyDrop.addProperty("maxAmount", 5);
        halloweenDrops.add(candyDrop);
        halloween.add("exclusiveDrops", halloweenDrops);
        
        JsonObject halloweenRewards = new JsonObject();
        halloweenRewards.addProperty("eventCoinsPerKill", 5);
        halloweenRewards.addProperty("bonusXPMultiplier", 1.5);
        halloweenRewards.addProperty("bossEventCoins", 100);
        halloween.add("rewards", halloweenRewards);
        
        events.add(halloween);
        
        // Evento Navidad
        JsonObject christmas = new JsonObject();
        christmas.addProperty("id", "christmas_2025");
        christmas.addProperty("name", "Winter Wonderland");
        christmas.addProperty("description", "La magia navideña se ha vuelto oscura...");
        christmas.addProperty("startDate", "2025-12-20T00:00:00");
        christmas.addProperty("endDate", "2025-12-26T23:59:59");
        christmas.addProperty("enabled", true);
        christmas.addProperty("autoStart", true);
        
        JsonArray christmasMobs = new JsonArray();
        JsonObject snowman = new JsonObject();
        snowman.addProperty("mobId", "corrupted_snowman");
        snowman.addProperty("spawnChance", 0.4);
        snowman.addProperty("spawnRadius", 50);
        christmasMobs.add(snowman);
        
        JsonObject evilSanta = new JsonObject();
        evilSanta.addProperty("mobId", "evil_santa");
        evilSanta.addProperty("spawnChance", 0.02);
        evilSanta.addProperty("spawnRadius", 150);
        christmasMobs.add(evilSanta);
        christmas.add("customMobs", christmasMobs);
        
        JsonArray christmasDrops = new JsonArray();
        JsonObject giftDrop = new JsonObject();
        giftDrop.addProperty("itemName", "Christmas Gift");
        giftDrop.addProperty("dropChance", 0.3);
        giftDrop.addProperty("minAmount", 1);
        giftDrop.addProperty("maxAmount", 3);
        christmasDrops.add(giftDrop);
        christmas.add("exclusiveDrops", christmasDrops);
        
        JsonObject christmasRewards = new JsonObject();
        christmasRewards.addProperty("eventCoinsPerKill", 7);
        christmasRewards.addProperty("bonusXPMultiplier", 2.0);
        christmasRewards.addProperty("bossEventCoins", 200);
        christmas.add("rewards", christmasRewards);
        
        events.add(christmas);
        
        root.add("events", events);
        
        try (Writer writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
            plugin.getLogger().info("Creado events_config.json por defecto con 2 eventos");
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando events_config.json: " + e.getMessage());
        }
    }
    
    /**
     * Parsea una configuración de evento desde JSON
     */
    private EventConfig parseEventConfig(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        String description = json.get("description").getAsString();
        LocalDateTime startDate = LocalDateTime.parse(json.get("startDate").getAsString());
        LocalDateTime endDate = LocalDateTime.parse(json.get("endDate").getAsString());
        boolean enabled = json.get("enabled").getAsBoolean();
        boolean autoStart = json.get("autoStart").getAsBoolean();
        
        List<EventConfig.EventMob> mobs = new ArrayList<>();
        if (json.has("customMobs")) {
            for (JsonElement mobEl : json.getAsJsonArray("customMobs")) {
                JsonObject mobObj = mobEl.getAsJsonObject();
                mobs.add(new EventConfig.EventMob(
                    mobObj.get("mobId").getAsString(),
                    mobObj.get("spawnChance").getAsDouble(),
                    mobObj.get("spawnRadius").getAsInt()
                ));
            }
        }
        
        List<EventConfig.EventDrop> drops = new ArrayList<>();
        if (json.has("exclusiveDrops")) {
            for (JsonElement dropEl : json.getAsJsonArray("exclusiveDrops")) {
                JsonObject dropObj = dropEl.getAsJsonObject();
                drops.add(new EventConfig.EventDrop(
                    dropObj.get("itemName").getAsString(),
                    dropObj.get("dropChance").getAsDouble(),
                    dropObj.get("minAmount").getAsInt(),
                    dropObj.get("maxAmount").getAsInt()
                ));
            }
        }
        
        EventConfig.EventRewards rewards = null;
        if (json.has("rewards")) {
            JsonObject rewardsObj = json.getAsJsonObject("rewards");
            rewards = new EventConfig.EventRewards(
                rewardsObj.get("eventCoinsPerKill").getAsInt(),
                rewardsObj.get("bonusXPMultiplier").getAsDouble(),
                rewardsObj.get("bossEventCoins").getAsInt()
            );
        }
        
        return new EventConfig(id, name, description, startDate, endDate, enabled, autoStart, mobs, drops, rewards);
    }
    
    /**
     * Inicia el checker automático de eventos
     */
    private void startAutoEventChecker() {
        // Verificar cada 5 minutos si hay eventos para activar/desactivar
        autoCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalDateTime now = LocalDateTime.now();
            
            for (EventConfig config : eventConfigs.values()) {
                if (!config.isEnabled() || !config.isAutoStart()) continue;
                
                boolean shouldBeActive = now.isAfter(config.getStartDate()) && now.isBefore(config.getEndDate());
                boolean isActive = activeSessions.containsKey(config.getId());
                
                if (shouldBeActive && !isActive) {
                    startEvent(config.getId(), "world");
                } else if (!shouldBeActive && isActive) {
                    stopEvent(config.getId());
                }
            }
        }, 100L, 6000L); // 5 segundos inicial, luego cada 5 minutos
    }
    
    /**
     * Inicia un evento en un mundo específico
     */
    public boolean startEvent(String eventId, String worldName) {
        EventConfig config = eventConfigs.get(eventId);
        if (config == null || !config.isEnabled()) {
            return false;
        }
        
        if (activeSessions.containsKey(eventId)) {
            plugin.getLogger().warning("El evento " + eventId + " ya está activo");
            return false;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Mundo " + worldName + " no encontrado");
            return false;
        }
        
        EventSession session = new EventSession(eventId, config.getName(), worldName);
        activeSessions.put(eventId, session);
        
        // Guardar en BD
        saveEventStart(session);
        
        // Broadcast
        net.kyori.adventure.audience.Audience audience = org.bukkit.Bukkit.getServer();
        net.kyori.adventure.text.Component msg1 = net.kyori.adventure.text.Component.text("§6§l[EVENTO] §e" + config.getName() + " §7ha comenzado!");
        net.kyori.adventure.text.Component msg2 = net.kyori.adventure.text.Component.text("§7" + config.getDescription());
        audience.sendMessage(msg1);
        audience.sendMessage(msg2);
        
        plugin.getLogger().info("Evento " + eventId + " iniciado en mundo " + worldName);
        return true;
    }
    
    /**
     * Detiene un evento activo
     */
    public boolean stopEvent(String eventId) {
        EventSession session = activeSessions.remove(eventId);
        if (session == null) {
            return false;
        }
        
        session.setEndedAt(LocalDateTime.now());
        session.setStatus("COMPLETED");
        
        // Actualizar BD
        saveEventEnd(session);
        
        // Broadcast
        net.kyori.adventure.audience.Audience audience = org.bukkit.Bukkit.getServer();
        net.kyori.adventure.text.Component msg1 = net.kyori.adventure.text.Component.text("§6§l[EVENTO] §e" + session.getEventName() + " §7ha finalizado!");
        net.kyori.adventure.text.Component msg2 = net.kyori.adventure.text.Component.text("§7Participantes: §e" + session.getParticipants().size() + " §7| Kills totales: §c" + session.getTotalKills());
        audience.sendMessage(msg1);
        audience.sendMessage(msg2);
        
        plugin.getLogger().info("Evento " + eventId + " finalizado");
        return true;
    }
    
    /**
     * Registra un kill de mob de evento
     */
    public void onEventMobKill(Player killer, String mobId) {
        for (Map.Entry<String, EventSession> entry : activeSessions.entrySet()) {
            EventConfig config = eventConfigs.get(entry.getKey());
            if (config == null) continue;
            
            // Verificar si el mob es parte del evento
            boolean isMobFromEvent = config.getCustomMobs().stream()
                .anyMatch(em -> em.getMobId().equals(mobId));
            
            if (isMobFromEvent) {
                EventSession session = entry.getValue();
                UUID playerId = killer.getUniqueId();
                
                session.addPlayerKill(playerId, killer.getName());
                
                // Recompensas
                if (config.getRewards() != null) {
                    int eventCoins = config.getRewards().getEventCoinsPerKill();
                    eventCurrency.merge(playerId, Integer.valueOf(eventCoins), (a, b) -> Integer.valueOf(a.intValue() + b.intValue()));
                    killer.sendMessage("§a§l[EVENTO] §7+§e" + eventCoins + " §7monedas de evento");
                }
                
                // Actualizar BD
                updateParticipantKills(session.getHistoryId(), playerId, killer.getName());
            }
        }
    }
    
    /**
     * Obtiene las monedas de evento de un jugador
     */
    public int getEventCurrency(UUID playerId) {
        return eventCurrency.getOrDefault(playerId, 0);
    }
    
    /**
     * Consume monedas de evento de un jugador
     */
    public boolean spendEventCurrency(UUID playerId, int amount) {
        int current = eventCurrency.getOrDefault(playerId, 0);
        if (current < amount) {
            return false;
        }
        eventCurrency.put(playerId, current - amount);
        return true;
    }
    
    /**
     * Obtiene todos los eventos configurados
     */
    public Map<String, EventConfig> getEventConfigs() {
        return new HashMap<>(eventConfigs);
    }
    
    /**
     * Obtiene las sesiones activas
     */
    public Map<String, EventSession> getActiveSessions() {
        return new HashMap<>(activeSessions);
    }
    
    /**
     * Verifica si hay algún evento activo
     */
    public boolean hasActiveEvents() {
        return !activeSessions.isEmpty();
    }
    
    /**
     * Obtiene el historial de eventos desde la BD
     */
    public List<Map<String, Object>> getEventHistory(int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        String query = "SELECT * FROM event_history ORDER BY started_at DESC LIMIT ?";
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> event = new HashMap<>();
                event.put("id", rs.getInt("id"));
                event.put("event_id", rs.getString("event_id"));
                event.put("event_name", rs.getString("event_name"));
                event.put("started_at", rs.getString("started_at"));
                event.put("ended_at", rs.getString("ended_at"));
                event.put("participants", rs.getInt("participants"));
                event.put("total_kills", rs.getInt("total_kills"));
                event.put("status", rs.getString("status"));
                event.put("world", rs.getString("world"));
                history.add(event);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error obteniendo historial de eventos: " + e.getMessage());
        }
        
        return history;
    }
    
    private void saveEventStart(EventSession session) {
        String sql = "INSERT INTO event_history (event_id, event_name, started_at, world, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, session.getEventId());
            stmt.setString(2, session.getEventName());
            stmt.setString(3, session.getStartedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stmt.setString(4, session.getWorldName());
            stmt.setString(5, "ACTIVE");
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                session.setHistoryId(rs.getInt(1));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando inicio de evento: " + e.getMessage());
        }
    }
    
    private void saveEventEnd(EventSession session) {
        String sql = "UPDATE event_history SET ended_at = ?, participants = ?, total_kills = ?, status = ? WHERE id = ?";
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, session.getEndedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            stmt.setInt(2, session.getParticipants().size());
            stmt.setInt(3, session.getTotalKills());
            stmt.setString(4, session.getStatus());
            stmt.setInt(5, session.getHistoryId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando fin de evento: " + e.getMessage());
        }
    }
    
    private void updateParticipantKills(int historyId, UUID playerId, String playerName) {
        String checkSql = "SELECT id, kills FROM event_participants WHERE event_history_id = ? AND player_uuid = ?";
        String insertSql = "INSERT INTO event_participants (event_history_id, player_uuid, player_name, kills) VALUES (?, ?, ?, 1)";
        String updateSql = "UPDATE event_participants SET kills = ? WHERE id = ?";
        
        try (Connection conn = plugin.getDatabase().getConnection()) {
            // Verificar si existe
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, historyId);
                checkStmt.setString(2, playerId.toString());
                
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int kills = rs.getInt("kills");
                    
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, kills + 1);
                        updateStmt.setInt(2, id);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, historyId);
                        insertStmt.setString(2, playerId.toString());
                        insertStmt.setString(3, playerName);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error actualizando kills de participante: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        if (autoCheckTask != null) {
            autoCheckTask.cancel();
        }
        
        // Finalizar eventos activos
        for (String eventId : new ArrayList<>(activeSessions.keySet())) {
            stopEvent(eventId);
        }
    }

    public List<Map<String, Object>> getCurrencyHistory(UUID playerId, int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT event_id, event_coins_earned, kills FROM event_participants WHERE player_uuid = ? ORDER BY id DESC LIMIT ?";
        
        try (Connection conn = plugin.getDatabase().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, limit);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("event_id", rs.getString("event_id"));
                entry.put("coins", rs.getInt("event_coins_earned"));
                entry.put("kills", rs.getInt("kills"));
                history.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error obteniendo historial de monedas: " + e.getMessage());
        }
        
        return history;
    }

    public void addEventCurrency(UUID playerId, int amount, String eventId, String reason) {
        int current = eventCurrency.getOrDefault(playerId, 0);
        eventCurrency.put(playerId, current + amount);
        
        // Actualizar en BD si hay sesión activa
        EventSession session = activeSessions.get(eventId);
        if (session != null && session.getHistoryId() > 0) {
            String sql = "UPDATE event_participants SET event_coins_earned = event_coins_earned + ? WHERE event_history_id = ? AND player_uuid = ?";
            try (Connection conn = plugin.getDatabase().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, amount);
                stmt.setInt(2, session.getHistoryId());
                stmt.setString(3, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error actualizando monedas de evento: " + e.getMessage());
            }
        }
    }

    public boolean spendEventCurrency(UUID playerId, int amount, String reason) {
        int current = eventCurrency.getOrDefault(playerId, 0);
        if (current < amount) {
            return false;
        }
        
        eventCurrency.put(playerId, current - amount);
        return true;
    }

    public int getEventCurrency(UUID playerId) {
        return eventCurrency.getOrDefault(playerId, 0);
    }

    public boolean validateEventMobs(String eventId, MobManager mobManager) {
        EventConfig config = eventConfigs.get(eventId);
        if (config == null) {
            return false;
        }
        
        // Verificar que todos los mobs del evento existen
        for (String mobId : config.getCustomMobs().keySet()) {
            if (mobManager.getMobConfig(mobId) == null) {
                plugin.getLogger().warning("Mob no encontrado en evento " + eventId + ": " + mobId);
                return false;
            }
        }
        
        return true;
    }
}
