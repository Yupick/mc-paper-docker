package com.nightslayer.mmorpg.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una quest completa
 */
public class Quest {
    private final String id;
    private final String name;
    private final String description;
    private final int requiredLevel;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final String npcGiverId;
    private final QuestDifficulty difficulty;
    private boolean repeatable;
    private long cooldownTime; // En milisegundos
    
    public enum QuestDifficulty {
        EASY("§aFácil", 1.0),
        NORMAL("§eNormal", 1.5),
        HARD("§6Difícil", 2.0),
        EPIC("§5Épica", 3.0),
        LEGENDARY("§cLegendaria", 5.0);
        
        private final String display;
        private final double rewardMultiplier;
        
        QuestDifficulty(String display, double rewardMultiplier) {
            this.display = display;
            this.rewardMultiplier = rewardMultiplier;
        }
        
        public String getDisplay() {
            return display;
        }
        
        public double getRewardMultiplier() {
            return rewardMultiplier;
        }
    }
    
    public Quest(String id, String name, String description, int requiredLevel, String npcGiverId, QuestDifficulty difficulty) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.npcGiverId = npcGiverId;
        this.difficulty = difficulty;
        this.objectives = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.repeatable = false;
        this.cooldownTime = 0;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public String getNpcGiverId() {
        return npcGiverId;
    }
    
    public QuestDifficulty getDifficulty() {
        return difficulty;
    }
    
    public List<QuestObjective> getObjectives() {
        return objectives;
    }
    
    public void addObjective(QuestObjective objective) {
        objectives.add(objective);
    }
    
    public List<QuestReward> getRewards() {
        return rewards;
    }
    
    public void addReward(QuestReward reward) {
        rewards.add(reward);
    }
    
    public boolean isRepeatable() {
        return repeatable;
    }
    
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
    
    public long getCooldownTime() {
        return cooldownTime;
    }
    
    public void setCooldownTime(long cooldownTime) {
        this.cooldownTime = cooldownTime;
    }
    
    /**
     * Serializa a JSON
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("requiredLevel", requiredLevel);
        json.addProperty("npcGiver", npcGiverId);
        json.addProperty("difficulty", difficulty.name());
        json.addProperty("repeatable", repeatable);
        json.addProperty("cooldown", cooldownTime);
        
        // Objetivos
        JsonArray objectivesJson = new JsonArray();
        for (QuestObjective obj : objectives) {
            objectivesJson.add(obj.toJson());
        }
        json.add("objectives", objectivesJson);
        
        // Recompensas
        JsonArray rewardsJson = new JsonArray();
        for (QuestReward reward : rewards) {
            rewardsJson.add(reward.toJson());
        }
        json.add("rewards", rewardsJson);
        
        return json;
    }
}
