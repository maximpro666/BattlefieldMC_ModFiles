package com.yourmod.teamsystem.core;

import net.minecraft.nbt.CompoundTag;

public class PlayerCombatData {
    private Team team;
    private int kills;
    private int deaths;
    private int squadId;

    public PlayerCombatData() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
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
    }

    // ===== NBT Serialization =====

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Team", team.ordinal());
        tag.putInt("Kills", kills);
        tag.putInt("Deaths", deaths);
        tag.putInt("SquadId", squadId);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.team = Team.fromOrdinal(tag.getInt("Team"));
        this.kills = tag.getInt("Kills");
        this.deaths = tag.getInt("Deaths");
        this.squadId = tag.getInt("SquadId");
    }

    public static PlayerCombatData fromNBT(CompoundTag tag) {
        PlayerCombatData data = new PlayerCombatData();
        data.deserializeNBT(tag);
        return data;
    }

    @Override
    public String toString() {
        return String.format("PlayerCombatData{team=%s, kills=%d, deaths=%d, kd=%.2f, squad=%d}",
                team.getName(), kills, deaths, getKDRatio(), squadId);
    }
}
