package com.nightslayer.mmorpg.mobs;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un mob personalizado con stats y drops customizados
 */
public class CustomMob {
    private final String id;
    private final String name;
    private final EntityType entityType;
    private final double health;
    private final double damage;
    private final double defense;
    private final int level;
    private final List<MobDrop> drops;
    private final int experienceReward;
    private final boolean isBoss;
    private final Location spawnLocation;
    
    public CustomMob(String id, String name, EntityType entityType, double health, 
                     double damage, double defense, int level, List<MobDrop> drops,
                     int experienceReward, boolean isBoss, Location spawnLocation) {
        this.id = id;
        this.name = name;
        this.entityType = entityType;
        this.health = health;
        this.damage = damage;
        this.defense = defense;
        this.level = level;
        this.drops = drops != null ? drops : new ArrayList<>();
        this.experienceReward = experienceReward;
        this.isBoss = isBoss;
        this.spawnLocation = spawnLocation;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public EntityType getEntityType() { return entityType; }
    public double getHealth() { return health; }
    public double getDamage() { return damage; }
    public double getDefense() { return defense; }
    public int getLevel() { return level; }
    public List<MobDrop> getDrops() { return drops; }
    public int getExperienceReward() { return experienceReward; }
    public boolean isBoss() { return isBoss; }
    public Location getSpawnLocation() { return spawnLocation; }
    
    /**
     * Clase interna para representar un drop de mob
     */
    public static class MobDrop {
        private final String itemType;
        private final int minAmount;
        private final int maxAmount;
        private final double dropChance;
        
        public MobDrop(String itemType, int minAmount, int maxAmount, double dropChance) {
            this.itemType = itemType;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.dropChance = dropChance;
        }
        
        public String getItemType() { return itemType; }
        public int getMinAmount() { return minAmount; }
        public int getMaxAmount() { return maxAmount; }
        public double getDropChance() { return dropChance; }
    }
}
