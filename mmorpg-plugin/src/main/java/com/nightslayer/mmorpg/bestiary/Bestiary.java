package com.nightslayer.mmorpg.bestiary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bestiario de un jugador individual
 */
public class Bestiary {
    private final UUID playerUUID;
    private final Map<String, BestiaryEntry> entries;
    private int totalDiscoveries;
    private int totalKills;

    public Bestiary(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.entries = new HashMap<>();
        this.totalDiscoveries = 0;
        this.totalKills = 0;
    }

    /**
     * Registra una kill de mob
     * @param mobId ID del mob matado
     * @param thresholds Array de thresholds para calcular tier
     * @return true si es un nuevo descubrimiento
     */
    public boolean recordKill(String mobId, int[] thresholds) {
        BestiaryEntry entry = entries.computeIfAbsent(mobId, BestiaryEntry::new);
        
        boolean wasDiscovered = entry.isDiscovered();
        entry.recordKill();
        
        boolean tierUp = entry.updateTier(thresholds);
        
        if (!wasDiscovered) {
            totalDiscoveries++;
        }
        
        totalKills++;
        
        return !wasDiscovered; // true si es nuevo descubrimiento
    }

    /**
     * Obtiene la entrada de un mob específico
     */
    public BestiaryEntry getEntry(String mobId) {
        return entries.get(mobId);
    }

    /**
     * Verifica si un mob ha sido descubierto
     */
    public boolean isDiscovered(String mobId) {
        BestiaryEntry entry = entries.get(mobId);
        return entry != null && entry.isDiscovered();
    }

    /**
     * Obtiene lista de todos los mobs descubiertos
     */
    public List<String> getDiscoveredMobs() {
        return entries.values().stream()
            .filter(BestiaryEntry::isDiscovered)
            .map(BestiaryEntry::getMobId)
            .collect(Collectors.toList());
    }

    /**
     * Calcula el progreso total del bestiario
     * @param totalMobsAvailable Total de mobs disponibles en el servidor
     */
    public int getTotalProgress(int totalMobsAvailable) {
        if (totalMobsAvailable == 0) {
            return 0;
        }
        return (int) ((totalDiscoveries / (double) totalMobsAvailable) * 100);
    }

    /**
     * Obtiene kills totales de un mob específico
     */
    public int getKillsForMob(String mobId) {
        BestiaryEntry entry = entries.get(mobId);
        return entry != null ? entry.getKills() : 0;
    }

    // Getters
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Map<String, BestiaryEntry> getEntries() {
        return new HashMap<>(entries);
    }

    public int getTotalDiscoveries() {
        return totalDiscoveries;
    }

    public int getTotalKills() {
        return totalKills;
    }

    // Para cargar desde BD
    public void addEntry(BestiaryEntry entry) {
        entries.put(entry.getMobId(), entry);
        if (entry.isDiscovered()) {
            totalDiscoveries++;
        }
        totalKills += entry.getKills();
    }

    public void recalculateTotals() {
        totalDiscoveries = (int) entries.values().stream()
            .filter(BestiaryEntry::isDiscovered)
            .count();
        
        totalKills = entries.values().stream()
            .mapToInt(BestiaryEntry::getKills)
            .sum();
    }
}
