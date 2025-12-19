package com.nightslayer.mmorpg.classes;

/**
 * Estadísticas base de una clase RPG
 */
public class ClassStats {
    private final int baseHealth;
    private final int baseMana;
    private final int baseStamina;
    private final int baseMagicPower;
    private final int baseAttackSpeed;
    private final int baseDefense;
    
    public ClassStats(int health, int mana, int stamina, int magicPower, int attackSpeed, int defense) {
        this.baseHealth = health;
        this.baseMana = mana;
        this.baseStamina = stamina;
        this.baseMagicPower = magicPower;
        this.baseAttackSpeed = attackSpeed;
        this.baseDefense = defense;
    }
    
    public int getBaseHealth() {
        return baseHealth;
    }
    
    public int getBaseMana() {
        return baseMana;
    }
    
    public int getBaseStamina() {
        return baseStamina;
    }
    
    public int getBaseMagicPower() {
        return baseMagicPower;
    }
    
    public int getBaseAttackSpeed() {
        return baseAttackSpeed;
    }
    
    public int getBaseDefense() {
        return baseDefense;
    }
    
    /**
     * Calcula las estadísticas para un nivel específico
     */
    public ClassStats getStatsForLevel(int level) {
        double multiplier = 1.0 + (level - 1) * 0.1; // 10% por nivel
        
        return new ClassStats(
            (int)(baseHealth * multiplier),
            (int)(baseMana * multiplier),
            (int)(baseStamina * multiplier),
            (int)(baseMagicPower * multiplier),
            (int)(baseAttackSpeed * multiplier),
            (int)(baseDefense * multiplier)
        );
    }
}
