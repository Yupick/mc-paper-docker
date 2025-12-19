package com.nightslayer.mmorpg.classes;

import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representa la clase y progresión de un jugador
 */
public class PlayerClass {
    private final UUID playerUUID;
    private ClassType classType;
    private int level;
    private int experience;
    private int mana;
    private int maxMana;
    private final Map<String, Long> abilityCooldowns;
    private final Map<String, Integer> skillLevels;
    
    public PlayerClass(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.classType = ClassType.NONE;
        this.level = 1;
        this.experience = 0;
        this.mana = 100;
        this.maxMana = 100;
        this.abilityCooldowns = new HashMap<>();
        this.skillLevels = new HashMap<>();
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public ClassType getClassType() {
        return classType;
    }
    
    public void setClassType(ClassType classType) {
        this.classType = classType;
        this.updateStatsForClass();
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
        this.updateStatsForClass();
    }
    
    public int getExperience() {
        return experience;
    }
    
    public void addExperience(int amount) {
        this.experience += amount;
        
        // Verificar subida de nivel
        int requiredExp = getRequiredExperience();
        while (this.experience >= requiredExp) {
            this.experience -= requiredExp;
            this.level++;
            this.updateStatsForClass();
            requiredExp = getRequiredExperience();
        }
    }
    
    public int getRequiredExperience() {
        return 100 * level; // 100 XP por nivel
    }
    
    public int getMana() {
        return mana;
    }
    
    public int getMaxMana() {
        return maxMana;
    }
    
    public void setMana(int mana) {
        this.mana = Math.min(mana, this.maxMana);
    }
    
    public void consumeMana(int amount) {
        this.mana = Math.max(0, this.mana - amount);
    }
    
    public void regenerateMana(int amount) {
        this.mana = Math.min(this.maxMana, this.mana + amount);
    }
    
    public boolean isAbilityOnCooldown(String abilityId) {
        Long cooldownEnd = abilityCooldowns.get(abilityId);
        if (cooldownEnd == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= cooldownEnd) {
            abilityCooldowns.remove(abilityId);
            return false;
        }
        
        return true;
    }
    
    public void setAbilityCooldown(String abilityId, long cooldownEnd) {
        abilityCooldowns.put(abilityId, cooldownEnd);
    }
    
    public long getRemainingCooldown(String abilityId) {
        Long cooldownEnd = abilityCooldowns.get(abilityId);
        if (cooldownEnd == null) {
            return 0;
        }
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000); // Convertir a segundos
    }
    
    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }
    
    public void setSkillLevel(String skillId, int level) {
        skillLevels.put(skillId, level);
    }
    
    private void updateStatsForClass() {
        if (classType == ClassType.NONE) {
            this.maxMana = 100;
        } else {
            ClassStats stats = classType.getBaseStats().getStatsForLevel(level);
            this.maxMana = stats.getBaseMana();
            this.mana = Math.min(this.mana, this.maxMana);
        }
    }
    
    /**
     * Aplica los atributos de la clase al jugador de Minecraft
     */
    public void applyToPlayer(Player player) {
        if (classType == ClassType.NONE) {
            return;
        }
        
        ClassStats stats = classType.getBaseStats().getStatsForLevel(level);
        // Aplicar vida máxima usando atributos (Paper/Spigot moderno)
        double newMaxHealth = stats.getBaseHealth() / 5.0; // 20 = 10 corazones
        player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
        player.setHealth(Math.min(player.getHealth(), newMaxHealth));
    }
    
    /**
     * Serializa a JSON para guardar
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", playerUUID.toString());
        json.addProperty("class", classType.name());
        json.addProperty("level", level);
        json.addProperty("experience", experience);
        json.addProperty("mana", mana);
        json.addProperty("maxMana", maxMana);
        
        // Cooldowns activos
        JsonObject cooldownsJson = new JsonObject();
        for (Map.Entry<String, Long> entry : abilityCooldowns.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis()) {
                cooldownsJson.addProperty(entry.getKey(), entry.getValue());
            }
        }
        json.add("cooldowns", cooldownsJson);
        
        // Niveles de habilidades
        JsonObject skillsJson = new JsonObject();
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            skillsJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("skills", skillsJson);
        
        return json;
    }
    
    /**
     * Carga desde JSON
     */
    public static PlayerClass fromJson(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        PlayerClass playerClass = new PlayerClass(uuid);
        
        playerClass.classType = ClassType.valueOf(json.get("class").getAsString());
        playerClass.level = json.get("level").getAsInt();
        playerClass.experience = json.get("experience").getAsInt();
        playerClass.mana = json.get("mana").getAsInt();
        playerClass.maxMana = json.get("maxMana").getAsInt();
        
        // Cargar cooldowns
        if (json.has("cooldowns")) {
            JsonObject cooldowns = json.getAsJsonObject("cooldowns");
            for (String key : cooldowns.keySet()) {
                playerClass.abilityCooldowns.put(key, cooldowns.get(key).getAsLong());
            }
        }
        
        // Cargar skills
        if (json.has("skills")) {
            JsonObject skills = json.getAsJsonObject("skills");
            for (String key : skills.keySet()) {
                playerClass.skillLevels.put(key, skills.get(key).getAsInt());
            }
        }
        
        return playerClass;
    }
}
