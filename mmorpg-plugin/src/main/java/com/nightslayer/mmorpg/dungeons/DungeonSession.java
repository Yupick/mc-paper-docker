package com.nightslayer.mmorpg.dungeons;

import org.bukkit.Location;
import org.bukkit.World;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa una sesión activa de mazmorra con jugadores y salas generadas
 */
public class DungeonSession {
    private final String sessionId;
    private final String dungeonId;
    private final String dungeonName;
    private final World world;
    private final LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status; // ACTIVE, COMPLETED, ABANDONED, FAILED
    
    private final Set<UUID> players;
    private final Map<UUID, Integer> playerKills;
    private final Map<UUID, Long> playerDamageDealt;
    private final List<GeneratedRoom> rooms;
    private int currentRoomIndex;
    private int totalMobsKilled;
    private boolean bossFinalDefeated;
    
    public DungeonSession(String sessionId, String dungeonId, String dungeonName, World world) {
        this.sessionId = sessionId;
        this.dungeonId = dungeonId;
        this.dungeonName = dungeonName;
        this.world = world;
        this.startedAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.players = ConcurrentHashMap.newKeySet();
        this.playerKills = new ConcurrentHashMap<>();
        this.playerDamageDealt = new ConcurrentHashMap<>();
        this.rooms = new ArrayList<>();
        this.currentRoomIndex = 0;
        this.totalMobsKilled = 0;
        this.bossFinalDefeated = false;
    }
    
    /**
     * Agrega un jugador a la sesión
     */
    public void addPlayer(UUID playerId, String playerName) {
        players.add(playerId);
        playerKills.put(playerId, 0);
        playerDamageDealt.put(playerId, 0L);
    }
    
    /**
     * Registra un kill de jugador
     */
    public void recordKill(UUID playerId) {
        players.stream()
            .filter(id -> id.equals(playerId))
            .forEach(id -> playerKills.merge(id, 1, Integer::sum));
        totalMobsKilled++;
    }
    
    /**
     * Registra daño de jugador
     */
    public void recordDamage(UUID playerId, long damage) {
        playerDamageDealt.merge(playerId, damage, Long::sum);
    }
    
    /**
     * Pasa a la siguiente sala
     */
    public boolean nextRoom() {
        if (currentRoomIndex < rooms.size() - 1) {
            currentRoomIndex++;
            return true;
        }
        return false;
    }
    
    /**
     * Completa la mazmorra
     */
    public void complete() {
        this.completedAt = LocalDateTime.now();
        this.status = "COMPLETED";
    }
    
    /**
     * Abandona la mazmorra
     */
    public void abandon() {
        this.completedAt = LocalDateTime.now();
        this.status = "ABANDONED";
    }
    
    /**
     * Falla la mazmorra
     */
    public void fail() {
        this.completedAt = LocalDateTime.now();
        this.status = "FAILED";
    }
    
    public String getSessionId() { return sessionId; }
    public String getDungeonId() { return dungeonId; }
    public String getDungeonName() { return dungeonName; }
    public World getWorld() { return world; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getStatus() { return status; }
    public Set<UUID> getPlayers() { return players; }
    public Map<UUID, Integer> getPlayerKills() { return playerKills; }
    public Map<UUID, Long> getPlayerDamageDealt() { return playerDamageDealt; }
    public List<GeneratedRoom> getRooms() { return rooms; }
    public int getCurrentRoomIndex() { return currentRoomIndex; }
    public GeneratedRoom getCurrentRoom() { 
        return currentRoomIndex < rooms.size() ? rooms.get(currentRoomIndex) : null;
    }
    public int getTotalMobsKilled() { return totalMobsKilled; }
    public boolean isBossFinalDefeated() { return bossFinalDefeated; }
    public int getPlayerCount() { return players.size(); }
    
    public void setBossFinalDefeated(boolean defeated) { this.bossFinalDefeated = defeated; }
    
    /**
     * Sala generada proceduralmente
     */
    public static class GeneratedRoom {
        private final int index;
        private final String type;
        private final Location origin;
        private final int width;
        private final int height;
        private final List<MobSpawn> mobSpawns;
        private final List<Location> treasureLocations;
        private boolean cleared;
        
        public GeneratedRoom(int index, String type, Location origin, int width, int height) {
            this.index = index;
            this.type = type;
            this.origin = origin;
            this.width = width;
            this.height = height;
            this.mobSpawns = new ArrayList<>();
            this.treasureLocations = new ArrayList<>();
            this.cleared = false;
        }
        
        public void addMobSpawn(String mobId, Location location) {
            mobSpawns.add(new MobSpawn(mobId, location));
        }
        
        public void addTreasureLocation(Location location) {
            treasureLocations.add(location);
        }
        
        public int getIndex() { return index; }
        public String getType() { return type; }
        public Location getOrigin() { return origin; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public List<MobSpawn> getMobSpawns() { return mobSpawns; }
        public List<Location> getTreasureLocations() { return treasureLocations; }
        public boolean isCleared() { return cleared; }
        public void setCleared(boolean cleared) { this.cleared = cleared; }
    }
    
    /**
     * Información de spawn de mob
     */
    public static class MobSpawn {
        private final String mobId;
        private final Location location;
        
        public MobSpawn(String mobId, Location location) {
            this.mobId = mobId;
            this.location = location.clone();
        }
        
        public String getMobId() { return mobId; }
        public Location getLocation() { return location; }
    }
}
