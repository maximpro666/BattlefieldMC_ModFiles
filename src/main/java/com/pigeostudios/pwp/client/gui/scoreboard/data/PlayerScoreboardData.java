package com.pigeostudios.pwp.client.gui.scoreboard.data;

public class PlayerScoreboardData {
    public enum DonateLevel {
        NONE, VIP, ELITE, GENERAL
    }

    public String nick;
    public String callsign;
    public String squad;
    public int rankId;
    public int teamOrdinal;
    public int kills;
    public int deaths;
    public DonateLevel donateLevel = DonateLevel.NONE;
    public float donateBarProgress = 0f;
    public int pingMs;
    public boolean isSelf;
    public transient int sortKey;

    public PlayerScoreboardData() {}

    public float getKD() {
        if (deaths == 0) return kills;
        return (float) kills / (float) deaths;
    }
}
