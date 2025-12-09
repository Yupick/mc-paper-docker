package com.nightslayer.mmorpg.enchanting;

import org.bukkit.inventory.ItemStack;
import java.util.*;

public class EnchantedItem {
    private final ItemStack item;
    private final Map<String, Integer> enchantments; // enchantment_id -> level
    private final long createdAt;
    private final UUID createdBy;
    private int experienceInvested;

    public EnchantedItem(ItemStack item, UUID createdBy) {
        this.item = new ItemStack(item);
        this.enchantments = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.createdBy = createdBy;
        this.experienceInvested = 0;
    }

    public boolean addEnchantment(String enchantmentId, int level) {
        if (level < 1) return false;
        enchantments.put(enchantmentId, level);
        return true;
    }

    public boolean hasEnchantment(String enchantmentId) {
        return enchantments.containsKey(enchantmentId);
    }

    public int getEnchantmentLevel(String enchantmentId) {
        return enchantments.getOrDefault(enchantmentId, 0);
    }

    public void removeEnchantment(String enchantmentId) {
        enchantments.remove(enchantmentId);
    }

    public boolean canAddEnchantment(String enchantmentId, Set<String> incompatibilities) {
        for (String existingEnch : enchantments.keySet()) {
            if (incompatibilities.contains(existingEnch)) {
                return false;
            }
        }
        return true;
    }

    public String getDisplayName() {
        String baseName = item.getType().toString();
        if (enchantments.isEmpty()) {
            return baseName;
        }
        return baseName + " [" + enchantments.size() + " enchantments]";
    }

    // Getters
    public ItemStack getItem() { return new ItemStack(item); }
    public Map<String, Integer> getEnchantments() { return new HashMap<>(enchantments); }
    public long getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public int getExperienceInvested() { return experienceInvested; }
    public void addExperienceInvested(int xp) { this.experienceInvested += xp; }
    public int getEnchantmentCount() { return enchantments.size(); }
}
