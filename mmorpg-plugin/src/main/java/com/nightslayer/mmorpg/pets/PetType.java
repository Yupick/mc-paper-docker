package com.nightslayer.mmorpg.pets;

public enum PetType {
    COMBAT("Combate", "Mascotas que luchan junto a ti"),
    SUPPORT("Soporte", "Mascotas que otorgan buffs y ayudas"),
    GATHERING("Recolección", "Mascotas que ayudan a recolectar recursos");

    private final String displayName;
    private final String description;

    PetType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
