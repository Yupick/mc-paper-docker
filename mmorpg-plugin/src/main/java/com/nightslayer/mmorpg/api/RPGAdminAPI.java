package com.nightslayer.mmorpg.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.npcs.CustomNPC;
import com.nightslayer.mmorpg.npcs.NPCType;
import com.nightslayer.mmorpg.quests.Quest;
import com.nightslayer.mmorpg.quests.QuestObjective;
import com.nightslayer.mmorpg.quests.QuestObjectiveType;
import com.nightslayer.mmorpg.quests.QuestReward;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * API para administración del plugin desde el panel web
 */
public class RPGAdminAPI {
    private final MMORPGPlugin plugin;
    private final File apiFolder;
    
    public RPGAdminAPI(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.apiFolder = new File(plugin.getDataFolder(), "api");
        
        if (!apiFolder.exists()) {
            apiFolder.mkdirs();
        }
    }
    
    /**
     * Procesa comandos desde archivos JSON
     */
    public void processCommands() {
        File commandsFile = new File(apiFolder, "commands.json");
        if (!commandsFile.exists()) {
            return;
        }
        
        try {
            String content = Files.readString(commandsFile.toPath());
            JsonObject commands = JsonParser.parseString(content).getAsJsonObject();
            
            if (commands.has("createQuest")) {
                processCreateQuest(commands.getAsJsonObject("createQuest"));
            }
            
            if (commands.has("updateQuest")) {
                processUpdateQuest(commands.getAsJsonObject("updateQuest"));
            }
            
            if (commands.has("deleteQuest")) {
                processDeleteQuest(commands.get("deleteQuest").getAsString());
            }
            
            if (commands.has("createNPC")) {
                processCreateNPC(commands.getAsJsonObject("createNPC"));
            }
            
            if (commands.has("updateNPC")) {
                processUpdateNPC(commands.getAsJsonObject("updateNPC"));
            }
            
            if (commands.has("deleteNPC")) {
                processDeleteNPC(commands.get("deleteNPC").getAsString());
            }
            
            // Borrar archivo de comandos después de procesar
            commandsFile.delete();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error procesando comandos API: " + e.getMessage());
        }
    }
    
    /**
     * Procesa creación de quest
     */
    private void processCreateQuest(JsonObject data) {
        try {
            String id = data.get("id").getAsString();
            String name = data.get("name").getAsString();
            String description = data.get("description").getAsString();
            int minLevel = data.get("minLevel").getAsInt();
            String npcGiverId = data.has("npcGiverId") ? data.get("npcGiverId").getAsString() : "";
            
            Quest.QuestDifficulty difficulty = Quest.QuestDifficulty.valueOf(
                data.get("difficulty").getAsString().toUpperCase()
            );
            
            // Crear quest con constructor correcto
            Quest quest = new Quest(id, name, description, minLevel, npcGiverId, difficulty);
            
            // Añadir objetivos
            if (data.has("objectives")) {
                JsonArray objectivesArray = data.getAsJsonArray("objectives");
                for (int i = 0; i < objectivesArray.size(); i++) {
                    JsonObject obj = objectivesArray.get(i).getAsJsonObject();
                    String objId = obj.has("id") ? obj.get("id").getAsString() : ("obj_" + i);
                    quest.addObjective(new QuestObjective(
                        objId,
                        QuestObjectiveType.valueOf(obj.get("type").getAsString().toUpperCase()),
                        obj.get("target").getAsString(),
                        obj.get("amount").getAsInt()
                    ));
                }
            }
            
            // Añadir recompensas
            if (data.has("rewards")) {
                JsonArray rewardsArray = data.getAsJsonArray("rewards");
                for (int i = 0; i < rewardsArray.size(); i++) {
                    JsonObject rew = rewardsArray.get(i).getAsJsonObject();
                    quest.addReward(new QuestReward(
                        QuestReward.RewardType.valueOf(rew.get("type").getAsString().toUpperCase()),
                        (int) rew.get("value").getAsDouble()
                    ));
                }
            }
            
            // Configurar propiedades adicionales
            if (data.has("repeatable")) {
                quest.setRepeatable(data.get("repeatable").getAsBoolean());
            }
            
            if (data.has("cooldown")) {
                quest.setCooldownTime(data.get("cooldown").getAsLong());
            }
            
            // Registrar quest
            plugin.getQuestManager().registerQuest(quest);
            plugin.getLogger().info("Quest creada via API: " + id);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error creando quest: " + e.getMessage());
        }
    }
    
    /**
     * Procesa actualización de quest
     */
    private void processUpdateQuest(JsonObject data) {
        try {
            String id = data.get("id").getAsString();
            Quest quest = plugin.getQuestManager().getQuest(id);
            
            if (quest == null) {
                plugin.getLogger().warning("Quest no encontrada: " + id);
                return;
            }
            
            // Actualizar campos modificables
            if (data.has("repeatable")) {
                quest.setRepeatable(data.get("repeatable").getAsBoolean());
            }
            
            if (data.has("cooldown")) {
                quest.setCooldownTime(data.get("cooldown").getAsLong());
            }
            
            plugin.getLogger().info("Quest actualizada via API: " + id);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando quest: " + e.getMessage());
        }
    }
    
    /**
     * Procesa eliminación de quest
     */
    private void processDeleteQuest(String questId) {
        try {
            plugin.getQuestManager().unregisterQuest(questId);
            plugin.getLogger().info("Quest eliminada via API: " + questId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error eliminando quest: " + e.getMessage());
        }
    }
    
    /**
     * Procesa creación de NPC
     */
    private void processCreateNPC(JsonObject data) {
        try {
            String id = data.get("id").getAsString();
            String name = data.get("name").getAsString();
            NPCType type = NPCType.valueOf(data.get("type").getAsString().toUpperCase());
            
            // Parsear ubicación
            JsonObject locData = data.getAsJsonObject("location");
            World world = Bukkit.getWorld(locData.get("world").getAsString());
            
            if (world == null) {
                plugin.getLogger().warning("Mundo no encontrado: " + locData.get("world").getAsString());
                return;
            }
            
            Location location = new Location(
                world,
                locData.get("x").getAsDouble(),
                locData.get("y").getAsDouble(),
                locData.get("z").getAsDouble(),
                locData.get("yaw").getAsFloat(),
                locData.get("pitch").getAsFloat()
            );
            
            // Determinar tipo de entidad
            EntityType entityType = data.has("entityType") ? 
                EntityType.valueOf(data.get("entityType").getAsString().toUpperCase()) : 
                EntityType.VILLAGER;
            
            // Crear NPC
            CustomNPC npc = new CustomNPC(id, name, type, location, entityType);
            
            // Asociar quest si existe
            if (data.has("questId")) {
                npc.setQuestId(data.get("questId").getAsString());
            }
            
            // Registrar y spawnear NPC
            plugin.getNPCManager().registerNPC(npc);
            npc.spawn();
            
            plugin.getLogger().info("NPC creado via API: " + id);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error creando NPC: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Procesa actualización de NPC
     */
    private void processUpdateNPC(JsonObject data) {
        try {
            String id = data.get("id").getAsString();
            CustomNPC npc = plugin.getNPCManager().getNPC(id);
            
            if (npc == null) {
                plugin.getLogger().warning("NPC no encontrado: " + id);
                return;
            }
            
            // Actualizar quest asociada
            if (data.has("questId")) {
                npc.setQuestId(data.get("questId").getAsString());
            }
            
            // Si cambió la ubicación, mover NPC
            if (data.has("location")) {
                JsonObject locData = data.getAsJsonObject("location");
                World world = Bukkit.getWorld(locData.get("world").getAsString());
                
                if (world != null) {
                    Location newLocation = new Location(
                        world,
                        locData.get("x").getAsDouble(),
                        locData.get("y").getAsDouble(),
                        locData.get("z").getAsDouble(),
                        locData.get("yaw").getAsFloat(),
                        locData.get("pitch").getAsFloat()
                    );
                    
                    npc.despawn();
                    npc.setLocation(newLocation);
                    npc.spawn();
                }
            }
            
            plugin.getLogger().info("NPC actualizado via API: " + id);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando NPC: " + e.getMessage());
        }
    }
    
    /**
     * Procesa eliminación de NPC
     */
    private void processDeleteNPC(String npcId) {
        try {
            CustomNPC npc = plugin.getNPCManager().getNPC(npcId);
            if (npc != null) {
                npc.despawn();
                plugin.getNPCManager().removeNPC(npcId);
                plugin.getLogger().info("NPC eliminado via API: " + npcId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error eliminando NPC: " + e.getMessage());
        }
    }
    
    /**
     * Exporta el estado actual para confirmación
     */
    public void exportStatus() {
        JsonObject status = new JsonObject();
        status.addProperty("status", "ready");
        status.addProperty("timestamp", System.currentTimeMillis());
        status.addProperty("questCount", plugin.getQuestManager().getAllQuests().size());
        status.addProperty("npcCount", plugin.getNPCManager().getAllNPCs().size());
        
        try {
            File statusFile = new File(apiFolder, "status.json");
            Files.writeString(statusFile.toPath(), plugin.getGson().toJson(status));
        } catch (IOException e) {
            plugin.getLogger().warning("Error exportando status API: " + e.getMessage());
        }
    }
}
