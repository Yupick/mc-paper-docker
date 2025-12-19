package com.nightslayer.mmorpg.bestiary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor principal del sistema de bestiario
 */
public class BestiaryManager {
    private final MMORPGPlugin plugin;
    private final Gson gson;
    private final Map<UUID, Bestiary> bestiaries;
    private final Map<String, BestiaryCategory> categories;
    private final Map<String, BestiaryReward> tierRewards;
    private int[] progressThresholds;
    private boolean enabled;

    public BestiaryManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        this.bestiaries = new ConcurrentHashMap<>();
        this.categories = new HashMap<>();
        this.tierRewards = new HashMap<>();
        this.progressThresholds = new int[]{0, 25, 50, 75, 100, 500, 1000};
        this.enabled = true;
        
        loadConfiguration();
        createDatabaseTable();
    }

    /**
     * Carga la configuración desde bestiary_config.json
     */
    private void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "data/bestiary_config.json");
        
        if (!configFile.exists()) {
            createDefaultConfiguration(configFile);
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject config = gson.fromJson(reader, JsonObject.class);
            
            // Cargar thresholds
            if (config.has("progressThresholds")) {
                List<Integer> thresholds = new ArrayList<>();
                config.getAsJsonArray("progressThresholds").forEach(e -> 
                    thresholds.add(e.getAsInt()));
                progressThresholds = thresholds.stream().mapToInt(i -> i).toArray();
            }

            // Cargar categorías
            if (config.has("categories")) {
                JsonObject categoriesObj = config.getAsJsonObject("categories");
                categoriesObj.entrySet().forEach(entry -> {
                    String catId = entry.getKey();
                    JsonObject catData = entry.getValue().getAsJsonObject();
                    
                    String name = catData.get("name").getAsString();
                    String description = catData.has("description") ? 
                        catData.get("description").getAsString() : "";
                    
                    List<String> mobIds = new ArrayList<>();
                    catData.getAsJsonArray("mobs").forEach(e -> mobIds.add(e.getAsString()));
                    
                    BestiaryReward reward = null;
                    if (catData.has("completionReward")) {
                        JsonObject rewardData = catData.getAsJsonObject("completionReward");
                        reward = parseReward(catId + "_completion", rewardData);
                    }
                    
                    categories.put(catId, new BestiaryCategory(catId, name, description, mobIds, reward));
                });
            }

            // Cargar recompensas por tier
            if (config.has("discoveryRewards")) {
                JsonObject rewards = config.getAsJsonObject("discoveryRewards");
                rewards.entrySet().forEach(entry -> {
                    String tierId = entry.getKey();
                    int xp = entry.getValue().getAsInt();
                    BestiaryReward reward = new BestiaryReward(
                        tierId, "Tier " + tierId, xp, 0, null, null, false
                    );
                    tierRewards.put(tierId, reward);
                });
            }

            enabled = config.has("enabled") ? config.get("enabled").getAsBoolean() : true;
            
            plugin.getLogger().info("Configuración de bestiario cargada: " + 
                categories.size() + " categorías, " + 
                tierRewards.size() + " tier rewards");
                
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar configuración de bestiario", e);
        }
    }

    /**
     * Parsea una recompensa desde JSON
     */
    private BestiaryReward parseReward(String id, JsonObject rewardData) {
        String name = rewardData.has("name") ? rewardData.get("name").getAsString() : id;
        int xp = rewardData.has("xp") ? rewardData.get("xp").getAsInt() : 0;
        int coins = rewardData.has("coins") ? rewardData.get("coins").getAsInt() : 0;
        String title = rewardData.has("title") ? rewardData.get("title").getAsString() : null;
        boolean broadcast = rewardData.has("broadcast") ? rewardData.get("broadcast").getAsBoolean() : false;
        
        List<String> items = new ArrayList<>();
        if (rewardData.has("items")) {
            rewardData.getAsJsonArray("items").forEach(e -> items.add(e.getAsString()));
        } else if (rewardData.has("item")) {
            items.add(rewardData.get("item").getAsString());
        }
        
        return new BestiaryReward(id, name, xp, coins, title, items, broadcast);
    }

    /**
     * Crea la configuración por defecto
     */
    private void createDefaultConfiguration(File configFile) {
        configFile.getParentFile().mkdirs();
        
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", true);
        config.put("progressThresholds", Arrays.asList(0, 25, 50, 75, 100, 500, 1000));
        
        Map<String, Integer> discoveryRewards = new HashMap<>();
        discoveryRewards.put("firstKill", 100);
        discoveryRewards.put("tier1", 250);
        discoveryRewards.put("tier2", 500);
        discoveryRewards.put("tier3", 1000);
        discoveryRewards.put("tier4", 2500);
        config.put("discoveryRewards", discoveryRewards);
        
        Map<String, Object> categories = new HashMap<>();
        
        Map<String, Object> undead = new HashMap<>();
        undead.put("name", "No-muertos");
        undead.put("description", "Criaturas que volvieron de la muerte");
        undead.put("mobs", Arrays.asList("zombie_elite", "skeleton_archer", "zombie_bruja"));
        Map<String, Object> undeadReward = new HashMap<>();
        undeadReward.put("title", "Cazador de No-muertos");
        undeadReward.put("xp", 5000);
        undeadReward.put("item", "legendary_sword");
        undead.put("completionReward", undeadReward);
        categories.put("undead", undead);
        
        Map<String, Object> beasts = new HashMap<>();
        beasts.put("name", "Bestias");
        beasts.put("description", "Criaturas salvajes y peligrosas");
        beasts.put("mobs", Arrays.asList("spider_giant", "wolf_alpha"));
        Map<String, Object> beastsReward = new HashMap<>();
        beastsReward.put("title", "Domador de Bestias");
        beastsReward.put("xp", 5000);
        beastsReward.put("item", "beast_tamer_staff");
        beasts.put("completionReward", beastsReward);
        categories.put("beasts", beasts);
        
        config.put("categories", categories);
        
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
            plugin.getLogger().info("Configuración por defecto de bestiario creada");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear configuración de bestiario", e);
        }
    }

    /**
     * Crea la tabla de bestiario en la base de datos
     */
    private void createDatabaseTable() {
        String createTable = "CREATE TABLE IF NOT EXISTS player_bestiary (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "player_uuid TEXT NOT NULL," +
            "mob_id TEXT NOT NULL," +
            "kills INTEGER DEFAULT 0," +
            "first_kill_date TEXT," +
            "last_kill_date TEXT," +
            "current_tier INTEGER DEFAULT 0," +
            "discovered BOOLEAN DEFAULT 0," +
            "UNIQUE(player_uuid, mob_id)" +
            ")";
        
        String createIndex = "CREATE INDEX IF NOT EXISTS idx_player_bestiary " +
            "ON player_bestiary(player_uuid)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            stmt.execute(createIndex);
            plugin.getLogger().info("Tabla de bestiario creada/verificada");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al crear tabla de bestiario", e);
        }
    }

    /**
     * Carga el bestiario de un jugador desde la BD
     */
    public Bestiary loadBestiary(UUID playerUUID) {
        Bestiary bestiary = new Bestiary(playerUUID);
        
        String query = "SELECT * FROM player_bestiary WHERE player_uuid = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String mobId = rs.getString("mob_id");
                int kills = rs.getInt("kills");
                String firstKillStr = rs.getString("first_kill_date");
                String lastKillStr = rs.getString("last_kill_date");
                int tier = rs.getInt("current_tier");
                boolean discovered = rs.getBoolean("discovered");
                
                LocalDateTime firstKill = firstKillStr != null ? 
                    LocalDateTime.parse(firstKillStr) : null;
                LocalDateTime lastKill = lastKillStr != null ? 
                    LocalDateTime.parse(lastKillStr) : null;
                
                BestiaryEntry entry = new BestiaryEntry(mobId, kills, firstKill, lastKill, tier, discovered);
                bestiary.addEntry(entry);
            }
            
            bestiary.recalculateTotals();
            bestiaries.put(playerUUID, bestiary);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cargar bestiario de " + playerUUID, e);
        }
        
        return bestiary;
    }

    /**
     * Guarda el bestiario de un jugador en la BD
     */
    public void saveBestiary(UUID playerUUID) {
        Bestiary bestiary = bestiaries.get(playerUUID);
        if (bestiary == null) return;

        String upsert = "INSERT OR REPLACE INTO player_bestiary " +
            "(player_uuid, mob_id, kills, first_kill_date, last_kill_date, current_tier, discovered) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(upsert)) {
            
            for (BestiaryEntry entry : bestiary.getEntries().values()) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, entry.getMobId());
                stmt.setInt(3, entry.getKills());
                stmt.setString(4, entry.getFirstKillDate() != null ? 
                    entry.getFirstKillDate().toString() : null);
                stmt.setString(5, entry.getLastKillDate() != null ? 
                    entry.getLastKillDate().toString() : null);
                stmt.setInt(6, entry.getCurrentTier());
                stmt.setBoolean(7, entry.isDiscovered());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar bestiario de " + playerUUID, e);
        }
    }

    /**
     * Registra una kill de mob para un jugador
     */
    public void recordMobKill(Player player, String mobId) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();
        Bestiary bestiary = bestiaries.computeIfAbsent(uuid, this::loadBestiary);
        
        boolean isNewDiscovery = bestiary.recordKill(mobId, progressThresholds);
        
        if (isNewDiscovery) {
            handleNewDiscovery(player, mobId);
        }
        
        // Verificar tier up
        BestiaryEntry entry = bestiary.getEntry(mobId);
        if (entry != null && entry.updateTier(progressThresholds)) {
            handleTierUp(player, mobId, entry.getCurrentTier());
        }
        
        // Verificar completado de categoría
        checkCategoryCompletion(player, mobId);
    }

    /**
     * Maneja el descubrimiento de un nuevo mob
     */
    private void handleNewDiscovery(Player player, String mobId) {
        BestiaryReward reward = tierRewards.get("firstKill");
        if (reward != null && reward.getXp() > 0) {
            player.giveExp(reward.getXp());
            player.sendMessage("§a✦ Nuevo descubrimiento: §e" + mobId + " §a(+" + reward.getXp() + " XP)");
        }
    }

    /**
     * Maneja la subida de tier de un mob
     */
    private void handleTierUp(Player player, String mobId, int newTier) {
        String tierKey = "tier" + newTier;
        BestiaryReward reward = tierRewards.get(tierKey);
        
        if (reward != null && reward.getXp() > 0) {
            player.giveExp(reward.getXp());
            player.sendMessage("§6⬆ Tier " + newTier + " alcanzado para " + mobId + " §6(+" + reward.getXp() + " XP)");
        }
    }

    /**
     * Verifica si se completó una categoría
     */
    private void checkCategoryCompletion(Player player, String mobId) {
        UUID uuid = player.getUniqueId();
        Bestiary bestiary = bestiaries.get(uuid);
        if (bestiary == null) return;

        for (BestiaryCategory category : categories.values()) {
            if (category.containsMob(mobId) && 
                category.isCompleted(bestiary.getDiscoveredMobs())) {
                
                grantCategoryReward(player, category);
            }
        }
    }

    /**
     * Otorga la recompensa por completar una categoría
     */
    private void grantCategoryReward(Player player, BestiaryCategory category) {
        BestiaryReward reward = category.getCompletionReward();
        if (reward == null) return;

        if (reward.getXp() > 0) {
            player.giveExp(reward.getXp());
        }
        
        if (reward.shouldBroadcast()) {
            net.kyori.adventure.audience.Audience audience = plugin.getServer();
            net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.text("§6✦ " + player.getName() + " §eha completado la categoría §6" + category.getName() + " §edel bestiario!");
            audience.sendMessage(msg);
        } else {
            player.sendMessage("§6✦ Categoría completada: §e" + category.getName());
        }
    }

    /**
     * Obtiene el bestiario de un jugador
     */
    public Bestiary getBestiary(UUID playerUUID) {
        return bestiaries.computeIfAbsent(playerUUID, this::loadBestiary);
    }

    /**
     * Guarda todos los bestiarios
     */
    public void saveAll() {
        bestiaries.keySet().forEach(this::saveBestiary);
        plugin.getLogger().info("Bestiarios guardados: " + bestiaries.size());
    }

    // Getters
    public Map<String, BestiaryCategory> getCategories() {
        return new HashMap<>(categories);
    }

    public int[] getProgressThresholds() {
        return progressThresholds.clone();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
