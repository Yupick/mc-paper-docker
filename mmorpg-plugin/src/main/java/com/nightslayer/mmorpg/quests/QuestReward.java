package com.nightslayer.mmorpg.quests;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Representa una recompensa de quest
 */
public class QuestReward {
    private final RewardType type;
    private final int amount;
    private final String identifier; // ID de item, clase, etc.
    
    public enum RewardType {
        EXPERIENCE,
        MONEY,
        ITEM,
        CLASS_SKILL_POINT
    }
    
    public QuestReward(RewardType type, int amount) {
        this(type, amount, null);
    }
    
    public QuestReward(RewardType type, int amount, String identifier) {
        this.type = type;
        this.amount = amount;
        this.identifier = identifier;
    }
    
    public RewardType getType() {
        return type;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Obtiene la descripción de la recompensa
     */
    public String getDescription() {
        switch (type) {
            case EXPERIENCE:
                return "§b+" + amount + " XP";
            case MONEY:
                return "§6+" + amount + " monedas";
            case ITEM:
                return "§a" + amount + "x " + identifier;
            case CLASS_SKILL_POINT:
                return "§d+" + amount + " punto(s) de habilidad";
            default:
                return "Recompensa desconocida";
        }
    }
    
    /**
     * Crea un ItemStack si la recompensa es un item
     */
    public ItemStack createItemStack() {
        if (type != RewardType.ITEM || identifier == null) {
            return null;
        }
        
        try {
            Material material = Material.valueOf(identifier.toUpperCase());
            return new ItemStack(material, amount);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Serializa a JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("amount", amount);
        if (identifier != null) {
            json.addProperty("identifier", identifier);
        }
        return json;
    }
}
