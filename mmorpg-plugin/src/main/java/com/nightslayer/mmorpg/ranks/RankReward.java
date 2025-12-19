package com.nightslayer.mmorpg.ranks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recompensas de un rango
 */
public class RankReward {
    private final int xp;
    private final int coins;
    private final String title;
    private final List<String> permissions;
    private final List<String> items;
    private final boolean broadcast;

    public RankReward(int xp, int coins, String title, List<String> permissions, List<String> items, boolean broadcast) {
        this.xp = xp;
        this.coins = coins;
        this.title = title;
        this.permissions = permissions != null ? new ArrayList<>(permissions) : new ArrayList<>();
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
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

    public List<String> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isBroadcast() {
        return broadcast;
    }
}
