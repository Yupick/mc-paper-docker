package com.nightslayer.mmorpg.quests;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representa el progreso de un jugador en una quest
 */
public class PlayerQuestProgress {
    private final UUID playerUUID;
    private final String questId;
    private final Map<String, Integer> objectiveProgress; // ID del objetivo -> progreso actual
    private QuestStatus status;
    private long startTime;
    private long completionTime;
    private long nextAvailableTime; // Para quests repetibles
    
    public enum QuestStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        CLAIMED,
        ON_COOLDOWN
    }
    
    public PlayerQuestProgress(UUID playerUUID, String questId) {
        this.playerUUID = playerUUID;
        this.questId = questId;
        this.objectiveProgress = new HashMap<>();
        this.status = QuestStatus.NOT_STARTED;
        this.startTime = 0;
        this.completionTime = 0;
        this.nextAvailableTime = 0;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getQuestId() {
        return questId;
    }
    
    public QuestStatus getStatus() {
        return status;
    }
    
    public void setStatus(QuestStatus status) {
        this.status = status;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getCompletionTime() {
        return completionTime;
    }
    
    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }
    
    public long getNextAvailableTime() {
        return nextAvailableTime;
    }
    
    public void setNextAvailableTime(long nextAvailableTime) {
        this.nextAvailableTime = nextAvailableTime;
    }
    
    /**
     * Obtiene el progreso de un objetivo
     */
    public int getObjectiveProgress(String objectiveId) {
        return objectiveProgress.getOrDefault(objectiveId, 0);
    }
    
    /**
     * Establece el progreso de un objetivo
     */
    public void setObjectiveProgress(String objectiveId, int progress) {
        objectiveProgress.put(objectiveId, progress);
    }
    
    /**
     * Incrementa el progreso de un objetivo
     */
    public void incrementObjectiveProgress(String objectiveId, int amount) {
        int current = getObjectiveProgress(objectiveId);
        setObjectiveProgress(objectiveId, current + amount);
    }
    
    /**
     * Verifica si un objetivo está completado
     */
    public boolean isObjectiveCompleted(QuestObjective objective) {
        int progress = getObjectiveProgress(objective.getId());
        return objective.isCompleted(progress);
    }
    
    /**
     * Verifica si todos los objetivos están completados
     */
    public boolean areAllObjectivesCompleted(Quest quest) {
        for (QuestObjective objective : quest.getObjectives()) {
            if (!isObjectiveCompleted(objective)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Inicia la quest
     */
    public void start() {
        this.status = QuestStatus.IN_PROGRESS;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Completa la quest
     */
    public void complete() {
        this.status = QuestStatus.COMPLETED;
        this.completionTime = System.currentTimeMillis();
    }
    
    /**
     * Marca la quest como reclamada
     */
    public void claim(Quest quest) {
        this.status = QuestStatus.CLAIMED;
        
        if (quest.isRepeatable()) {
            this.nextAvailableTime = System.currentTimeMillis() + quest.getCooldownTime();
            this.status = QuestStatus.ON_COOLDOWN;
        }
    }
    
    /**
     * Resetea la quest para repetirla
     */
    public void reset() {
        this.objectiveProgress.clear();
        this.status = QuestStatus.NOT_STARTED;
        this.startTime = 0;
        this.completionTime = 0;
    }
    
    /**
     * Verifica si la quest está disponible (para repetibles)
     */
    public boolean isAvailable() {
        if (status == QuestStatus.ON_COOLDOWN) {
            return System.currentTimeMillis() >= nextAvailableTime;
        }
        return status == QuestStatus.NOT_STARTED;
    }
    
    /**
     * Obtiene el tiempo restante de cooldown en segundos
     */
    public long getRemainingCooldown() {
        if (status != QuestStatus.ON_COOLDOWN) {
            return 0;
        }
        long remaining = nextAvailableTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Serializa a JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("player", playerUUID.toString());
        json.addProperty("quest", questId);
        json.addProperty("status", status.name());
        json.addProperty("startTime", startTime);
        json.addProperty("completionTime", completionTime);
        json.addProperty("nextAvailable", nextAvailableTime);
        
        // Progreso de objetivos
        JsonObject progressJson = new JsonObject();
        for (Map.Entry<String, Integer> entry : objectiveProgress.entrySet()) {
            progressJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("progress", progressJson);
        
        return json;
    }
    
    /**
     * Carga desde JSON
     */
    public static PlayerQuestProgress fromJson(JsonObject json) {
        UUID playerUUID = UUID.fromString(json.get("player").getAsString());
        String questId = json.get("quest").getAsString();
        
        PlayerQuestProgress progress = new PlayerQuestProgress(playerUUID, questId);
        progress.status = QuestStatus.valueOf(json.get("status").getAsString());
        progress.startTime = json.get("startTime").getAsLong();
        progress.completionTime = json.get("completionTime").getAsLong();
        progress.nextAvailableTime = json.get("nextAvailable").getAsLong();
        
        // Cargar progreso de objetivos
        if (json.has("progress")) {
            JsonObject progressJson = json.getAsJsonObject("progress");
            for (String key : progressJson.keySet()) {
                progress.objectiveProgress.put(key, progressJson.get(key).getAsInt());
            }
        }
        
        return progress;
    }
}
