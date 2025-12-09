package com.nightslayer.mmorpg.squads;

public enum SquadRole {
    CAPTAIN("Capitán", 0),
    LIEUTENANT("Teniente", 1),
    MEMBER("Miembro", 2);

    private final String displayName;
    private final int rank;

    SquadRole(String displayName, int rank) {
        this.displayName = displayName;
        this.rank = rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRank() {
        return rank;
    }

    public boolean hasPermission(String permission) {
        switch (this) {
            case CAPTAIN:
                return true;
            case LIEUTENANT:
                return !permission.equals("DISBAND_SQUAD") && !permission.equals("CHANGE_RANKS");
            case MEMBER:
                return permission.equals("ACCESS_SQUAD_LOG");
            default:
                return false;
        }
    }

    public static SquadRole fromString(String value) {
        try {
            return SquadRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
