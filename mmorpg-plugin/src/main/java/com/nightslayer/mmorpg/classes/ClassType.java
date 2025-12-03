package com.nightslayer.mmorpg.classes;

/**
 * Tipos de clases disponibles en el sistema RPG
 */
public enum ClassType {
    WARRIOR("Guerrero", "⚔", "Maestro del combate cuerpo a cuerpo", 
            new ClassStats(120, 100, 50, 30, 15, 10)),
    MAGE("Mago", "✦", "Manipulador de energía arcana", 
            new ClassStats(80, 200, 30, 100, 25, 8)),
    ARCHER("Arquero", "➶", "Experto en ataques a distancia", 
            new ClassStats(90, 120, 80, 60, 40, 12)),
    ASSASSIN("Asesino", "🗡", "Maestro del sigilo y ataques críticos", 
            new ClassStats(85, 110, 90, 40, 60, 9)),
    CLERIC("Clérigo", "✝", "Sanador sagrado y protector", 
            new ClassStats(95, 180, 35, 90, 15, 11)),
    PALADIN("Paladín", "🛡", "Guerrero sagrado con poderes divinos", 
            new ClassStats(115, 140, 45, 70, 20, 14)),
    NONE("Sin clase", "", "Sin especialización", 
            new ClassStats(100, 100, 50, 50, 20, 10));
    
    private final String displayName;
    private final String icon;
    private final String description;
    private final ClassStats baseStats;
    
    ClassType(String displayName, String icon, String description, ClassStats baseStats) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.baseStats = baseStats;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ClassStats getBaseStats() {
        return baseStats;
    }
    
    public static ClassType fromString(String name) {
        for (ClassType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return NONE;
    }
}
