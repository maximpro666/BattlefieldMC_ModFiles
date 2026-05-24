package com.pigeostudios.pwp.service;

import java.util.Random;

public enum MatchEventType {
    // Combat
    AIRSTRIKE("combat", 10, 30),
    ARTILLERY("combat", 10, 25),
    CARPET_BOMB("combat", 5, 20),
    INCENDIARY("combat", 8, 25),
    GAS_ATTACK("combat", 7, 25),
    CLUSTER_BOMB("combat", 5, 20),

    // Environmental
    SANDSTORM("environment", 8, 45),
    HEAVY_FOG("environment", 8, 45),
    RAINSTORM("environment", 8, 40),
    EARTHQUAKE("environment", 4, 15),

    // Support
    SUPPLY_DROP("support", 10, 30),
    MEDICAL_SUPPLY("support", 10, 30),
    REPAIR_BOOST("support", 8, 30),
    RECON_DRONE("support", 8, 25),
    FORTIFICATION("support", 7, 35),

    // Team bonuses
    DOMINATION("team", 8, 30),
    BLITZ("team", 6, 20),
    OVERWATCH("team", 7, 30),
    DOUBLE_BC("team", 6, 35),
    HALF_COOLDOWN("team", 5, 30),
    REINFORCEMENTS("team", 5, 0),

    // Malus / enemy debuffs
    EMP_STORM("malus", 6, 20),
    RADIO_SILENCE("malus", 7, 30),
    BLACKOUT("malus", 7, 25),
    MINEFIELD("malus", 6, 35),
    HACK("malus", 5, 25),

    // Special
    BOUNTY_HUNTER("special", 6, 40),
    VIP_PROTECT("special", 5, 45),
    LAST_STAND("special", 4, 20),
    TRUCE("special", 2, 15);

    private final String category;
    private final int defaultWeight;
    private int defaultDuration;

    MatchEventType(String category, int defaultWeight, int defaultDuration) {
        this.category = category;
        this.defaultWeight = defaultWeight;
        this.defaultDuration = defaultDuration;
    }

    public String getCategory() { return category; }
    public int getDefaultWeight() { return defaultWeight; }
    public int getDefaultDuration() { return defaultDuration; }

    public String getTranslationKey() { return "event." + name().toLowerCase(); }
    public String getStartKey() { return "event." + name().toLowerCase() + ".start"; }
    public String getEndKey() { return "event." + name().toLowerCase() + ".end"; }

    private static final Random RNG = new Random();
    private static final MatchEventType[] VALUES = values();

    public static MatchEventType randomWeighted(int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        if (total <= 0) return VALUES[RNG.nextInt(VALUES.length)];
        int r = RNG.nextInt(total);
        for (int i = 0; i < VALUES.length; i++) {
            r -= weights[i];
            if (r < 0) return VALUES[i];
        }
        return VALUES[VALUES.length - 1];
    }
}
