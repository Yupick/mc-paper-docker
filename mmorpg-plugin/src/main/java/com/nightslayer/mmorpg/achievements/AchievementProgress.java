package com.nightslayer.mmorpg.achievements;

import java.time.LocalDateTime;

/**
 * Progreso individual de un logro para un jugador
 */
public class AchievementProgress {
    private final String achievementId;
    private int progress;
    private boolean completed;
    private LocalDateTime completedAt;

    public AchievementProgress(String achievementId) {
        this.achievementId = achievementId;
        this.progress = 0;
        this.completed = false;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public boolean incrementAndCheck(int target) {
        if (completed) {
            return false;
        }
        progress++;
        if (progress >= target) {
            completed = true;
            completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
