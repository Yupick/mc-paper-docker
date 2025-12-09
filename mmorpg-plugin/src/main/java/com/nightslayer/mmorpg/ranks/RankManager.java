package com.nightslayer.mmorpg.ranks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.achievements.AchievementManager;
import org.bukkit.Bukkit;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor principal del sistema de rangos y títulos
 */
public class RankManager {
    private final MMORPGPlugin plugin;
    private final AchievementManager achievementManager;
    private final Gson gson;
    private final Map<String, Rank> ranks;
    private final Map<UUID, String> playerRanks;
    private boolean enabled;

    public RankManager(MMORPGPlugin plugin, AchievementManager achievementManager) {
        this.plugin = plugin;
        this.achievementManager = achievementManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.ranks = new ConcurrentHashMap<>();
        this.playerRanks = new ConcurrentHashMap<>();
        this.enabled = true;

        loadConfiguration();
        createDatabaseTable();
    }

    private void createDatabaseTable() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_ranks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_uuid TEXT UNIQUE NOT NULL, " +
                    "rank_id TEXT, " +
                    "title TEXT, " +
                    "updated_at TEXT" +
                ")"
            );
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear tabla player_ranks", e);
        }
    }

    private void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "data/ranks_config.json");
        if (!configFile.exists()) {
            createDefaultConfiguration(configFile);
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            this.enabled = json.has("enabled") && json.get("enabled").getAsBoolean();

            if (json.has("ranks")) {
                JsonObject ranksObj = json.getAsJsonObject("ranks");
                ranksObj.entrySet().forEach(entry -> {
                    String id = entry.getKey();
                    JsonObject data = entry.getValue().getAsJsonObject();

                    String name = data.get("name").getAsString();
                    String description = data.has("description") ? data.get("description").getAsString() : "";
                    int order = data.has("order") ? data.get("order").getAsInt() : 0;

                    JsonObject reqObj = data.has("requirements") ? data.getAsJsonObject("requirements") : new JsonObject();
                    int requiredLevel = reqObj.has("level") ? reqObj.get("level").getAsInt() : 0;
                    int achievementsNeeded = reqObj.has("achievementsCompleted") ? reqObj.get("achievementsCompleted").getAsInt() : 0;
                    RankRequirement requirement = new RankRequirement(requiredLevel, achievementsNeeded);

                    JsonObject rewardObj = data.has("reward") ? data.getAsJsonObject("reward") : new JsonObject();
                    int xp = rewardObj.has("xp") ? rewardObj.get("xp").getAsInt() : 0;
                    int coins = rewardObj.has("coins") ? rewardObj.get("coins").getAsInt() : 0;
                    String title = rewardObj.has("title") ? rewardObj.get("title").getAsString() : "";
                    boolean broadcast = rewardObj.has("broadcast") && rewardObj.get("broadcast").getAsBoolean();

                    List<String> permissions = new ArrayList<>();
                    if (rewardObj.has("permissions")) {
                        JsonArray arr = rewardObj.getAsJsonArray("permissions");
                        arr.forEach(e -> permissions.add(e.getAsString()));
                    }

                    List<String> items = new ArrayList<>();
                    if (rewardObj.has("items")) {
                        JsonArray arr = rewardObj.getAsJsonArray("items");
                        arr.forEach(e -> items.add(e.getAsString()));
                    } else if (rewardObj.has("item")) {
                        items.add(rewardObj.get("item").getAsString());
                    }

                    RankReward reward = new RankReward(xp, coins, title, permissions, items, broadcast);
                    Rank rank = new Rank(id, name, description, order, requirement, reward);
                    ranks.put(id, rank);
                });
            }

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando ranks_config.json", e);
        }
    }

    private void createDefaultConfiguration(File configFile) {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            root.addProperty("enabled", true);

            JsonObject ranksObj = new JsonObject();

            JsonObject novato = new JsonObject();
            novato.addProperty("name", "Novato");
            novato.addProperty("description", "Primer rango del sistema");
            novato.addProperty("order", 1);
            JsonObject novatoReq = new JsonObject();
            novatoReq.addProperty("level", 1);
            novatoReq.addProperty("achievementsCompleted", 0);
            novato.add("requirements", novatoReq);
            JsonObject novatoReward = new JsonObject();
            novatoReward.addProperty("xp", 100);
            novatoReward.addProperty("coins", 50);
            novatoReward.addProperty("title", "Novato");
            novato.add("reward", novatoReward);
            ranksObj.add("novato", novato);

            JsonObject aventurero = new JsonObject();
            aventurero.addProperty("name", "Aventurero");
            aventurero.addProperty("description", "Supera los primeros retos");
            aventurero.addProperty("order", 2);
            JsonObject aventureroReq = new JsonObject();
            aventureroReq.addProperty("level", 5);
            aventureroReq.addProperty("achievementsCompleted", 1);
            aventurero.add("requirements", aventureroReq);
            JsonObject aventureroReward = new JsonObject();
            aventureroReward.addProperty("xp", 500);
            aventureroReward.addProperty("coins", 200);
            aventureroReward.addProperty("title", "Aventurero");
            aventureroReward.addProperty("broadcast", true);
            aventurero.add("reward", aventureroReward);
            ranksObj.add("aventurero", aventurero);

            JsonObject heroe = new JsonObject();
            heroe.addProperty("name", "Heroe");
            heroe.addProperty("description", "Jugadores comprometidos con el RPG");
            heroe.addProperty("order", 3);
            JsonObject heroeReq = new JsonObject();
            heroeReq.addProperty("level", 12);
            heroeReq.addProperty("achievementsCompleted", 3);
            heroe.add("requirements", heroeReq);
            JsonObject heroeReward = new JsonObject();
            heroeReward.addProperty("xp", 2000);
            heroeReward.addProperty("coins", 1000);
            heroeReward.addProperty("title", "Heroe");
            heroeReward.addProperty("broadcast", true);
            heroe.add("reward", heroeReward);
            ranksObj.add("heroe", heroe);

            root.add("ranks", ranksObj);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo crear ranks_config.json", e);
        }
    }

    private void savePlayerRank(UUID uuid, String rankId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO player_ranks (player_uuid, rank_id, title, updated_at) VALUES (?, ?, ?, ?)");
        ) {
            ps.setString(1, uuid.toString());
            ps.setString(2, rankId);
            Rank rank = ranks.get(rankId);
            ps.setString(3, rank != null && rank.getReward() != null ? rank.getReward().getTitle() : null);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar rango de jugador", e);
        }
    }

    private String loadPlayerRank(UUID uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT rank_id FROM player_ranks WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("rank_id");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo cargar rango de jugador", e);
        }
        return null;
    }

    public Map<String, Rank> getRanks() {
        return new HashMap<>(ranks);
    }

    public String getPlayerRank(UUID uuid) {
        return playerRanks.computeIfAbsent(uuid, this::loadPlayerRank);
    }

    public Rank getRank(String rankId) {
        return ranks.get(rankId);
    }

    public boolean setPlayerRank(Player player, String rankId, boolean applyRewards) {
        if (!enabled || player == null || !ranks.containsKey(rankId)) {
            return false;
        }

        playerRanks.put(player.getUniqueId(), rankId);
        savePlayerRank(player.getUniqueId(), rankId);

        if (applyRewards) {
            applyRewards(player, ranks.get(rankId));
        }
        return true;
    }

    private void applyRewards(Player player, Rank rank) {
        if (rank == null || rank.getReward() == null) {
            return;
        }

        RankReward reward = rank.getReward();
        if (reward.getXp() > 0) {
            player.giveExp(reward.getXp());
        }

        if (reward.getCoins() > 0 && plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().deposit(player, reward.getCoins());
        }

        if (reward.getTitle() != null && !reward.getTitle().isEmpty()) {
            player.sendMessage("§6Nuevo título: §e" + reward.getTitle());
        }

        if (reward.isBroadcast()) {
            Bukkit.broadcastMessage("§b" + player.getName() + " ascendió al rango §e" + rank.getName() + "§b!");
        } else {
            player.sendMessage("§aAscendiste al rango §f" + rank.getName());
        }
    }

    public Rank evaluateAndPromote(Player player) {
        if (!enabled || player == null || ranks.isEmpty()) {
            return null;
        }

        List<Rank> orderedRanks = new ArrayList<>(ranks.values());
        orderedRanks.sort(Comparator.naturalOrder());

        String currentRankId = getPlayerRank(player.getUniqueId());
        int currentOrder = currentRankId != null && ranks.containsKey(currentRankId)
                ? ranks.get(currentRankId).getOrder() : 0;

        Rank bestRank = null;
        for (Rank rank : orderedRanks) {
            if (rank.getOrder() < currentOrder) {
                continue; // ya tiene un rango superior en orden
            }
            if (rank.getRequirement().isMet(player, achievementManager)) {
                bestRank = rank;
            }
        }

        if (bestRank != null && (currentRankId == null || !bestRank.getId().equals(currentRankId))) {
            setPlayerRank(player, bestRank.getId(), true);
            return bestRank;
        }

        return null;
    }

    public Map<String, Object> getPlayerRankInfo(UUID uuid) {
        Map<String, Object> data = new HashMap<>();
        String rankId = getPlayerRank(uuid);
        if (rankId != null && ranks.containsKey(rankId)) {
            Rank rank = ranks.get(rankId);
            data.put("rankId", rank.getId());
            data.put("name", rank.getName());
            data.put("description", rank.getDescription());
            data.put("order", rank.getOrder());
        }
        return data;
    }

    public void saveAll() {
        playerRanks.forEach(this::savePlayerRank);
    }

    public void reload() {
        ranks.clear();
        loadConfiguration();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
