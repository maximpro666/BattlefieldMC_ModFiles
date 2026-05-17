package com.yourmod.teamsystem.client;

public class SpeakingPlayer {
    private final String name;
    private final String rankPrefix;
    private final long startTime;
    private static final long DISPLAY_DURATION_MS = 3000;

    public SpeakingPlayer(String name, String rankPrefix) {
        this.name = name;
        this.rankPrefix = rankPrefix;
        this.startTime = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public String getRankPrefix() { return rankPrefix; }
    public long getStartTime() { return startTime; }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > DISPLAY_DURATION_MS;
    }

    public long getRemainingMs() {
        return Math.max(0, DISPLAY_DURATION_MS - (System.currentTimeMillis() - startTime));
    }
}
