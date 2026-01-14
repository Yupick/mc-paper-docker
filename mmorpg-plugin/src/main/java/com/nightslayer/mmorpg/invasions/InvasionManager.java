package com.nightslayer.mmorpg.invasions;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.mobs.MobManager;
import com.nightslayer.mmorpg.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.audience.Audience;

/**
 * Manager for invasion system
 */
public class InvasionManager {
    private final MMORPGPlugin plugin;
    private final Logger logger;
    private final File configFile;
    private final Connection dbConnection;
    
    private Map<String, InvasionConfig> invasionConfigs;
    private Map<String, InvasionSession> activeSessions;
    private Map<String, BukkitTask> scheduledTasks;
    private Map<String, List<Entity>> invasionMobs;

    public InvasionManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder() + "/data", "invasions_config.json");
        this.invasionConfigs = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.scheduledTasks = new HashMap<>();
        this.invasionMobs = new HashMap<>();
        // Usar conexión de DatabaseManager en lugar de crear una propia
        this.dbConnection = plugin.getDatabaseManager().getConnection();
        try {
            createTables();
        } catch (SQLException e) {
            logger.severe("Error al crear tablas de invasiones: " + e.getMessage());
        }

        loadConfig();
        scheduleInvasions();
    }

    /**
     * Create database tables
     */
    private void createTables() throws SQLException {
        if (dbConnection == null) {
            logger.severe("ERROR en InvasionManager: dbConnection es NULL!");
            return;
        }
        // Las tablas ya están creadas por DatabaseManager.createTables()
        // Este método se mantiene por compatibilidad pero no hace nada
        logger.info("Tablas de invasiones creadas/verificadas");
    }

    /**
     * Load invasions config from JSON
     */
    public void loadConfig() {
        try {
            if (!configFile.exists()) {
                createDefaultConfig();
            }

            String content = new String(Files.readAllBytes(configFile.toPath()));
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(content, JsonObject.class);
            JsonArray invasions = json.getAsJsonArray("invasions");

            invasionConfigs.clear();
            for (JsonElement element : invasions) {
                JsonObject invasionJson = element.getAsJsonObject();
                InvasionConfig config = parseInvasionConfig(invasionJson);
                invasionConfigs.put(config.getInvasionId(), config);
            }

            logger.info("Loaded " + invasionConfigs.size() + " invasion configurations");
        } catch (Exception e) {
            logger.severe("Failed to load invasions config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse invasion config from JSON
     */
    private InvasionConfig parseInvasionConfig(JsonObject json) {
        String invasionId = json.get("invasionId").getAsString();
        String displayName = json.get("displayName").getAsString();
        String description = json.get("description").getAsString();
        
        List<String> targetWorlds = new ArrayList<>();
        JsonArray worldsJson = json.getAsJsonArray("targetWorlds");
        for (JsonElement element : worldsJson) {
            targetWorlds.add(element.getAsString());
        }

        List<InvasionConfig.InvasionWaveConfig> waves = new ArrayList<>();
        JsonArray wavesJson = json.getAsJsonArray("waves");
        for (JsonElement element : wavesJson) {
            JsonObject waveJson = element.getAsJsonObject();
            waves.add(new InvasionConfig.InvasionWaveConfig(
                    waveJson.get("waveNumber").getAsInt(),
                    waveJson.get("mobType").getAsString(),
                    waveJson.get("mobCount").getAsInt(),
                    waveJson.get("mobLevel").getAsInt(),
                    waveJson.get("delaySeconds").getAsInt(),
                    waveJson.has("isBossWave") ? waveJson.get("isBossWave").getAsBoolean() : false,
                    waveJson.has("bossName") ? waveJson.get("bossName").getAsString() : null,
                    waveJson.has("bossHealthMultiplier") ? waveJson.get("bossHealthMultiplier").getAsDouble() : 1.0
            ));
        }

        JsonObject rewardsJson = json.getAsJsonObject("rewards");
        List<String> specialItems = new ArrayList<>();
        if (rewardsJson.has("specialItems")) {
            JsonArray itemsJson = rewardsJson.getAsJsonArray("specialItems");
            for (JsonElement element : itemsJson) {
                specialItems.add(element.getAsString());
            }
        }
        InvasionConfig.InvasionRewards rewards = new InvasionConfig.InvasionRewards(
                rewardsJson.get("xpPerWave").getAsInt(),
                rewardsJson.get("coinsPerWave").getAsInt(),
                rewardsJson.get("xpBonus").getAsInt(),
                rewardsJson.get("coinsBonus").getAsInt(),
                specialItems
        );

        JsonObject scheduleJson = json.getAsJsonObject("schedule");
        List<String> fixedTimes = new ArrayList<>();
        if (scheduleJson.has("fixedTimes")) {
            JsonArray timesJson = scheduleJson.getAsJsonArray("fixedTimes");
            for (JsonElement element : timesJson) {
                fixedTimes.add(element.getAsString());
            }
        }
        InvasionConfig.InvasionSchedule schedule = new InvasionConfig.InvasionSchedule(
                scheduleJson.get("scheduleType").getAsString(),
                fixedTimes,
                scheduleJson.has("randomMinHours") ? scheduleJson.get("randomMinHours").getAsInt() : 4,
                scheduleJson.has("randomMaxHours") ? scheduleJson.get("randomMaxHours").getAsInt() : 8,
                scheduleJson.get("durationMinutes").getAsInt()
        );

        boolean enabled = json.has("enabled") ? json.get("enabled").getAsBoolean() : true;

        return new InvasionConfig(invasionId, displayName, description, targetWorlds, waves, rewards, schedule, enabled);
    }

    /**
     * Create default config
     */
    private void createDefaultConfig() throws Exception {
        configFile.getParentFile().mkdirs();

        JsonObject config = new JsonObject();
        JsonArray invasions = new JsonArray();

        // Zombie invasion
        JsonObject zombieInvasion = new JsonObject();
        zombieInvasion.addProperty("invasionId", "zombie_horde");
        zombieInvasion.addProperty("displayName", "Horda de Zombies");
        zombieInvasion.addProperty("description", "Una horda masiva de zombies ataca el mundo!");
        
        JsonArray targetWorlds = new JsonArray();
        targetWorlds.add("mmorpg");
        zombieInvasion.add("targetWorlds", targetWorlds);
        zombieInvasion.addProperty("enabled", true);

        JsonArray zombieWaves = new JsonArray();
        for (int i = 1; i <= 5; i++) {
            JsonObject wave = new JsonObject();
            wave.addProperty("waveNumber", i);
            wave.addProperty("mobType", "ZOMBIE");
            wave.addProperty("mobCount", 10 + (i * 5));
            wave.addProperty("mobLevel", i * 2);
            wave.addProperty("delaySeconds", 30);
            wave.addProperty("isBossWave", i == 5);
            if (i == 5) {
                wave.addProperty("bossName", "Rey Zombie");
                wave.addProperty("bossHealthMultiplier", 5.0);
            }
            zombieWaves.add(wave);
        }
        zombieInvasion.add("waves", zombieWaves);

        JsonObject zombieRewards = new JsonObject();
        zombieRewards.addProperty("xpPerWave", 500);
        zombieRewards.addProperty("coinsPerWave", 100);
        zombieRewards.addProperty("xpBonus", 2000);
        zombieRewards.addProperty("coinsBonus", 500);
        
        JsonArray specialItems = new JsonArray();
        specialItems.add("DIAMOND_SWORD");
        specialItems.add("GOLDEN_APPLE");
        zombieRewards.add("specialItems", specialItems);
        zombieInvasion.add("rewards", zombieRewards);

        JsonObject zombieSchedule = new JsonObject();
        zombieSchedule.addProperty("scheduleType", "RANDOM");
        zombieSchedule.addProperty("randomMinHours", 4);
        zombieSchedule.addProperty("randomMaxHours", 8);
        zombieSchedule.addProperty("durationMinutes", 30);
        zombieInvasion.add("schedule", zombieSchedule);

        invasions.add(zombieInvasion);
        config.add("invasions", invasions);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(gson.toJson(config));
        }
    }

    /**
     * Schedule invasions based on config
     */
    public void scheduleInvasions() {
        // Cancel existing scheduled tasks
        scheduledTasks.values().forEach(BukkitTask::cancel);
        scheduledTasks.clear();

        for (InvasionConfig config : invasionConfigs.values()) {
            if (!config.isEnabled()) continue;

            InvasionConfig.InvasionSchedule schedule = config.getSchedule();
            
            if ("RANDOM".equals(schedule.getScheduleType())) {
                scheduleRandomInvasion(config, schedule);
            } else if ("FIXED".equals(schedule.getScheduleType())) {
                scheduleFixedInvasion(config, schedule);
            }
        }
    }

    /**
     * Schedule random invasion
     */
    private void scheduleRandomInvasion(InvasionConfig config, InvasionConfig.InvasionSchedule schedule) {
        int minTicks = schedule.getRandomMinHours() * 60 * 60 * 20;
        int maxTicks = schedule.getRandomMaxHours() * 60 * 60 * 20;
        int randomTicks = minTicks + new Random().nextInt(maxTicks - minTicks);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                startInvasion(config.getInvasionId());
                scheduleRandomInvasion(config, schedule); // Reschedule
            }
        }.runTaskLater(plugin, randomTicks);

        scheduledTasks.put(config.getInvasionId(), task);
    }

    /**
     * Schedule fixed time invasion (simplified - would need proper time parsing)
     */
    private void scheduleFixedInvasion(InvasionConfig config, InvasionConfig.InvasionSchedule schedule) {
        // This would need proper implementation to parse HH:MM times and schedule accordingly
        // For now, just schedule at random intervals
        scheduleRandomInvasion(config, schedule);
    }

    /**
     * Start an invasion
     */
    public boolean startInvasion(String invasionId) {
        InvasionConfig config = invasionConfigs.get(invasionId);
        if (config == null || !config.isEnabled()) {
            return false;
        }

        // Select random target world
        List<String> targetWorlds = config.getTargetWorlds();
        if (targetWorlds.isEmpty()) return false;
        
        String worldName = targetWorlds.get(new Random().nextInt(targetWorlds.size()));
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;

        // Check if already active in this world
        if (isInvasionActiveInWorld(worldName)) {
            logger.warning("Invasion already active in world: " + worldName);
            return false;
        }

        // Create session
        InvasionSession session = new InvasionSession(invasionId, worldName, config.getWaves().size());
        activeSessions.put(session.getSessionId(), session);
        invasionMobs.put(session.getSessionId(), new ArrayList<>());

        // Broadcast start
        Audience audience = Bukkit.getServer();
        audience.sendMessage(Component.text("§c§l[INVASIÓN] §e" + config.getDisplayName() + " §7ha comenzado en §b" + worldName + "§7!"));

        // Start first wave
        scheduleWave(session, config, 0);

        return true;
    }

    /**
     * Schedule a wave
     */
    private void scheduleWave(InvasionSession session, InvasionConfig config, int waveIndex) {
        if (waveIndex >= config.getWaves().size()) {
            completeInvasion(session, config, true);
            return;
        }

        InvasionConfig.InvasionWaveConfig waveConfig = config.getWaves().get(waveIndex);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeSessions.containsKey(session.getSessionId())) {
                    return; // Session was cancelled
                }

                session.nextWave();
                spawnWave(session, config, waveConfig);

                // Broadcast wave start
                Audience audience = Bukkit.getServer();
                audience.sendMessage(Component.text("§c§l[INVASIÓN] §7Oleada §e" + waveConfig.getWaveNumber() +
                    "§7/§e" + config.getWaves().size() + " §7- §6" + waveConfig.getMobCount() +
                    " " + waveConfig.getMobType() + (waveConfig.isBossWave() ? " §c§lBOSS" : "")));

                // Check wave completion periodically
                checkWaveCompletion(session, config, waveIndex);
            }
        }.runTaskLater(plugin, waveConfig.getDelaySeconds() * 20L);
    }

    /**
     * Spawn wave mobs
     */
    private void spawnWave(InvasionSession session, InvasionConfig config, InvasionConfig.InvasionWaveConfig waveConfig) {
        World world = Bukkit.getWorld(session.getWorldName());
        if (world == null) return;

        List<Entity> waveMobs = invasionMobs.get(session.getSessionId());
        MobManager mobManager = plugin.getMobManager();

        // Get random spawn location (simplified - would need better logic)
        List<Player> playersInWorld = world.getPlayers();
        if (playersInWorld.isEmpty()) return;

        for (int i = 0; i < waveConfig.getMobCount(); i++) {
            Player randomPlayer = playersInWorld.get(new Random().nextInt(playersInWorld.size()));
            Location spawnLoc = randomPlayer.getLocation().add(
                    (new Random().nextInt(20) - 10),
                    0,
                    (new Random().nextInt(20) - 10)
            );

            Entity entity = mobManager.spawnCustomMob(
                    waveConfig.getMobType(),
                    spawnLoc
            );

            if (entity instanceof LivingEntity) {
                LivingEntity mob = (LivingEntity) entity;
                // Set custom properties for invasion mob
                mob.customName(Component.text("§c" + waveConfig.getMobType() + " Lv." + waveConfig.getMobLevel()));
                mob.setCustomNameVisible(true);
                waveMobs.add(mob);
                session.setTotalMobsSpawned(session.getTotalMobsSpawned() + 1);
            }
        }

        // Spawn boss if boss wave
        if (waveConfig.isBossWave()) {
            Player randomPlayer = playersInWorld.get(new Random().nextInt(playersInWorld.size()));
            Location bossLoc = randomPlayer.getLocation().add(0, 5, 0);
            
            Entity bossEntity = mobManager.spawnCustomMob(
                    waveConfig.getMobType(),
                    bossLoc
            );

            if (bossEntity instanceof LivingEntity) {
                LivingEntity boss = (LivingEntity) bossEntity;
                boss.customName(Component.text("§c§l" + waveConfig.getBossName()));
                boss.setCustomNameVisible(true);
                double oldMaxHealth = boss.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                double newMaxHealth = oldMaxHealth * waveConfig.getBossHealthMultiplier();
                boss.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
                boss.setHealth(newMaxHealth);
                waveMobs.add(boss);
                session.setTotalMobsSpawned(session.getTotalMobsSpawned() + 1);
            }
        }
    }

    /**
     * Check wave completion
     */
    private void checkWaveCompletion(InvasionSession session, InvasionConfig config, int waveIndex) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeSessions.containsKey(session.getSessionId())) {
                    cancel();
                    return;
                }

                List<Entity> mobs = invasionMobs.get(session.getSessionId());
                mobs.removeIf(entity -> entity == null || entity.isDead());

                if (mobs.isEmpty()) {
                    // Wave completed
                    giveWaveRewards(session, config);
                    
                    // Schedule next wave
                    scheduleWave(session, config, waveIndex + 1);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }

    /**
     * Give wave completion rewards
     */
    private void giveWaveRewards(InvasionSession session, InvasionConfig config) {
        InvasionConfig.InvasionRewards rewards = config.getRewards();
        EconomyManager economy = plugin.getEconomyManager();

        // Give rewards to all participants
        for (Map.Entry<UUID, Integer> entry : session.getPlayerKills().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                int xp = rewards.getXpPerWave();
                int coins = rewards.getCoinsPerWave();
                
                player.sendMessage("§a§l[INVASIÓN] §7Recompensa: §e+" + xp + " XP §7y §6" + coins + " monedas");
                // Would call RPGManager to add XP
                economy.deposit(player, coins);
            }
        }
    }

    /**
     * Complete invasion
     */
    private void completeInvasion(InvasionSession session, InvasionConfig config, boolean success) {
        session.complete(success);
        
        // Give completion rewards
        if (success) {
            InvasionConfig.InvasionRewards rewards = config.getRewards();
            EconomyManager economy = plugin.getEconomyManager();

            for (Map.Entry<UUID, Integer> entry : session.getPlayerKills().entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    int xpBonus = rewards.getXpBonus();
                    int coinsBonus = rewards.getCoinsBonus();
                    
                    player.sendMessage("§a§l[INVASIÓN COMPLETADA] §7Bonus: §e+" + xpBonus + " XP §7y §6" + coinsBonus + " monedas");
                    economy.deposit(player, coinsBonus);
                }
            }

            Audience audience = Bukkit.getServer();
            audience.sendMessage(Component.text("§a§l[INVASIÓN] §7¡La invasión de §e" + config.getDisplayName() + " §7ha sido derrotada!"));
        } else {
            Audience audience = Bukkit.getServer();
            audience.sendMessage(Component.text("§c§l[INVASIÓN] §7La invasión de §e" + config.getDisplayName() + " §7ha fracasado..."));
        }

        // Save to database
        saveInvasionHistory(session);

        // Cleanup
        invasionMobs.remove(session.getSessionId());
        activeSessions.remove(session.getSessionId());
    }

    /**
     * Save invasion to history
     */
    private void saveInvasionHistory(InvasionSession session) {
        try {
            String sql = "INSERT INTO invasion_history (session_id, invasion_id, world_name, start_time, " +
                    "end_time, total_waves, completed_waves, status, total_mobs_killed, total_mobs_spawned, " +
                    "success, duration_seconds, top_player_uuid, top_player_kills) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Find top player
            UUID topPlayer = null;
            int topKills = 0;
            for (Map.Entry<UUID, Integer> entry : session.getPlayerKills().entrySet()) {
                if (entry.getValue() > topKills) {
                    topKills = entry.getValue();
                    topPlayer = entry.getKey();
                }
            }

            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, session.getSessionId());
                stmt.setString(2, session.getInvasionId());
                stmt.setString(3, session.getWorldName());
                stmt.setLong(4, session.getStartTime().getTime());
                stmt.setLong(5, session.getEndTime() != null ? session.getEndTime().getTime() : 0);
                stmt.setInt(6, session.getTotalWaves());
                stmt.setInt(7, session.getCurrentWave());
                stmt.setString(8, session.getStatus());
                stmt.setInt(9, session.getTotalMobsKilled());
                stmt.setInt(10, session.getTotalMobsSpawned());
                stmt.setInt(11, session.isSuccess() ? 1 : 0);
                stmt.setLong(12, session.getDurationSeconds());
                stmt.setString(13, topPlayer != null ? topPlayer.toString() : null);
                stmt.setInt(14, topKills);
                stmt.executeUpdate();
            }

            // Save participants
            String participantSql = "INSERT INTO invasion_participants (session_id, player_uuid, kills) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(participantSql)) {
                for (Map.Entry<UUID, Integer> entry : session.getPlayerKills().entrySet()) {
                    stmt.setString(1, session.getSessionId());
                    stmt.setString(2, entry.getKey().toString());
                    stmt.setInt(3, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

        } catch (SQLException e) {
            logger.severe("Failed to save invasion history: " + e.getMessage());
        }
    }

    /**
     * Check if invasion is active in world
     */
    private boolean isInvasionActiveInWorld(String worldName) {
        return activeSessions.values().stream()
                .anyMatch(session -> session.getWorldName().equals(worldName) && "ACTIVE".equals(session.getStatus()));
    }

    /**
     * Handle mob death during invasion
     */
    public void handleInvasionMobDeath(Entity mob, UUID killerUuid) {
        for (Map.Entry<String, List<Entity>> entry : invasionMobs.entrySet()) {
            if (entry.getValue().contains(mob)) {
                InvasionSession session = activeSessions.get(entry.getKey());
                if (session != null) {
                    session.addPlayerKill(killerUuid);
                }
                break;
            }
        }
    }

    /**
     * Cancel invasion
     */
    public boolean cancelInvasion(String sessionId) {
        InvasionSession session = activeSessions.get(sessionId);
        if (session == null) return false;

        session.cancel();
        
        // Remove all invasion mobs
        List<Entity> mobs = invasionMobs.get(sessionId);
        if (mobs != null) {
            mobs.forEach(Entity::remove);
        }

        saveInvasionHistory(session);
        invasionMobs.remove(sessionId);
        activeSessions.remove(sessionId);

        Audience audience = Bukkit.getServer();
        audience.sendMessage(Component.text("§c§l[INVASIÓN] §7La invasión ha sido cancelada por un administrador."));
        return true;
    }

    // Getters
    public Map<String, InvasionConfig> getInvasionConfigs() { return invasionConfigs; }
    public Map<String, InvasionSession> getActiveSessions() { return activeSessions; }
    public Connection getDbConnection() { return dbConnection; }

    /**
     * Save and cleanup
     */
    public void shutdown() {
        // Cancel all scheduled tasks
        scheduledTasks.values().forEach(BukkitTask::cancel);
        
        // Save all active sessions
        activeSessions.values().forEach(session -> {
            session.cancel();
            saveInvasionHistory(session);
        });

        // NO cerrar la conexión compartida de DatabaseManager
        // La conexión será cerrada por DatabaseManager cuando el plugin se deshabilite
    }
}
