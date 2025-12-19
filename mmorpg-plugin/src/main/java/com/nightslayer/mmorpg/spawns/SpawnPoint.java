package com.nightslayer.mmorpg.spawns;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Representa un punto de spawn individual con configuración de respawn
 */
public class SpawnPoint {
    private final String id;
    private final String type; // "item", "mob", "npc"
    private final Location location;
    private final boolean respawnEnabled;
    private final int respawnTimeSeconds;
    private final boolean respawnOnDeath;
    private final boolean respawnOnUse;
    
    private String itemType; // Para type="item"
    private String entityType; // Para type="mob" o "npc"
    private boolean spawned;
    private UUID entityUUID;
    private long lastDespawnTime;
    
    public SpawnPoint(String id, String type, Location location, 
                     boolean respawnEnabled, int respawnTimeSeconds,
                     boolean respawnOnDeath, boolean respawnOnUse) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.respawnEnabled = respawnEnabled;
        this.respawnTimeSeconds = respawnTimeSeconds;
        this.respawnOnDeath = respawnOnDeath;
        this.respawnOnUse = respawnOnUse;
        this.spawned = false;
        this.lastDespawnTime = 0;
    }
    
    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public Location getLocation() { return location; }
    public boolean isRespawnEnabled() { return respawnEnabled; }
    public int getRespawnTimeSeconds() { return respawnTimeSeconds; }
    public boolean isRespawnOnDeath() { return respawnOnDeath; }
    public boolean isRespawnOnUse() { return respawnOnUse; }
    public String getItemType() { return itemType; }
    public String getEntityType() { return entityType; }
    public boolean isSpawned() { return spawned; }
    public UUID getEntityUUID() { return entityUUID; }
    public long getLastDespawnTime() { return lastDespawnTime; }
    
    // Setters
    public void setItemType(String itemType) { this.itemType = itemType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public void setSpawned(boolean spawned) { this.spawned = spawned; }
    public void setEntityUUID(UUID entityUUID) { this.entityUUID = entityUUID; }
    public void setLastDespawnTime(long lastDespawnTime) { this.lastDespawnTime = lastDespawnTime; }
}
