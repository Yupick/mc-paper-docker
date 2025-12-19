package com.nightslayer.mmorpg.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor principal del sistema de logros
 */
public class AchievementManager {
    private final MMORPGPlugin plugin;
    private final Gson gson;
    private final Map<String, Achievement> achievements;
    private final Map<UUID, Map<String, AchievementProgress>> progressByPlayer;
    private boolean enabled;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AchievementManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.achievements = new ConcurrentHashMap<>();
        this.progressByPlayer = new ConcurrentHashMap<>();
        this.enabled = true;

        loadConfiguration();
        createDatabaseTable();
    }

    private void createDatabaseTable() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_achievements (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_uuid TEXT NOT NULL, " +
                    "achievement_id TEXT NOT NULL, " +
                    "progress INTEGER DEFAULT 0, " +
                    "completed INTEGER DEFAULT 0, " +
                    "completed_at TEXT, " +
                    "UNIQUE(player_uuid, achievement_id)" +
                ")"
            );
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear tabla player_achievements", e);
        }
    }

    private void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "data/achievements_config.json");
        if (!configFile.exists()) {
            createDefaultConfiguration(configFile);
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            this.enabled = json.has("enabled") && json.get("enabled").getAsBoolean();

            if (json.has("achievements")) {
                JsonObject achObj = json.getAsJsonObject("achievements");
                achObj.entrySet().forEach(entry -> {
                    String id = entry.getKey();
                    JsonObject data = entry.getValue().getAsJsonObject();

                    String name = data.get("name").getAsString();
                    String description = data.has("description") ? data.get("description").getAsString() : "";
                    String triggerTypeStr = data.getAsJsonObject("trigger").get("type").getAsString();
                    Achievement.TriggerType triggerType = Achievement.TriggerType.valueOf(triggerTypeStr.toUpperCase());
                    String mobId = data.getAsJsonObject("trigger").has("mobId") ?
                            data.getAsJsonObject("trigger").get("mobId").getAsString() : null;
                    int target = data.getAsJsonObject("trigger").get("target").getAsInt();

                    AchievementReward reward = new AchievementReward(
                        data.getAsJsonObject("reward").has("xp") ? data.getAsJsonObject("reward").get("xp").getAsInt() : 0,
                        data.getAsJsonObject("reward").has("coins") ? data.getAsJsonObject("reward").get("coins").getAsInt() : 0,
                        data.getAsJsonObject("reward").has("title") ? data.getAsJsonObject("reward").get("title").getAsString() : "",
                        data.getAsJsonObject("reward").has("item") ? data.getAsJsonObject("reward").get("item").getAsString() : "",
                        data.getAsJsonObject("reward").has("broadcast") && data.getAsJsonObject("reward").get("broadcast").getAsBoolean()
                    );

                    Achievement achievement = new Achievement(id, name, description, triggerType, mobId, target, reward);
                    achievements.put(id, achievement);
                });
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando achievements_config.json", e);
        }
    }

    private void createDefaultConfiguration(File configFile) {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            root.addProperty("enabled", true);

            JsonObject achievementsObj = new JsonObject();

            JsonObject killerNovato = new JsonObject();
            killerNovato.addProperty("name", "Cazador Novato");
            killerNovato.addProperty("description", "Mata 10 mobs cualquiera");
            JsonObject trigger1 = new JsonObject();
            trigger1.addProperty("type", "KILL_ANY");
            trigger1.addProperty("target", 10);
            killerNovato.add("trigger", trigger1);
            JsonObject reward1 = new JsonObject();
            reward1.addProperty("xp", 250);
            reward1.addProperty("coins", 100);
            reward1.addProperty("title", "Novato");
            achievementsObj.add("killer_1", killerNovato);
            killerNovato.add("reward", reward1);

            JsonObject dragonSlayer = new JsonObject();
            dragonSlayer.addProperty("name", "Asesino de Dragones");
            dragonSlayer.addProperty("description", "Derrota al Ender Dragon");
            JsonObject trigger2 = new JsonObject();
            trigger2.addProperty("type", "KILL_MOB");
            trigger2.addProperty("mobId", "ender_dragon");
            trigger2.addProperty("target", 1);
            dragonSlayer.add("trigger", trigger2);
            JsonObject reward2 = new JsonObject();
            reward2.addProperty("xp", 5000);
            reward2.addProperty("title", "Dragon Slayer");
            reward2.addProperty("broadcast", true);
            dragonSlayer.add("reward", reward2);

            achievementsObj.add("dragon_slayer", dragonSlayer);

            root.add("achievements", achievementsObj);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear achievements_config.json", e);
        }
    }

    public Map<String, Achievement> getAchievements() {
        return new HashMap<>(achievements);
    }

    public Map<String, AchievementProgress> getProgress(UUID playerUUID) {
        ensureProgressLoaded(playerUUID);
        return progressByPlayer.getOrDefault(playerUUID, new HashMap<>());
    }

    private void ensureProgressLoaded(UUID playerUUID) {
        if (progressByPlayer.containsKey(playerUUID)) {
            return;
        }

        Map<String, AchievementProgress> progressMap = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT achievement_id, progress, completed, completed_at FROM player_achievements WHERE player_uuid = ?")) {
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AchievementProgress progress = new AchievementProgress(rs.getString("achievement_id"));
                progress.setProgress(rs.getInt("progress"));
                progress.setCompleted(rs.getInt("completed") == 1);
                String completedAt = rs.getString("completed_at");
                if (completedAt != null) {
                    progress.setCompletedAt(LocalDateTime.parse(completedAt, formatter));
                }
                progressMap.put(progress.getAchievementId(), progress);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo cargar progreso de logros", e);
        }

        progressByPlayer.put(playerUUID, progressMap);
    }

    public void recordKill(Player player, String mobId) {
        if (!enabled || player == null) {
            return;
        }

        ensureProgressLoaded(player.getUniqueId());
        Map<String, AchievementProgress> progressMap = progressByPlayer.get(player.getUniqueId());

        achievements.values().forEach(achievement -> {
            if (achievement.getTriggerType() == Achievement.TriggerType.KILL_ANY
                    || (achievement.getTriggerType() == Achievement.TriggerType.KILL_MOB
                    && achievement.getMobId() != null
                    && achievement.getMobId().equalsIgnoreCase(mobId))) {

                AchievementProgress progress = progressMap.computeIfAbsent(achievement.getId(), AchievementProgress::new);
                boolean completedNow = progress.incrementAndCheck(achievement.getTarget());

                if (completedNow) {
                    handleCompletion(player, achievement, progress);
                }

                saveProgress(player.getUniqueId(), progress);
            }
        });
    }

    private void handleCompletion(Player player, Achievement achievement, AchievementProgress progress) {
        AchievementReward reward = achievement.getReward();

        if (reward.getXp() > 0) {
            player.giveExp(reward.getXp());
        }

        if (reward.getCoins() > 0 && plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().deposit(player, reward.getCoins());
        }

        if (reward.getTitle() != null && !reward.getTitle().isEmpty()) {
            player.sendMessage("§6¡Has obtenido el título: §e" + reward.getTitle() + "§6!");
        }

        if (reward.isBroadcast()) {
            net.kyori.adventure.audience.Audience audience = org.bukkit.Bukkit.getServer();
            net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.text("§b" + player.getName() + " completó el logro §e" + achievement.getName() + "§b!");
            audience.sendMessage(msg);
        } else {
            player.sendMessage("§aLogro completado: §f" + achievement.getName());
        }

        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
    }

    private void saveProgress(UUID playerUUID, AchievementProgress progress) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO player_achievements " +
                             "(player_uuid, achievement_id, progress, completed, completed_at) VALUES (?, ?, ?, ?, ?)");
        ) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, progress.getAchievementId());
            ps.setInt(3, progress.getProgress());
            ps.setInt(4, progress.isCompleted() ? 1 : 0);
            ps.setString(5, progress.getCompletedAt() != null ? formatter.format(progress.getCompletedAt()) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar progreso de logro", e);
        }
    }

    public void saveAll() {
        progressByPlayer.forEach((uuid, map) -> map.values().forEach(progress -> saveProgress(uuid, progress)));
    }

    public void reloadConfiguration() {
        achievements.clear();
        loadConfiguration();
    }
}
