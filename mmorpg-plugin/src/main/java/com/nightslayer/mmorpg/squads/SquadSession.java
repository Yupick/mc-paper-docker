package com.nightslayer.mmorpg.squads;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SquadSession {
    private final String squadId;
    private final String squadName;
    private final String captainUuid;
    private int level;
    private long treasuryCoins;
    private long treasuryXp;
    private final Map<String, SquadMember> members;

    public SquadSession(String squadId, String squadName, String captainUuid, int level) {
        this.squadId = squadId;
        this.squadName = squadName;
        this.captainUuid = captainUuid;
        this.level = level;
        this.treasuryCoins = 0;
        this.treasuryXp = 0;
        this.members = new ConcurrentHashMap<>();
    }

    public void addMember(String playerUuid, String playerName, SquadRole role) {
        members.put(playerUuid, new SquadMember(playerUuid, playerName, role));
    }

    public void removeMember(String playerUuid) {
        members.remove(playerUuid);
    }

    public void setMemberRole(String playerUuid, SquadRole newRole) {
        SquadMember member = members.get(playerUuid);
        if (member != null) {
            member.setRole(newRole);
        }
    }

    public void addMemberContribution(String playerUuid, long coins, long xp) {
        SquadMember member = members.get(playerUuid);
        if (member != null) {
            member.addContribution(coins, xp);
        }
    }

    public void addTreasuryCoins(long coins) {
        this.treasuryCoins += coins;
    }

    public void withdrawTreasuryCoins(long coins) {
        this.treasuryCoins = Math.max(0, this.treasuryCoins - coins);
    }

    public void addTreasuryXp(long xp) {
        this.treasuryXp += xp;
    }

    public void withdrawTreasuryXp(long xp) {
        this.treasuryXp = Math.max(0, this.treasuryXp - xp);
    }

    public void setLevel(int newLevel) {
        this.level = Math.min(newLevel, 5);
    }

    // Getters
    public String getSquadId() { return squadId; }
    public String getSquadName() { return squadName; }
    public String getCaptainUuid() { return captainUuid; }
    public int getLevel() { return level; }
    public long getTreasuryCoins() { return treasuryCoins; }
    public long getTreasuryXp() { return treasuryXp; }
    public Map<String, SquadMember> getMembers() { return new HashMap<>(members); }
    public int getMemberCount() { return members.size(); }
    public boolean isMember(String playerUuid) { return members.containsKey(playerUuid); }
    public SquadMember getMember(String playerUuid) { return members.get(playerUuid); }

    public static class SquadMember {
        private final String playerUuid;
        private final String playerName;
        private SquadRole role;
        private long contributionCoins;
        private long contributionXp;

        public SquadMember(String playerUuid, String playerName, SquadRole role) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.role = role;
            this.contributionCoins = 0;
            this.contributionXp = 0;
        }

        public void addContribution(long coins, long xp) {
            this.contributionCoins += coins;
            this.contributionXp += xp;
        }

        public void setRole(SquadRole role) {
            this.role = role;
        }

        public String getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public SquadRole getRole() { return role; }
        public long getContributionCoins() { return contributionCoins; }
        public long getContributionXp() { return contributionXp; }
    }
}
