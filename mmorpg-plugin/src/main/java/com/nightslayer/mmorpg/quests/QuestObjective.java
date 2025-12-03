package com.nightslayer.mmorpg.quests;

import com.google.gson.JsonObject;

/**
 * Representa un objetivo de una quest
 */
public class QuestObjective {
    private final String id;
    private final QuestObjectiveType type;
    private final String target;
    private final int requiredAmount;
    private final String description;
    private String recipient; // Para objetivos de DELIVER
    
    public QuestObjective(String id, QuestObjectiveType type, String target, int requiredAmount) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.requiredAmount = requiredAmount;
        this.description = generateDescription();
    }
    
    public String getId() {
        return id;
    }
    
    public QuestObjectiveType getType() {
        return type;
    }
    
    public String getTarget() {
        return target;
    }
    
    public int getRequiredAmount() {
        return requiredAmount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    /**
     * Genera la descripción del objetivo basada en el template
     */
    private String generateDescription() {
        String desc = type.getTemplate();
        desc = desc.replace("%amount%", String.valueOf(requiredAmount));
        desc = desc.replace("%target%", target);
        if (recipient != null) {
            desc = desc.replace("%recipient%", recipient);
        }
        return desc;
    }
    
    /**
     * Verifica si un progreso cumple con el objetivo
     */
    public boolean isCompleted(int currentProgress) {
        return currentProgress >= requiredAmount;
    }
    
    /**
     * Serializa a JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("type", type.name());
        json.addProperty("target", target);
        json.addProperty("required", requiredAmount);
        json.addProperty("description", description);
        if (recipient != null) {
            json.addProperty("recipient", recipient);
        }
        return json;
    }
}
