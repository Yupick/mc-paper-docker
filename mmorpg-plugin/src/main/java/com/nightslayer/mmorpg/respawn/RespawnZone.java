package com.nightslayer.mmorpg.respawn;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representa una zona de respawn de mobs
 */
public class RespawnZone {
    private final String zoneId;
    private final String name;
    private final ZoneType type;
    private final String worldName;
    private final List<String> mobIds;
    private final List<Location> spawnLocations;
    private final int maxMobs;
    private final int respawnInterval; // en segundos
    private boolean enabled;
    
    private long lastRespawnTime;
    private int currentMobCount;
    
    public enum ZoneType {
        FARMEO("Zona de Farmeo"),
        DUNGEON("Dungeon"),
        BOSS_ARENA("Arena de Boss");
        
        private final String displayName;
        
        ZoneType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public RespawnZone(String zoneId, String name, ZoneType type, String worldName,
                      List<String> mobIds, List<Location> spawnLocations,
                      int maxMobs, int respawnInterval) {
        this.zoneId = zoneId;
        this.name = name;
        this.type = type;
        this.worldName = worldName;
        this.mobIds = new CopyOnWriteArrayList<>(mobIds);
        this.spawnLocations = new CopyOnWriteArrayList<>(spawnLocations);
        this.maxMobs = maxMobs;
        this.respawnInterval = respawnInterval;
        this.enabled = true;
        this.lastRespawnTime = System.currentTimeMillis();
        this.currentMobCount = 0;
    }
    
    // ======================= Getters =======================
    
    public String getZoneId() { return zoneId; }
    public String getName() { return name; }
    public ZoneType getType() { return type; }
    public String getWorldName() { return worldName; }
    public List<String> getMobIds() { return new CopyOnWriteArrayList<>(mobIds); }
    public List<Location> getSpawnLocations() { return new CopyOnWriteArrayList<>(spawnLocations); }
    public int getMaxMobs() { return maxMobs; }
    public int getRespawnInterval() { return respawnInterval; }
    public boolean isEnabled() { return enabled; }
    public int getCurrentMobCount() { return currentMobCount; }
    public long getLastRespawnTime() { return lastRespawnTime; }
    
    // ======================= Setters =======================
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setCurrentMobCount(int count) {
        this.currentMobCount = count;
    }
    
    public void setLastRespawnTime(long time) {
        this.lastRespawnTime = time;
    }
    
    // ======================= Métodos =======================
    
    /**
     * Verifica si es tiempo de respawnear
     */
    public boolean shouldRespawn() {
        if (!enabled) return false;
        if (currentMobCount >= maxMobs) return false;
        
        long elapsed = (System.currentTimeMillis() - lastRespawnTime) / 1000;
        return elapsed >= respawnInterval;
    }
    
    /**
     * Obtiene una ubicación aleatoria de spawn
     */
    public Location getRandomSpawnLocation(World world) {
        if (spawnLocations.isEmpty()) return null;
        
        Location baseLoc = spawnLocations.get((int) (Math.random() * spawnLocations.size()));
        // Agregar variación aleatoria (±5 bloques)
        double offsetX = (Math.random() - 0.5) * 10;
        double offsetZ = (Math.random() - 0.5) * 10;
        
        return new Location(world, 
            baseLoc.getX() + offsetX, 
            baseLoc.getY(), 
            baseLoc.getZ() + offsetZ);
    }
    
    /**
     * Obtiene un mob ID aleatorio de la zona
     */
    public String getRandomMobId() {
        if (mobIds.isEmpty()) return null;
        return mobIds.get((int) (Math.random() * mobIds.size()));
    }
    
    /**
     * Calcula cuántos mobs faltan para respawnear
     */
    public int getMobsToSpawn() {
        return Math.max(0, maxMobs - currentMobCount);
    }
    
    /**
     * Incrementa contador de mobs
     */
    public void incrementMobCount() {
        currentMobCount++;
    }
    
    /**
     * Decrementa contador de mobs
     */
    public void decrementMobCount() {
        if (currentMobCount > 0) {
            currentMobCount--;
        }
    }
    
    /**
     * Obtiene información de la zona como string
     */
    @Override
    public String toString() {
        return String.format(
            "RespawnZone{id=%s, name=%s, type=%s, mobs=%d/%d, interval=%ds}",
            zoneId, name, type.name(), currentMobCount, maxMobs, respawnInterval
        );
    }
}
