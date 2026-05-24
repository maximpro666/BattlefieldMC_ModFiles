package com.pigeostudios.pwp.punishment;

public enum StaffRole {
    MOD(0, "Moderator"),
    ADMIN(2, "Admin"),
    OWNER(4, "Owner");

    private final int opLevel;
    private final String displayName;

    StaffRole(int opLevel, String displayName) {
        this.opLevel = opLevel;
        this.displayName = displayName;
    }

    public int getOpLevel() { return opLevel; }
    public String getDisplayName() { return displayName; }

    public boolean canUse(PunishmentType type) {
        return switch (this) {
            case MOD -> type == PunishmentType.WARN;
            case ADMIN -> true;
            case OWNER -> true;
        };
    }

    public boolean hasMinOpLevel(int level) {
        return opLevel <= level;
    }
}
