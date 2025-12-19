package com.nightslayer.mmorpg.crafting;

import org.bukkit.inventory.Inventory;
import java.util.UUID;

public class CraftingSession {
    private final String sessionId;
    private final UUID playerUUID;
    private final Recipe recipe;
    private final CraftingStation station;
    private final long startTime;
    private final long completionTime;
    private final Inventory inventorySnapshot;
    private boolean completed;

    public CraftingSession(String sessionId, UUID playerUUID, Recipe recipe, 
                          CraftingStation station, Inventory inventorySnapshot) {
        this.sessionId = sessionId;
        this.playerUUID = playerUUID;
        this.recipe = recipe;
        this.station = station;
        this.startTime = System.currentTimeMillis();
        this.completionTime = startTime + (recipe.getCraftingTimeSeconds() * 1000L);
        this.inventorySnapshot = inventorySnapshot;
        this.completed = false;
    }

    public boolean isComplete() {
        return System.currentTimeMillis() >= completionTime;
    }

    public long getTimeRemaining() {
        long remaining = completionTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public double getProgress() {
        long total = recipe.getCraftingTimeSeconds() * 1000L;
        long elapsed = getElapsedTime();
        return Math.min(1.0, (double) elapsed / total);
    }

    public void markComplete() {
        this.completed = true;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public UUID getPlayerUUID() { return playerUUID; }
    public Recipe getRecipe() { return recipe; }
    public CraftingStation getStation() { return station; }
    public long getStartTime() { return startTime; }
    public long getCompletionTime() { return completionTime; }
    public Inventory getInventorySnapshot() { return inventorySnapshot; }
    public boolean isCompleted() { return completed; }
    public int getCraftingTimeSeconds() { return recipe.getCraftingTimeSeconds(); }
}
