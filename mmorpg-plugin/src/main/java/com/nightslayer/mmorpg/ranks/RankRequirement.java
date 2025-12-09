package com.nightslayer.mmorpg.ranks;

import com.nightslayer.mmorpg.achievements.AchievementManager;
import com.nightslayer.mmorpg.achievements.AchievementProgress;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Requisitos para optar a un rango
 */
public class RankRequirement {
    private final int requiredLevel;
    private final int achievementsCompleted;

    public RankRequirement(int requiredLevel, int achievementsCompleted) {
        this.requiredLevel = requiredLevel;
        this.achievementsCompleted = achievementsCompleted;
    }

    public boolean isMet(Player player, AchievementManager achievementManager) {
        if (player == null) {
            return false;
        }

        if (requiredLevel > 0 && player.getLevel() < requiredLevel) {
            return false;
        }

        if (achievementsCompleted > 0 && achievementManager != null) {
            Map<String, AchievementProgress> progress = achievementManager.getProgress(player.getUniqueId());
            long completed = progress.values().stream().filter(AchievementProgress::isCompleted).count();
            if (completed < achievementsCompleted) {
                return false;
            }
        }

        return true;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getAchievementsCompleted() {
        return achievementsCompleted;
    }
}
