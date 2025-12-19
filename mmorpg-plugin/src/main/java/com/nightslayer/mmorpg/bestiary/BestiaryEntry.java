package com.nightslayer.mmorpg.bestiary;

import java.time.LocalDateTime;

/**
 * Representa una entrada individual en el bestiario de un jugador
 */
public class BestiaryEntry {
    private final String mobId;
    private int kills;
    private LocalDateTime firstKillDate;
    private LocalDateTime lastKillDate;
    private int currentTier;
    private boolean discovered;

    public BestiaryEntry(String mobId) {
        this.mobId = mobId;
        this.kills = 0;
        this.currentTier = 0;
        this.discovered = false;
    }

    public BestiaryEntry(String mobId, int kills, LocalDateTime firstKillDate, 
                        LocalDateTime lastKillDate, int currentTier, boolean discovered) {
        this.mobId = mobId;
        this.kills = kills;
        this.firstKillDate = firstKillDate;
        this.lastKillDate = lastKillDate;
        this.currentTier = currentTier;
        this.discovered = discovered;
    }

    /**
     * Registra una nueva kill del mob
     */
    public void recordKill() {
        kills++;
        LocalDateTime now = LocalDateTime.now();
        
        if (!discovered) {
            discovered = true;
            firstKillDate = now;
        }
        
        lastKillDate = now;
    }

    /**
     * Calcula el progreso porcentual del tier actual
     * @param thresholds Array de kills requeridos por tier
     * @return Porcentaje de 0-100
     */
    public int getProgressPercentage(int[] thresholds) {
        if (currentTier >= thresholds.length - 1) {
            return 100; // Tier máximo alcanzado
        }
        
        int currentThreshold = thresholds[currentTier];
        int nextThreshold = thresholds[currentTier + 1];
        
        if (kills >= nextThreshold) {
            return 100;
        }
        
        int killsInTier = kills - currentThreshold;
        int killsNeeded = nextThreshold - currentThreshold;
        
        return (int) ((killsInTier / (double) killsNeeded) * 100);
    }

    /**
     * Actualiza el tier basado en los kills actuales
     * @param thresholds Array de kills requeridos por tier
     * @return true si subió de tier
     */
    public boolean updateTier(int[] thresholds) {
        int newTier = 0;
        
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (kills >= thresholds[i]) {
                newTier = i;
                break;
            }
        }
        
        if (newTier > currentTier) {
            currentTier = newTier;
            return true;
        }
        
        return false;
    }

    // Getters
    public String getMobId() {
        return mobId;
    }

    public int getKills() {
        return kills;
    }

    public LocalDateTime getFirstKillDate() {
        return firstKillDate;
    }

    public LocalDateTime getLastKillDate() {
        return lastKillDate;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    // Setters (para cargar desde BD)
    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setFirstKillDate(LocalDateTime firstKillDate) {
        this.firstKillDate = firstKillDate;
    }

    public void setLastKillDate(LocalDateTime lastKillDate) {
        this.lastKillDate = lastKillDate;
    }

    public void setCurrentTier(int currentTier) {
        this.currentTier = currentTier;
    }

    public void setDiscovered(boolean discovered) {
        this.discovered = discovered;
    }
}
