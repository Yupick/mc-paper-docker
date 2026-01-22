package com.nightslayer.mmorpg.npcs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;

import java.util.*;

/**
 * Representa un NPC personalizado en el mundo RPG
 */
public class CustomNPC {
    private final String id;
    private final String name;
    private final NPCType type;
    private final Location location;
    private final EntityType entityType;
    private final Map<String, NPCDialogue> dialogues;
    private String initialDialogueId;
    private LivingEntity entity;
    private final List<String> associatedQuests;
    
    public CustomNPC(String id, String name, NPCType type, Location location) {
        this(id, name, type, location, EntityType.VILLAGER);
    }
    
    public CustomNPC(String id, String name, NPCType type, Location location, EntityType entityType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.entityType = entityType;
        this.dialogues = new HashMap<>();
        this.associatedQuests = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public NPCType getType() {
        return type;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public LivingEntity getEntity() {
        return entity;
    }
    
    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }
    
    public void addDialogue(NPCDialogue dialogue) {
        dialogues.put(dialogue.getId(), dialogue);
        
        if (initialDialogueId == null) {
            initialDialogueId = dialogue.getId();
        }
    }
    
    public NPCDialogue getDialogue(String dialogueId) {
        return dialogues.get(dialogueId);
    }
    
    public NPCDialogue getInitialDialogue() {
        return dialogues.get(initialDialogueId);
    }
    
    public void setInitialDialogueId(String dialogueId) {
        this.initialDialogueId = dialogueId;
    }
    
    public String getInitialDialogueId() {
        return initialDialogueId;
    }
    
    public Collection<NPCDialogue> getAllDialogues() {
        return dialogues.values();
    }
    
    public void setQuestGiverId(String questId) {
        setQuestId(questId);
    }
    
    public String getQuestGiverId() {
        return associatedQuests.isEmpty() ? null : associatedQuests.get(0);
    }
    
    public List<String> getAssociatedQuests() {
        return associatedQuests;
    }
    
    public void addQuest(String questId) {
        associatedQuests.add(questId);
    }
    
    /**
     * Establece la quest asociada (limpia y añade una)
     */
    public void setQuestId(String questId) {
        associatedQuests.clear();
        if (questId != null && !questId.isEmpty()) {
            associatedQuests.add(questId);
        }
    }
    
    /**
     * Actualiza la ubicación del NPC
     */
    public void setLocation(Location newLocation) {
        // Nota: esto solo actualiza el campo, se debe llamar despawn()/spawn() para aplicar
        this.location.setWorld(newLocation.getWorld());
        this.location.setX(newLocation.getX());
        this.location.setY(newLocation.getY());
        this.location.setZ(newLocation.getZ());
        this.location.setYaw(newLocation.getYaw());
        this.location.setPitch(newLocation.getPitch());
    }
    
    /**
     * Spawna el NPC en el mundo
     */
    public void spawn() {
        if (entity != null && !entity.isDead()) {
            return; // Ya está spawneado
        }
        
        entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);
        entity.setCustomName(type.getColorCode() + name);
        entity.setCustomNameVisible(true);
        entity.setAI(false);
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.setPersistent(true);
        
        // Configuración específica por tipo de entidad
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            villager.setProfession(getVillagerProfession());
        }
    }
    
    /**
     * Despawnea el NPC
     */
    public void despawn() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        entity = null;
    }
    
    /**
     * Verifica si el NPC está spawneado
     */
    public boolean isSpawned() {
        return entity != null && !entity.isDead();
    }
    
    /**
     * Obtiene la profesión de aldeano según el tipo de NPC
     */
    private Villager.Profession getVillagerProfession() {
        switch (type) {
            case MERCHANT:
                return Villager.Profession.FARMER;
            case QUEST_GIVER:
                return Villager.Profession.LIBRARIAN;
            case TRAINER:
                return Villager.Profession.WEAPONSMITH;
            case GUARD:
                return Villager.Profession.ARMORER;
            default:
                return Villager.Profession.NITWIT;
        }
    }
    
    /**
     * Serializa el NPC a JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("type", type.name());
        json.addProperty("entityType", entityType.name());
        
        // Ubicación
        JsonObject locJson = new JsonObject();
        locJson.addProperty("world", location.getWorld().getName());
        locJson.addProperty("x", location.getX());
        locJson.addProperty("y", location.getY());
        locJson.addProperty("z", location.getZ());
        locJson.addProperty("yaw", location.getYaw());
        locJson.addProperty("pitch", location.getPitch());
        json.add("location", locJson);
        
        // Quests asociadas
        JsonArray questsJson = new JsonArray();
        for (String questId : associatedQuests) {
            questsJson.add(questId);
        }
        json.add("quests", questsJson);
        
        json.addProperty("spawned", isSpawned());
        
        return json;
    }
}
