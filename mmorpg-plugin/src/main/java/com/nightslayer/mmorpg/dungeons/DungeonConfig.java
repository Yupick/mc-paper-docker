package com.nightslayer.mmorpg.dungeons;

import java.util.*;

/**
 * Configuración de una mazmorra dinámica
 */
public class DungeonConfig {
    private final String id;
    private final String name;
    private final String description;
    private final int minLevel;
    private final int maxLevel;
    private final int minPlayers;
    private final int maxPlayers;
    private final int estimatedDuration; // minutos
    private final List<DungeonRoom> roomTemplates;
    private final DungeonRewards rewards;
    private final boolean enabled;
    
    public DungeonConfig(String id, String name, String description,
                        int minLevel, int maxLevel, int minPlayers, int maxPlayers,
                        int estimatedDuration, List<DungeonRoom> roomTemplates,
                        DungeonRewards rewards, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.estimatedDuration = estimatedDuration;
        this.roomTemplates = roomTemplates;
        this.rewards = rewards;
        this.enabled = enabled;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getEstimatedDuration() { return estimatedDuration; }
    public List<DungeonRoom> getRoomTemplates() { return roomTemplates; }
    public DungeonRewards getRewards() { return rewards; }
    public boolean isEnabled() { return enabled; }
    
    /**
     * Plantilla de sala de mazmorra
     */
    public static class DungeonRoom {
        private final String id;
        private final String type; // CHAMBER, CORRIDOR, BOSS_ARENA, TREASURE
        private final int width;
        private final int height;
        private final List<String> possibleMobs;
        private final int mobCount;
        private final boolean hasTreasure;
        private final boolean hasBoss;
        
        public DungeonRoom(String id, String type, int width, int height,
                          List<String> possibleMobs, int mobCount, 
                          boolean hasTreasure, boolean hasBoss) {
            this.id = id;
            this.type = type;
            this.width = width;
            this.height = height;
            this.possibleMobs = possibleMobs;
            this.mobCount = mobCount;
            this.hasTreasure = hasTreasure;
            this.hasBoss = hasBoss;
        }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public List<String> getPossibleMobs() { return possibleMobs; }
        public int getMobCount() { return mobCount; }
        public boolean hasTreasure() { return hasTreasure; }
        public boolean hasBoss() { return hasBoss; }
    }
    
    /**
     * Recompensas de mazmorra
     */
    public static class DungeonRewards {
        private final int baseXp;
        private final int baseCoin;
        private final List<String> treasureItems;
        private final double treasureDropChance;
        private final int bossXpBonus;
        private final int bossCoinBonus;
        
        public DungeonRewards(int baseXp, int baseCoin, List<String> treasureItems,
                             double treasureDropChance, int bossXpBonus, int bossCoinBonus) {
            this.baseXp = baseXp;
            this.baseCoin = baseCoin;
            this.treasureItems = treasureItems;
            this.treasureDropChance = treasureDropChance;
            this.bossXpBonus = bossXpBonus;
            this.bossCoinBonus = bossCoinBonus;
        }
        
        public int getBaseXp() { return baseXp; }
        public int getBaseCoin() { return baseCoin; }
        public List<String> getTreasureItems() { return treasureItems; }
        public double getTreasureDropChance() { return treasureDropChance; }
        public int getBossXpBonus() { return bossXpBonus; }
        public int getBossCoinBonus() { return bossCoinBonus; }
    }
}
