package com.nightslayer.mmorpg.events;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configuración de un evento temático
 */
public class EventConfig {
    private final String id;
    private final String name;
    private final String description;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final boolean enabled;
    private final boolean autoStart;
    private final List<EventMob> customMobs;
    private final List<EventDrop> exclusiveDrops;
    private final EventRewards rewards;
    
    public EventConfig(String id, String name, String description, 
                      LocalDateTime startDate, LocalDateTime endDate,
                      boolean enabled, boolean autoStart,
                      List<EventMob> customMobs, List<EventDrop> exclusiveDrops,
                      EventRewards rewards) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.enabled = enabled;
        this.autoStart = autoStart;
        this.customMobs = customMobs;
        this.exclusiveDrops = exclusiveDrops;
        this.rewards = rewards;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public boolean isEnabled() { return enabled; }
    public boolean isAutoStart() { return autoStart; }
    public List<EventMob> getCustomMobs() { return customMobs; }
    public List<EventDrop> getExclusiveDrops() { return exclusiveDrops; }
    public EventRewards getRewards() { return rewards; }
    
    /**
     * Verifica si el evento está activo en este momento
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return enabled && now.isAfter(startDate) && now.isBefore(endDate);
    }
    
    /**
     * Mob específico de un evento
     */
    public static class EventMob {
        private final String mobId;
        private final double spawnChance;
        private final int spawnRadius;
        
        public EventMob(String mobId, double spawnChance, int spawnRadius) {
            this.mobId = mobId;
            this.spawnChance = spawnChance;
            this.spawnRadius = spawnRadius;
        }
        
        public String getMobId() { return mobId; }
        public double getSpawnChance() { return spawnChance; }
        public int getSpawnRadius() { return spawnRadius; }
    }
    
    /**
     * Drop exclusivo de un evento
     */
    public static class EventDrop {
        private final String itemName;
        private final double dropChance;
        private final int minAmount;
        private final int maxAmount;
        
        public EventDrop(String itemName, double dropChance, int minAmount, int maxAmount) {
            this.itemName = itemName;
            this.dropChance = dropChance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
        
        public String getItemName() { return itemName; }
        public double getDropChance() { return dropChance; }
        public int getMinAmount() { return minAmount; }
        public int getMaxAmount() { return maxAmount; }
    }
    
    /**
     * Recompensas de un evento
     */
    public static class EventRewards {
        private final int eventCoinsPerKill;
        private final double bonusXPMultiplier;
        private final int bossEventCoins;
        
        public EventRewards(int eventCoinsPerKill, double bonusXPMultiplier, int bossEventCoins) {
            this.eventCoinsPerKill = eventCoinsPerKill;
            this.bonusXPMultiplier = bonusXPMultiplier;
            this.bossEventCoins = bossEventCoins;
        }
        
        public int getEventCoinsPerKill() { return eventCoinsPerKill; }
        public double getBonusXPMultiplier() { return bonusXPMultiplier; }
        public int getBossEventCoins() { return bossEventCoins; }
    }
}
