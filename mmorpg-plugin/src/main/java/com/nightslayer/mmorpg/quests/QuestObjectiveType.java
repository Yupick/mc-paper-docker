package com.nightslayer.mmorpg.quests;

/**
 * Tipos de objetivos de quest
 */
public enum QuestObjectiveType {
    KILL("Eliminar", "Elimina %amount% %target%"),
    COLLECT("Recolectar", "Recolecta %amount% %target%"),
    TALK("Hablar", "Habla con %target%"),
    REACH("Llegar", "Llega a %target%"),
    USE("Usar", "Usa %amount% %target%"),
    DELIVER("Entregar", "Entrega %amount% %target% a %recipient%");
    
    private final String displayName;
    private final String template;
    
    QuestObjectiveType(String displayName, String template) {
        this.displayName = displayName;
        this.template = template;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getTemplate() {
        return template;
    }
}
