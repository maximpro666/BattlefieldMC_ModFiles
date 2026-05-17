package com.yourmod.teamsystem.core;

import net.minecraft.nbt.CompoundTag;

public class PlayerCombatData {
    private Team team;
    private int kills;
    private int deaths;
    private int squadId;
    private String prefix;
    private String suffix;
    private String displayName;

    public PlayerCombatData() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
        this.prefix = "";
        this.suffix = "";
        this.displayName = "";
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void addDeath() {
        this.deaths++;
    }

    public int getSquadId() {
        return squadId;
    }

    public void setSquadId(int squadId) {
        this.squadId = squadId;
    }

    public boolean hasSquad() {
        return squadId >= 0;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix != null ? suffix : "";
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
    }

    public double getKDRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    public void resetStats() {
        this.kills = 0;
        this.deaths = 0;
    }

    public void reset() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
        this.prefix = "";
        this.suffix = "";
        this.displayName = "";
    }

    // ===== NBT Serialization =====

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Team", team.ordinal());
        tag.putInt("Kills", kills);
        tag.putInt("Deaths", deaths);
        tag.putInt("SquadId", squadId);
        tag.putString("Prefix", prefix);
        tag.putString("Suffix", suffix);
        tag.putString("DisplayName", displayName);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.team = Team.fromOrdinal(tag.getInt("Team"));
        this.kills = tag.getInt("Kills");
        this.deaths = tag.getInt("Deaths");
        this.squadId = tag.getInt("SquadId");
        this.prefix = tag.getString("Prefix");
        this.suffix = tag.getString("Suffix");
        this.displayName = tag.getString("DisplayName");
    }

    public static PlayerCombatData fromNBT(CompoundTag tag) {
        PlayerCombatData data = new PlayerCombatData();
        data.deserializeNBT(tag);
        return data;
    }

    @Override
    public String toString() {
        return String.format("PlayerCombatData{team=%s, kills=%d, deaths=%d, kd=%.2f, squad=%d, prefix='%s', suffix='%s', displayName='%s'}",
                team.getName(), kills, deaths, getKDRatio(), squadId, prefix, suffix, displayName);
    }
}
