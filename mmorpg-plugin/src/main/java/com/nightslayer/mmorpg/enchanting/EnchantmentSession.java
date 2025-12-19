package com.nightslayer.mmorpg.enchanting;

import java.util.UUID;

public class EnchantmentSession {
    private final String sessionId;
    private final UUID playerUUID;
    private final EnchantedItem item;
    private final RPGEnchantment enchantment;
    private final int targetLevel;
    private final long startTime;
    private final long durationMs;
    private final int costXP;
    private final int costCoins;
    private EnchantmentStatus status;

    public enum EnchantmentStatus {
        IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }

    public EnchantmentSession(String sessionId, UUID playerUUID, EnchantedItem item,
                             RPGEnchantment enchantment, int targetLevel,
                             int costXP, int costCoins, int durationSeconds) {
        this.sessionId = sessionId;
        this.playerUUID = playerUUID;
        this.item = item;
        this.enchantment = enchantment;
        this.targetLevel = targetLevel;
        this.costXP = costXP;
        this.costCoins = costCoins;
        this.startTime = System.currentTimeMillis();
        this.durationMs = durationSeconds * 1000L;
        this.status = EnchantmentStatus.IN_PROGRESS;
    }

    public boolean isComplete() {
        return (System.currentTimeMillis() - startTime) >= durationMs;
    }

    public long getTimeRemaining() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, durationMs - elapsed);
    }

    public double getProgress() {
        long elapsed = Math.min(System.currentTimeMillis() - startTime, durationMs);
        return (double) elapsed / durationMs;
    }

    public void complete() {
        this.status = EnchantmentStatus.COMPLETED;
    }

    public void fail() {
        this.status = EnchantmentStatus.FAILED;
    }

    public void cancel() {
        this.status = EnchantmentStatus.CANCELLED;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public UUID getPlayerUUID() { return playerUUID; }
    public EnchantedItem getItem() { return item; }
    public RPGEnchantment getEnchantment() { return enchantment; }
    public int getTargetLevel() { return targetLevel; }
    public long getStartTime() { return startTime; }
    public int getCostXP() { return costXP; }
    public int getCostCoins() { return costCoins; }
    public EnchantmentStatus getStatus() { return status; }
    public long getDurationMs() { return durationMs; }
}
