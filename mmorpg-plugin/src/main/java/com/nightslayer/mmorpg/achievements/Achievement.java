package com.nightslayer.mmorpg.achievements;

/**
 * Modelo de logro con trigger y recompensa
 */
public class Achievement {
    public enum TriggerType {
        KILL_ANY,
        KILL_MOB
    }

    private final String id;
    private final String name;
    private final String description;
    private final TriggerType triggerType;
    private final String mobId; // solo para KILL_MOB
    private final int target;
    private final AchievementReward reward;

    public Achievement(String id, String name, String description, TriggerType triggerType, String mobId, int target, AchievementReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.triggerType = triggerType;
        this.mobId = mobId;
        this.target = target;
        this.reward = reward;
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

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public String getMobId() {
        return mobId;
    }

    public int getTarget() {
        return target;
    }

    public AchievementReward getReward() {
        return reward;
    }
}
