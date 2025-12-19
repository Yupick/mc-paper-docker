package com.nightslayer.mmorpg;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightslayer.mmorpg.classes.PlayerClass;
import com.nightslayer.mmorpg.npcs.CustomNPC;
import com.nightslayer.mmorpg.quests.Quest;
// import com.nightslayer.mmorpg.quests.PlayerQuestProgress;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DataManager {
    
    private final MMORPGPlugin plugin;
    private final Gson gson;
    private final Map<String, JsonObject> worldDataCache;
    
    public DataManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getGson();
        this.worldDataCache = new HashMap<>();
    }
    
    /**
     * Crea el archivo status.json por defecto para un mundo
     */
    public void createDefaultStatusFile(String worldSlug) {
        JsonObject status = new JsonObject();
        status.addProperty("world", worldSlug);
        status.addProperty("rpg_enabled", true);
        status.addProperty("players_online", 0);
        status.addProperty("classes_active", 0);
        status.addProperty("quests_active", 0);
        status.addProperty("npcs_spawned", 0);
        
        saveJsonToFile(status, worldSlug, "status.json");
    }
    
    /**
     * Crea el archivo players.json por defecto para un mundo
     */
    public void createDefaultPlayersFile(String worldSlug) {
        JsonObject players = new JsonObject();
        players.add("players", gson.toJsonTree(new HashMap<String, Object>()));
        
        saveJsonToFile(players, worldSlug, "players.json");
    }
    
    /**
     * Guarda un objeto JSON en un archivo
     */
    private void saveJsonToFile(JsonObject data, String worldSlug, String filename) {
        File worldDataDir = new File(plugin.getDataFolder(), "data/" + worldSlug);
        worldDataDir.mkdirs();
        
        File file = new File(worldDataDir, filename);
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al guardar archivo " + filename + " para mundo " + worldSlug, e);
        }
    }
    
    /**
     * Actualiza el estado del mundo en status.json
     */
    public void updateWorldStatus(String worldSlug) {
        JsonObject status = new JsonObject();
        status.addProperty("world", worldSlug);
        status.addProperty("rpg_enabled", true);
        
        // Contar jugadores en el mundo
        int playersOnline = 0;
        org.bukkit.World world = plugin.getServer().getWorld(worldSlug);
        if (world != null) {
            playersOnline = world.getPlayers().size();
        }
        status.addProperty("players_online", playersOnline);
        
        // Estadísticas de clases activas
        int classesActive = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (world != null && player.getWorld().equals(world)) {
                PlayerClass playerClass = plugin.getClassManager().getPlayerClass(player);
                if (playerClass != null && playerClass.getClassType() != com.nightslayer.mmorpg.classes.ClassType.NONE) {
                    classesActive++;
                }
            }
        }
        status.addProperty("classes_active", classesActive);
        
        // Estadísticas de quests activas
        int questsActive = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (world != null && player.getWorld().equals(world)) {
                questsActive += plugin.getQuestManager().getActiveQuests(player).size();
            }
        }
        status.addProperty("quests_active", questsActive);
        
        // NPCs spawneados
        int npcsSpawned = (int) plugin.getNPCManager().getAllNPCs().stream()
            .filter(CustomNPC::isSpawned)
            .count();
        status.addProperty("npcs_spawned", npcsSpawned);
        
        status.addProperty("last_updated", System.currentTimeMillis());
        
        saveJsonToFile(status, worldSlug, "status.json");
    }
    
    /**
     * Exporta datos para el panel web
     */
    public void exportDataForWebPanel() {
        for (String worldSlug : plugin.getWorldRPGManager().getAllRPGWorlds().keySet()) {
            updateWorldStatus(worldSlug);
            updatePlayersData(worldSlug);
            updateClassesData(worldSlug);
            updateQuestsData(worldSlug);
            updateNPCsData(worldSlug);
        }
    }
    
    /**
     * Actualiza datos de jugadores
     */
    private void updatePlayersData(String worldSlug) {
        JsonObject playersData = new JsonObject();
        org.bukkit.World world = plugin.getServer().getWorld(worldSlug);
        
        if (world != null) {
            for (Player player : world.getPlayers()) {
                JsonObject playerInfo = new JsonObject();
                PlayerClass playerClass = plugin.getClassManager().getPlayerClass(player);
                
                playerInfo.addProperty("name", player.getName());
                playerInfo.addProperty("uuid", player.getUniqueId().toString());
                playerInfo.addProperty("level", playerClass.getLevel());
                playerInfo.addProperty("class", playerClass.getClassType().getDisplayName());
                playerInfo.addProperty("lastSeen", System.currentTimeMillis());
                
                playersData.add(player.getUniqueId().toString(), playerInfo);
            }
        }
        
        saveJsonToFile(playersData, worldSlug, "players.json");
    }
    
    /**
     * Actualiza datos de clases
     */
    private void updateClassesData(String worldSlug) {
        JsonArray classesData = new JsonArray();
        
        for (com.nightslayer.mmorpg.classes.ClassType classType : com.nightslayer.mmorpg.classes.ClassType.values()) {
            if (classType == com.nightslayer.mmorpg.classes.ClassType.NONE) continue;
            
            JsonObject classInfo = new JsonObject();
            classInfo.addProperty("name", classType.getDisplayName());
            classInfo.addProperty("icon", classType.getIcon());
            classInfo.addProperty("description", classType.getDescription());
            classesData.add(classInfo);
        }
        
        JsonObject wrapper = new JsonObject();
        wrapper.add("classes", classesData);
        saveJsonToFile(wrapper, worldSlug, "classes.json");
    }
    
    /**
     * Actualiza datos de quests
     */
    private void updateQuestsData(String worldSlug) {
        JsonArray questsData = new JsonArray();
        
        for (Quest quest : plugin.getQuestManager().getAllQuests()) {
            questsData.add(quest.toJson());
        }
        
        JsonObject wrapper = new JsonObject();
        wrapper.add("quests", questsData);
        saveJsonToFile(wrapper, worldSlug, "quests.json");
    }
    
    /**
     * Actualiza datos de NPCs
     */
    private void updateNPCsData(String worldSlug) {
        JsonArray npcsData = new JsonArray();
        
        for (CustomNPC npc : plugin.getNPCManager().getAllNPCs()) {
            npcsData.add(npc.toJson());
        }
        
        JsonObject wrapper = new JsonObject();
        wrapper.add("npcs", npcsData);
        saveJsonToFile(wrapper, worldSlug, "npcs.json");
    }
    
    /**
     * Guarda todos los datos antes de cerrar el plugin
     */
    public void saveAllData() {
        plugin.getLogger().info("Guardando datos de mundos RPG...");
        exportDataForWebPanel();
    }
    
    /**
     * Obtiene datos de un jugador en un mundo específico
     */
    public JsonObject getPlayerData(Player player, String worldSlug) {
        JsonObject playerData = new JsonObject();
        PlayerClass playerClass = plugin.getClassManager().getPlayerClass(player);
        
        playerData.addProperty("uuid", player.getUniqueId().toString());
        playerData.addProperty("name", player.getName());
        playerData.addProperty("world", worldSlug);
        playerData.addProperty("level", playerClass.getLevel());
        playerData.addProperty("class", playerClass.getClassType().getDisplayName());
        playerData.addProperty("experience", playerClass.getExperience());
        playerData.addProperty("mana", playerClass.getMana());
        playerData.addProperty("maxMana", playerClass.getMaxMana());
        
        // Quests activas
        JsonArray activeQuests = new JsonArray();
        for (Quest quest : plugin.getQuestManager().getActiveQuests(player)) {
            activeQuests.add(quest.getId());
        }
        playerData.add("activeQuests", activeQuests);
        
        return playerData;
    }
}
