package com.nightslayer.mmorpg.pets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerPetData {
    private String playerUuid;
    private List<OwnedPet> ownedPets;
    private List<String> unlockedMounts;
    private String activePetId;
    private String activeMountId;

    public static class OwnedPet {
        private String petId;
        private String customName;
        private int level;
        private int experience;
        private double currentHealth;
        private double hungerLevel;
        private long lastFedTimestamp;
        private Map<String, Long> abilityCooldowns;

        public OwnedPet(String petId) {
            this.petId = petId;
            this.customName = null;
            this.level = 1;
            this.experience = 0;
            this.currentHealth = 100.0;
            this.hungerLevel = 100.0;
            this.lastFedTimestamp = System.currentTimeMillis();
            this.abilityCooldowns = new HashMap<>();
        }

        public String getPetId() { return petId; }
        public String getCustomName() { return customName; }
        public void setCustomName(String customName) { this.customName = customName; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public int getExperience() { return experience; }
        public void setExperience(int experience) { this.experience = experience; }
        public double getCurrentHealth() { return currentHealth; }
        public void setCurrentHealth(double currentHealth) { this.currentHealth = currentHealth; }
        public double getHungerLevel() { return hungerLevel; }
        public void setHungerLevel(double hungerLevel) { this.hungerLevel = hungerLevel; }
        public long getLastFedTimestamp() { return lastFedTimestamp; }
        public void setLastFedTimestamp(long lastFedTimestamp) { this.lastFedTimestamp = lastFedTimestamp; }
        public Map<String, Long> getAbilityCooldowns() { return abilityCooldowns; }

        public void addExperience(int xp) {
            this.experience += xp;
        }

        public void heal(double amount) {
            this.currentHealth = Math.min(100.0, this.currentHealth + amount);
        }

        public void feed(double amount) {
            this.hungerLevel = Math.min(100.0, this.hungerLevel + amount);
            this.lastFedTimestamp = System.currentTimeMillis();
        }

        public boolean isAbilityOnCooldown(String abilityId) {
            Long cooldownEnd = abilityCooldowns.get(abilityId);
            if (cooldownEnd == null) return false;
            return System.currentTimeMillis() < cooldownEnd;
        }

        public void setCooldown(String abilityId, int cooldownSeconds) {
            long cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000L);
            abilityCooldowns.put(abilityId, cooldownEnd);
        }
    }

    public PlayerPetData(String playerUuid) {
        this.playerUuid = playerUuid;
        this.ownedPets = new ArrayList<>();
        this.unlockedMounts = new ArrayList<>();
        this.activePetId = null;
        this.activeMountId = null;
    }

    public String getPlayerUuid() { return playerUuid; }
    public List<OwnedPet> getOwnedPets() { return ownedPets; }
    public List<String> getUnlockedMounts() { return unlockedMounts; }
    public String getActivePetId() { return activePetId; }
    public void setActivePetId(String activePetId) { this.activePetId = activePetId; }
    public String getActiveMountId() { return activeMountId; }
    public void setActiveMountId(String activeMountId) { this.activeMountId = activeMountId; }

    public void adoptPet(String petId) {
        if (!hasPet(petId)) {
            ownedPets.add(new OwnedPet(petId));
        }
    }

    public void unlockMount(String mountId) {
        if (!unlockedMounts.contains(mountId)) {
            unlockedMounts.add(mountId);
        }
    }
    
    public void addMount(String mountId) {
        unlockMount(mountId);
    }
    
    public void setActivePet(String petId) {
        this.activePetId = petId;
    }
    
    public void setActiveMount(String mountId) {
        this.activeMountId = mountId;
    }
    
    public List<String> getOwnedPetIds() {
        List<String> ids = new ArrayList<>();
        for (OwnedPet pet : ownedPets) {
            ids.add(pet.getPetId());
        }
        return ids;
    }
    
    public boolean removePet(String petId) {
        return ownedPets.removeIf(p -> p.getPetId().equals(petId));
    }

    public boolean hasPet(String petId) {
        return ownedPets.stream().anyMatch(p -> p.getPetId().equals(petId));
    }

    public boolean hasMount(String mountId) {
        return unlockedMounts.contains(mountId);
    }

    public OwnedPet getPet(String petId) {
        return ownedPets.stream()
                .filter(p -> p.getPetId().equals(petId))
                .findFirst()
                .orElse(null);
    }

    public OwnedPet getActivePet() {
        if (activePetId == null) return null;
        return getPet(activePetId);
    }

    public int getTotalPets() {
        return ownedPets.size();
    }

    public int getTotalMounts() {
        return unlockedMounts.size();
    }
}
