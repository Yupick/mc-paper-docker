package com.nightslayer.mmorpg.npcs;

/**
 * Tipos de NPCs disponibles
 */
public enum NPCType {
    QUEST_GIVER("Dador de Misiones", "§e", "Ofrece misiones y recompensas"),
    MERCHANT("Comerciante", "§a", "Compra y vende objetos"),
    TRAINER("Entrenador", "§6", "Enseña habilidades y mejoras"),
    GUARD("Guardia", "§c", "Protege áreas y ayuda en combate"),
    VILLAGER("Aldeano", "§7", "NPC genérico con diálogos");
    
    private final String displayName;
    private final String colorCode;
    private final String description;
    
    NPCType(String displayName, String colorCode, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public String getDescription() {
        return description;
    }
}
