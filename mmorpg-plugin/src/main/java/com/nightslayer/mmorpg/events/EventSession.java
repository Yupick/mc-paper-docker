package com.nightslayer.mmorpg.events;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sesión de un evento activo
 */
public class EventSession {
    private final String eventId;
    private final String eventName;
    private final String worldName;
    private final LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status; // ACTIVE, COMPLETED, CANCELLED
    private int historyId; // ID en la tabla event_history
    
    private final Map<UUID, ParticipantData> participants;
    private int totalKills;
    
    public EventSession(String eventId, String eventName, String worldName) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.worldName = worldName;
        this.startedAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.participants = new ConcurrentHashMap<>();
        this.totalKills = 0;
    }
    
    /**
     * Registra un kill de un jugador
     */
    public void addPlayerKill(UUID playerId, String playerName) {
        participants.computeIfAbsent(playerId, k -> new ParticipantData(playerName))
                   .incrementKills();
        totalKills++;
    }
    
    /**
     * Añade monedas de evento ganadas por un jugador
     */
    public void addEventCoins(UUID playerId, int amount) {
        ParticipantData data = participants.get(playerId);
        if (data != null) {
            data.addEventCoins(amount);
        }
    }
    
    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getWorldName() { return worldName; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public String getStatus() { return status; }
    public int getHistoryId() { return historyId; }
    public Map<UUID, ParticipantData> getParticipants() { return participants; }
    public int getTotalKills() { return totalKills; }
    
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public void setStatus(String status) { this.status = status; }
    public void setHistoryId(int historyId) { this.historyId = historyId; }
    
    /**
     * Datos de un participante del evento
     */
    public static class ParticipantData {
        private final String playerName;
        private int kills;
        private int eventCoinsEarned;
        
        public ParticipantData(String playerName) {
            this.playerName = playerName;
            this.kills = 0;
            this.eventCoinsEarned = 0;
        }
        
        public void incrementKills() {
            kills++;
        }
        
        public void addEventCoins(int amount) {
            eventCoinsEarned += amount;
        }
        
        public String getPlayerName() { return playerName; }
        public int getKills() { return kills; }
        public int getEventCoinsEarned() { return eventCoinsEarned; }
    }
}
