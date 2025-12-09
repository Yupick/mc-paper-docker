package com.nightslayer.mmorpg.bestiary;

import java.util.ArrayList;
import java.util.List;

/**
 * Categoría de mobs en el bestiario (Undead, Beasts, Bosses, etc.)
 */
public class BestiaryCategory {
    private final String id;
    private final String name;
    private final String description;
    private final List<String> mobIds;
    private final BestiaryReward completionReward;

    public BestiaryCategory(String id, String name, String description, 
                           List<String> mobIds, BestiaryReward completionReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.mobIds = new ArrayList<>(mobIds);
        this.completionReward = completionReward;
    }

    /**
     * Verifica si un mob pertenece a esta categoría
     */
    public boolean containsMob(String mobId) {
        return mobIds.contains(mobId);
    }

    /**
     * Calcula el progreso de completado de la categoría
     * @param discoveredMobs Lista de mobs descubiertos
     * @return Porcentaje de 0-100
     */
    public int getCompletionPercentage(List<String> discoveredMobs) {
        if (mobIds.isEmpty()) {
            return 0;
        }
        
        long discovered = mobIds.stream()
            .filter(discoveredMobs::contains)
            .count();
        
        return (int) ((discovered / (double) mobIds.size()) * 100);
    }

    /**
     * Verifica si la categoría está completamente descubierta
     */
    public boolean isCompleted(List<String> discoveredMobs) {
        return mobIds.stream().allMatch(discoveredMobs::contains);
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getMobIds() {
        return new ArrayList<>(mobIds);
    }

    public BestiaryReward getCompletionReward() {
        return completionReward;
    }

    public int getTotalMobs() {
        return mobIds.size();
    }
}
