package com.yourmod.teamsystem.core;

public enum Rank {
    PRIVATE(0, "Private", "[Pvt]"),
    PFC(5, "Private First Class", "[PFC]"),
    CORPORAL(15, "Corporal", "[Cpl]"),
    SERGEANT(30, "Sergeant", "[Sgt]"),
    STAFF_SERGEANT(50, "Staff Sergeant", "[SSgt]"),
    LIEUTENANT(80, "Lieutenant", "[Lt]"),
    CAPTAIN(120, "Captain", "[Cpt]"),
    MAJOR(175, "Major", "[Maj]"),
    COLONEL(250, "Colonel", "[Col]"),
    GENERAL(400, "General", "[Gen]");

    private final int killRequirement;
    private final String displayName;
    private final String prefix;

    Rank(int killRequirement, String displayName, String prefix) {
        this.killRequirement = killRequirement;
        this.displayName = displayName;
        this.prefix = prefix;
    }

    public int getKillRequirement() {
        return killRequirement;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public static Rank fromOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return PRIVATE;
    }

    public static Rank fromKills(int kills) {
        Rank highest = PRIVATE;
        for (Rank rank : values()) {
            if (kills >= rank.killRequirement) {
                highest = rank;
            } else {
                break;
            }
        }
        return highest;
    }
}
