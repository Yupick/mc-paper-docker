package com.nightslayer.mmorpg.ranks;

/**
 * Modelo de rango RPG
 */
public class Rank implements Comparable<Rank> {
    private final String id;
    private final String name;
    private final String description;
    private final int order;
    private final RankRequirement requirement;
    private final RankReward reward;

    public Rank(String id, String name, String description, int order, RankRequirement requirement, RankReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.order = order;
        this.requirement = requirement;
        this.reward = reward;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }

    public RankRequirement getRequirement() {
        return requirement;
    }

    public RankReward getReward() {
        return reward;
    }

    @Override
    public int compareTo(Rank other) {
        return Integer.compare(this.order, other.order);
    }
}
