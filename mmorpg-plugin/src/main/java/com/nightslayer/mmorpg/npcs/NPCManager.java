package com.nightslayer.mmorpg.npcs;

import com.nightslayer.mmorpg.RPGPathResolver;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Gestiona todos los NPCs del sistema RPG
 * Lee desde: database universal.db (tabla npcs)
 */
public class NPCManager implements Listener {
    private final MMORPGPlugin plugin;
    private final RPGPathResolver pathResolver;
    private final DatabaseManager databaseManager;
    private final Map<String, CustomNPC> npcs;
    private final Map<UUID, String> playerDialogueState; // UUID del jugador -> ID del diálogo actual
    private final Gson gson;
    
    public NPCManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.pathResolver = plugin.getWorldRPGManager().getPathResolver();
        this.databaseManager = plugin.getDatabaseManager();
        this.npcs = new HashMap<>();
        this.playerDialogueState = new HashMap<>();
        this.gson = new Gson();
        
        loadNPCs();
        createDefaultNPCs();
    }
    
    /**
     * Carga NPCs desde la base de datos universal
     */
    private void loadNPCs() {
        npcs.clear();
        
        try {
            ResultSet rs = databaseManager.executeQuery("SELECT * FROM npcs");
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String typeStr = rs.getString("type");
                String entityTypeStr = rs.getString("entity_type");
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                String questId = rs.getString("quest_id");
                String initialDialogueId = rs.getString("initial_dialogue_id");
                
                // Buscar el mundo
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Mundo no encontrado para NPC " + id + ": " + worldName);
                    continue;
                }
                
                Location location = new Location(world, x, y, z, yaw, pitch);
                NPCType type = NPCType.valueOf(typeStr);
                EntityType entityType = EntityType.valueOf(entityTypeStr);
                
                CustomNPC npc = new CustomNPC(id, name, type, location, entityType);
                npc.setQuestGiverId(questId);
                
                // Cargar diálogos
                loadNPCDialogues(npc);
                
                // Cargar comercio
                loadNPCTrades(npc);
                
                npcs.put(id, npc);
                plugin.getLogger().info("Cargado NPC: " + id + " - " + name);
            }
            
            rs.close();
            plugin.getLogger().info("Cargados " + npcs.size() + " NPCs desde la base de datos");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cargar NPCs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga diálogos de un NPC desde la base de datos
     */
    private void loadNPCDialogues(CustomNPC npc) throws SQLException {
        ResultSet rs = databaseManager.executeQuery(
            "SELECT * FROM npc_dialogues WHERE npc_id = ?",
            npc.getId()
        );
        
        while (rs.next()) {
            String dialogueId = rs.getString("dialogue_id");
            String linesJson = rs.getString("lines_json");
            String optionsJson = rs.getString("options_json");
            String nextDialogueId = rs.getString("next_dialogue_id");
            
            // Parsear JSON de líneas
            JsonArray linesArray = gson.fromJson(linesJson, JsonArray.class);
            
            NPCDialogue dialogue = new NPCDialogue(dialogueId);
            dialogue.setNextDialogueId(nextDialogueId);
            
            // Agregar líneas
            for (JsonElement elem : linesArray) {
                dialogue.addLine(elem.getAsString());
            }
            
            // Parsear opciones si existen
            if (optionsJson != null && !optionsJson.isEmpty()) {
                JsonArray optionsArray = gson.fromJson(optionsJson, JsonArray.class);
                for (JsonElement optionElem : optionsArray) {
                    JsonObject optionObj = optionElem.getAsJsonObject();
                    String text = optionObj.get("text").getAsString();
                    String targetDialogueId = optionObj.has("targetDialogueId") ? optionObj.get("targetDialogueId").getAsString() : null;
                    String action = optionObj.has("action") ? optionObj.get("action").getAsString() : null;
                    
                    NPCDialogue.DialogueOption option = new NPCDialogue.DialogueOption(text, targetDialogueId, action);
                    dialogue.addOption(option);
                }
            }
            
            npc.addDialogue(dialogue);
        }
        
        rs.close();
    }
    
    /**
     * Carga configuración de comercio de un NPC
     */
    private void loadNPCTrades(CustomNPC npc) throws SQLException {
        ResultSet rs = databaseManager.executeQuery(
            "SELECT * FROM npc_trades WHERE npc_id = ? ORDER BY trade_slot",
            npc.getId()
        );
        
        while (rs.next()) {
            // Por ahora solo cargar - la implementación completa de trades vendrá después
            plugin.getLogger().info("Trade cargado para NPC " + npc.getId());
        }
        
        rs.close();
    }
    
    /**
     * Guarda un NPC en la base de datos
     */
    public void saveNPC(CustomNPC npc) {
        Location loc = npc.getLocation();
        
        databaseManager.executeUpdate(
            "INSERT OR REPLACE INTO npcs (id, name, type, entity_type, world, x, y, z, yaw, pitch, quest_id, initial_dialogue_id, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            npc.getId(),
            npc.getName(),
            npc.getType().name(),
            npc.getEntityType().name(),
            loc.getWorld().getName(),
            loc.getX(),
            loc.getY(),
            loc.getZ(),
            loc.getYaw(),
            loc.getPitch(),
            npc.getQuestGiverId(),
            npc.getInitialDialogueId(),
            System.currentTimeMillis()
        );
        
        // Guardar diálogos
        saveNPCDialogues(npc);
        
        plugin.getLogger().info("NPC guardado: " + npc.getId());
    }
    
    /**
     * Guarda los diálogos de un NPC en la base de datos
     */
    private void saveNPCDialogues(CustomNPC npc) {
        // Primero eliminar diálogos existentes
        databaseManager.executeUpdate("DELETE FROM npc_dialogues WHERE npc_id = ?", npc.getId());
        
        // Insertar nuevos diálogos
        for (NPCDialogue dialogue : npc.getAllDialogues()) {
            // Convertir líneas a JSON
            JsonArray linesArray = new JsonArray();
            for (String line : dialogue.getLines()) {
                linesArray.add(line);
            }
            String linesJson = gson.toJson(linesArray);
            
            // Convertir opciones a JSON
            String optionsJson = null;
            if (!dialogue.getOptions().isEmpty()) {
                JsonArray optionsArray = new JsonArray();
                for (NPCDialogue.DialogueOption option : dialogue.getOptions()) {
                    JsonObject optionObj = new JsonObject();
                    optionObj.addProperty("text", option.getText());
                    if (option.getAction() != null) {
                        optionObj.addProperty("action", option.getAction());
                    }
                    if (option.getTargetDialogueId() != null) {
                        optionObj.addProperty("targetDialogueId", option.getTargetDialogueId());
                    }
                    optionsArray.add(optionObj);
                }
                optionsJson = gson.toJson(optionsArray);
            }
            
            databaseManager.executeUpdate(
                "INSERT INTO npc_dialogues (npc_id, dialogue_id, lines_json, options_json, next_dialogue_id) VALUES (?, ?, ?, ?, ?)",
                npc.getId(),
                dialogue.getId(),
                linesJson,
                optionsJson,
                dialogue.getNextDialogueId()
            );
        }
    }
    
    /**
     * Elimina un NPC de la base de datos
     */
    public void deleteNPC(String npcId) {
        databaseManager.executeUpdate("DELETE FROM npc_trades WHERE npc_id = ?", npcId);
        databaseManager.executeUpdate("DELETE FROM npc_dialogues WHERE npc_id = ?", npcId);
        databaseManager.executeUpdate("DELETE FROM npcs WHERE id = ?", npcId);
        plugin.getLogger().info("NPC eliminado: " + npcId);
    }
    
    /**
     * Crea NPCs por defecto para demostración
     */
    private void createDefaultNPCs() {
        // Nota: Los NPCs se crearán con ubicaciones específicas cuando se implemente
        // el sistema de configuración por mundo
    }
    
    /**
     * Crea un nuevo NPC
     */
    public CustomNPC createNPC(String id, String name, NPCType type, Location location) {
        return createNPC(id, name, type, location, EntityType.VILLAGER);
    }
    
    /**
     * Crea un nuevo NPC con tipo de entidad específico
     */
    public CustomNPC createNPC(String id, String name, NPCType type, Location location, EntityType entityType) {
        CustomNPC npc = new CustomNPC(id, name, type, location, entityType);
        npcs.put(id, npc);
        saveNPC(npc); // Guardar en BD
        return npc;
    }
    
    /**
     * Registra un NPC existente
     */
    public void registerNPC(CustomNPC npc) {
        npcs.put(npc.getId(), npc);
        saveNPC(npc); // Guardar en BD
    }
    
    /**
     * Elimina un NPC del registro
     */
    public void removeNPC(String npcId) {
        npcs.remove(npcId);
        deleteNPC(npcId); // Eliminar de BD
    }
    
    /**
     * Obtiene un NPC por ID
     */
    public CustomNPC getNPC(String id) {
        return npcs.get(id);
    }
    
    /**
     * Obtiene todos los NPCs
     */
    public Collection<CustomNPC> getAllNPCs() {
        return npcs.values();
    }
    
    /**
     * Spawna todos los NPCs
     */
    public void spawnAll() {
        for (CustomNPC npc : npcs.values()) {
            npc.spawn();
        }
    }
    
    /**
     * Despawnea todos los NPCs
     */
    public void despawnAll() {
        for (CustomNPC npc : npcs.values()) {
            npc.despawn();
        }
    }
    
    /**
     * Spawna un NPC específico
     */
    public void spawnNPC(String id) {
        CustomNPC npc = npcs.get(id);
        if (npc != null) {
            npc.spawn();
        }
    }
    
    /**
     * Despawnea un NPC específico
     */
    public void despawnNPC(String id) {
        CustomNPC npc = npcs.get(id);
        if (npc != null) {
            npc.despawn();
        }
    }
    
    /**
     * Maneja la interacción con NPCs
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        // Buscar si la entidad es un NPC
        CustomNPC npc = null;
        for (CustomNPC n : npcs.values()) {
            if (n.getEntity() != null && n.getEntity().equals(event.getRightClicked())) {
                npc = n;
                break;
            }
        }
        
        if (npc == null) {
            return;
        }
        
        event.setCancelled(true);
        startDialogue(player, npc);
    }
    
    /**
     * Inicia un diálogo con un NPC
     */
    public void startDialogue(Player player, CustomNPC npc) {
        NPCDialogue dialogue = npc.getInitialDialogue();
        
        if (dialogue == null) {
            player.sendMessage(npc.getType().getColorCode() + npc.getName() + ": §7¡Hola!");
            return;
        }
        
        showDialogue(player, npc, dialogue);
    }
    
    /**
     * Muestra un diálogo al jugador
     */
    private void showDialogue(Player player, CustomNPC npc, NPCDialogue dialogue) {
        playerDialogueState.put(player.getUniqueId(), dialogue.getId());
        
        player.sendMessage("");
        player.sendMessage("§6§l=== " + npc.getType().getColorCode() + npc.getName() + " §6§l===");
        
        for (String line : dialogue.getLines()) {
            player.sendMessage("§f" + line);
        }
        
        player.sendMessage("");
        
        List<NPCDialogue.DialogueOption> options = dialogue.getOptions();
        if (!options.isEmpty()) {
            player.sendMessage("§eOpciones:");
            for (int i = 0; i < options.size(); i++) {
                NPCDialogue.DialogueOption option = options.get(i);
                player.sendMessage("§a[" + (i + 1) + "] §f" + option.getText());
            }
            player.sendMessage("§7Usa §e/npc respond <número> §7para responder");
        } else if (dialogue.getNextDialogueId() != null) {
            player.sendMessage("§7Presiona click derecho de nuevo para continuar...");
        }
    }
    
    /**
     * Responde a una opción de diálogo
     */
    public void respondToDialogue(Player player, CustomNPC npc, int optionIndex) {
        String currentDialogueId = playerDialogueState.get(player.getUniqueId());
        if (currentDialogueId == null) {
            player.sendMessage("§cNo estás en un diálogo actualmente");
            return;
        }
        
        NPCDialogue currentDialogue = npc.getDialogue(currentDialogueId);
        if (currentDialogue == null) {
            player.sendMessage("§cError en el diálogo");
            playerDialogueState.remove(player.getUniqueId());
            return;
        }
        
        List<NPCDialogue.DialogueOption> options = currentDialogue.getOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            player.sendMessage("§cOpción inválida");
            return;
        }
        
        NPCDialogue.DialogueOption selectedOption = options.get(optionIndex);
        
        // Ejecutar acción si existe
        if (selectedOption.getAction() != null) {
            executeDialogueAction(player, npc, selectedOption.getAction());
        }
        
        // Continuar al siguiente diálogo
        if (selectedOption.getTargetDialogueId() != null) {
            NPCDialogue nextDialogue = npc.getDialogue(selectedOption.getTargetDialogueId());
            if (nextDialogue != null) {
                showDialogue(player, npc, nextDialogue);
            } else {
                playerDialogueState.remove(player.getUniqueId());
            }
        } else {
            player.sendMessage("§7Conversación finalizada");
            playerDialogueState.remove(player.getUniqueId());
        }
    }
    
    /**
     * Ejecuta una acción de diálogo
     */
    private void executeDialogueAction(Player player, CustomNPC npc, String action) {
        switch (action) {
            case "quest_accept":
                player.sendMessage("§a¡Misión aceptada!");
                // Aquí se integrará con QuestManager
                break;
            case "shop_open":
                player.sendMessage("§aTienda abierta");
                // Aquí se implementará el sistema de comercio
                break;
            case "train":
                player.sendMessage("§aEntrenamiento iniciado");
                // Aquí se implementará el sistema de entrenamiento
                break;
        }
    }
}
