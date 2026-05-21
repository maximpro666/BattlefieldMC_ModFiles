package com.pigeostudios.pwp.core;

public enum Rank {
    PRIVATE(0, "Private", "[Pvt]", "Рядовой", "[Ряд]"),
    PFC(5, "Private First Class", "[PFC]", "Ефрейтор", "[Ефр]"),
    CORPORAL(15, "Corporal", "[Cpl]", "Младший сержант", "[МлСр]"),
    SERGEANT(30, "Sergeant", "[Sgt]", "Сержант", "[Серж]"),
    STAFF_SERGEANT(50, "Staff Sergeant", "[SSgt]", "Старший сержант", "[СтСр]"),
    LIEUTENANT(80, "Lieutenant", "[Lt]", "Лейтенант", "[Лейт]"),
    CAPTAIN(120, "Captain", "[Cpt]", "Капитан", "[Кап]"),
    MAJOR(175, "Major", "[Maj]", "Майор", "[Май]"),
    COLONEL(250, "Colonel", "[Col]", "Полковник", "[Плк]"),
    GENERAL(400, "General", "[Gen]", "Генерал", "[Ген]");

    private final int killRequirement;
    private final String displayName;
    private final String prefix;
    private final String russianName;
    private final String russianPrefix;

    Rank(int killRequirement, String displayName, String prefix, String russianName, String russianPrefix) {
        this.killRequirement = killRequirement;
        this.displayName = displayName;
        this.prefix = prefix;
        this.russianName = russianName;
        this.russianPrefix = russianPrefix;
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

    public String getRussianName() {
        return russianName;
    }

    public String getRussianPrefix() {
        return russianPrefix;
    }

    public String getDisplayName(boolean russian) {
        return russian ? russianName : displayName;
    }

    public String getPrefix(boolean russian) {
        return russian ? russianPrefix : prefix;
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
