package com.nightslayer.mmorpg.achievements;

/**
 * Modelo de recompensa para un logro
 */
public class AchievementReward {
    private int xp;
    private int coins;
    private String title;
    private String item;
    private boolean broadcast;

    public AchievementReward() {
        this.xp = 0;
        this.coins = 0;
        this.title = "";
        this.item = "";
        this.broadcast = false;
    }

    public AchievementReward(int xp, int coins, String title, String item, boolean broadcast) {
        this.xp = xp;
        this.coins = coins;
        this.title = title;
        this.item = item;
        this.broadcast = broadcast;
    }

    public int getXp() {
        return xp;
    }

    public int getCoins() {
        return coins;
    }

    public String getTitle() {
        return title;
    }

    public String getItem() {
        return item;
    }

    public boolean isBroadcast() {
        return broadcast;
    }
}
