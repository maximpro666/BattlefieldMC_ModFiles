package com.yourmod.teamsystem.data;

public enum LockState {
    AVAILABLE,
    LOCKED_RANK,
    LOCKED_KIT,
    LOCKED_TEAM,
    LOCKED_MAP,
    LOCKED_COST,
    INCOMPATIBLE;

    public String tooltip(String detail) {
        return switch (this) {
            case LOCKED_RANK  -> "Requires Rank: " + detail;
            case LOCKED_KIT   -> "Requires Kit: "  + detail;
            case LOCKED_TEAM  -> detail + " only";
            case LOCKED_MAP   -> "Not on this map";
            case LOCKED_COST  -> "Not enough " + detail;
            case INCOMPATIBLE -> "Not compatible with " + detail;
            default           -> "";
        };
    }

    public boolean isSelectable() { return this == AVAILABLE; }
}
