package com.nightslayer.mmorpg.invasions;

import java.util.Date;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an active or completed invasion session
 */
public class InvasionSession {
    private String sessionId;
    private String invasionId;
    private String worldName;
    private Date startTime;
    private Date endTime;
    private int currentWave;
    private int totalWaves;
    private String status; // ACTIVE, COMPLETED, FAILED, CANCELLED
    private Map<UUID, Integer> playerKills;
    private int totalMobsKilled;
    private int totalMobsSpawned;
    private boolean success;

    public InvasionSession(String invasionId, String worldName, int totalWaves) {
        this.sessionId = UUID.randomUUID().toString();
        this.invasionId = invasionId;
        this.worldName = worldName;
        this.startTime = new Date();
        this.currentWave = 0;
        this.totalWaves = totalWaves;
        this.status = "ACTIVE";
        this.playerKills = new HashMap<>();
        this.totalMobsKilled = 0;
        this.totalMobsSpawned = 0;
        this.success = false;
    }

    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getInvasionId() { return invasionId; }
    public void setInvasionId(String invasionId) { this.invasionId = invasionId; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public int getCurrentWave() { return currentWave; }
    public void setCurrentWave(int currentWave) { this.currentWave = currentWave; }

    public int getTotalWaves() { return totalWaves; }
    public void setTotalWaves(int totalWaves) { this.totalWaves = totalWaves; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<UUID, Integer> getPlayerKills() { return playerKills; }
    public void setPlayerKills(Map<UUID, Integer> playerKills) { this.playerKills = playerKills; }

    public int getTotalMobsKilled() { return totalMobsKilled; }
    public void setTotalMobsKilled(int totalMobsKilled) { this.totalMobsKilled = totalMobsKilled; }

    public int getTotalMobsSpawned() { return totalMobsSpawned; }
    public void setTotalMobsSpawned(int totalMobsSpawned) { this.totalMobsSpawned = totalMobsSpawned; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    /**
     * Add kill for a player
     */
    public void addPlayerKill(UUID playerUuid) {
        playerKills.put(playerUuid, playerKills.getOrDefault(playerUuid, 0) + 1);
        totalMobsKilled++;
    }

    /**
     * Start next wave
     */
    public void nextWave() {
        this.currentWave++;
    }

    /**
     * Complete the invasion
     */
    public void complete(boolean success) {
        this.endTime = new Date();
        this.status = success ? "COMPLETED" : "FAILED";
        this.success = success;
    }

    /**
     * Cancel the invasion
     */
    public void cancel() {
        this.endTime = new Date();
        this.status = "CANCELLED";
        this.success = false;
    }

    /**
     * Get invasion progress percentage
     */
    public double getProgress() {
        if (totalMobsSpawned == 0) return 0.0;
        return (double) totalMobsKilled / totalMobsSpawned * 100.0;
    }

    /**
     * Get invasion duration in seconds
     */
    public long getDurationSeconds() {
        Date end = endTime != null ? endTime : new Date();
        return (end.getTime() - startTime.getTime()) / 1000;
    }
}
