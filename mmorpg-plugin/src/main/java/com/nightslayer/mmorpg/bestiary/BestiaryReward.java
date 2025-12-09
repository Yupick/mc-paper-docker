package com.nightslayer.mmorpg.bestiary;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa las recompensas por descubrimientos y progreso en el bestiario
 */
public class BestiaryReward {
    private final String id;
    private final String name;
    private final int xp;
    private final int coins;
    private final String title;
    private final List<String> items;
    private final boolean broadcast;

    public BestiaryReward(String id, String name, int xp, int coins, String title, 
                         List<String> items, boolean broadcast) {
        this.id = id;
        this.name = name;
        this.xp = xp;
        this.coins = coins;
        this.title = title;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.broadcast = broadcast;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    public boolean shouldBroadcast() {
        return broadcast;
    }

    public boolean hasTitle() {
        return title != null && !title.isEmpty();
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }
}
