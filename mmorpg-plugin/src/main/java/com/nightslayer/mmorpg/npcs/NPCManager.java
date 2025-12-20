package com.nightslayer.mmorpg.npcs;

import com.nightslayer.mmorpg.RPGPathResolver;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Gestiona todos los NPCs del sistema RPG
 * Lee desde: worlds/active/data/npcs.json (mundo activo)
 * Archivos universales en: plugins/MMORPGPlugin/data/npcs.json
 */
public class NPCManager implements Listener {
    private final MMORPGPlugin plugin;
    private final RPGPathResolver pathResolver;
    private final Map<String, CustomNPC> npcs;
    private final Map<UUID, String> playerDialogueState; // UUID del jugador -> ID del diálogo actual
    private final Gson gson;
    
    public NPCManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.pathResolver = plugin.getWorldRPGManager().getPathResolver();
        this.npcs = new HashMap<>();
        this.playerDialogueState = new HashMap<>();
        this.gson = new Gson();
        
        createDefaultNPCs();
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
        return npc;
    }
    
    /**
     * Registra un NPC existente
     */
    public void registerNPC(CustomNPC npc) {
        npcs.put(npc.getId(), npc);
    }
    
    /**
     * Elimina un NPC del registro
     */
    public void removeNPC(String npcId) {
        npcs.remove(npcId);
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
    
    /**
     * Guarda todos los NPCs de un mundo específico
     * Guarda en: worlds/{worldName}/data/npcs.json
     */
    public void saveAll(String worldName) {
        File file = pathResolver.getLocalFile(worldName, "npcs.json");
        
        try (FileWriter writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            
            for (CustomNPC npc : npcs.values()) {
                // Solo guardar NPCs de este mundo
                if (npc.getLocation() != null && 
                    npc.getLocation().getWorld() != null &&
                    npc.getLocation().getWorld().getName().equals(worldName)) {
                    json.add(npc.getId(), npc.toJson());
                }
            }
            
            gson.toJson(json, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al guardar NPCs para mundo " + worldName + ": " + e.getMessage());
        }
    }
    
    /**
     * Carga NPCs de un mundo específico
     * Lee desde: worlds/{worldName}/data/npcs.json
     */
    public void loadWorld(String worldName) {
        File file = pathResolver.getLocalFile(worldName, "npcs.json");
        
        if (!file.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (String key : json.keySet()) {
                    // Implementar deserialización de NPCs desde JSON
                    plugin.getLogger().info("Cargado NPC: " + key + " del mundo " + worldName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al cargar NPCs para mundo " + worldName + ": " + e.getMessage());
        }
    }
}
