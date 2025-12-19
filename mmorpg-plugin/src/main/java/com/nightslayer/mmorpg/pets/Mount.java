package com.nightslayer.mmorpg.pets;

public class Mount {
    private String id;
    private String name;
    private String rarity;
    private String description;
    private double speed;
    private double jumpStrength;
    private double health;
    private String specialAbility;
    private int unlockCost;
    private int unlockLevel;

    public Mount(String id, String name, String rarity, String description,
                 double speed, double jumpStrength, double health,
                 String specialAbility, int unlockCost, int unlockLevel) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.description = description;
        this.speed = speed;
        this.jumpStrength = jumpStrength;
        this.health = health;
        this.specialAbility = specialAbility;
        this.unlockCost = unlockCost;
        this.unlockLevel = unlockLevel;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRarity() { return rarity; }
    public String getDescription() { return description; }
    public double getSpeed() { return speed; }
    public double getJumpStrength() { return jumpStrength; }
    public double getHealth() { return health; }
    public String getSpecialAbility() { return specialAbility; }
    public int getUnlockCost() { return unlockCost; }
    public int getUnlockLevel() { return unlockLevel; }

    public boolean hasSpecialAbility() {
        return specialAbility != null && !specialAbility.isEmpty();
    }
}
