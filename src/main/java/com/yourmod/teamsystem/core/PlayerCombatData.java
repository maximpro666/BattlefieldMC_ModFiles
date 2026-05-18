package com.yourmod.teamsystem.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class PlayerCombatData {
    private Team team;
    private int kills;
    private int deaths;
    private int squadId;
    private int rankOrdinal = 0;
    private String prefix;
    private String suffix;
    private String displayName;
    private int battleCredits;
    private int scorePoints;
    private final Set<String> unlockedKits;
    private final Map<String, CompoundTag> savedAttachments;

    private String callsign = "";
    private String rankPrefix = "";
    private boolean isAdmin = false;
    private int donatTier = 0;
    private String playerTitle = "";
    private String loadoutConfig = "";

    private boolean hasChosenTeam = false;

    public PlayerCombatData() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
        this.prefix = "";
        this.suffix = "";
        this.displayName = "";
        this.battleCredits = 0;
        this.scorePoints = 0;
        this.unlockedKits = new HashSet<>();
        this.savedAttachments = new HashMap<>();
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public boolean hasChosenTeam() { return hasChosenTeam; }
    public void setHasChosenTeam(boolean v) { this.hasChosenTeam = v; }

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

    public int getRankOrdinal() {
        return rankOrdinal;
    }

    public void setRankOrdinal(int rankOrdinal) {
        this.rankOrdinal = Math.max(0, rankOrdinal);
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

    public int getBattleCredits() { return battleCredits; }
    public void setBattleCredits(int bc) { this.battleCredits = Math.max(0, bc); }
    public void addBattleCredits(int amount) { this.battleCredits = Math.max(0, this.battleCredits + amount); }
    public boolean deductBattleCredits(int amount) {
        if (this.battleCredits < amount) return false;
        this.battleCredits -= amount;
        return true;
    }
    public int getScorePoints() { return scorePoints; }
    public void setScorePoints(int sp) { this.scorePoints = Math.max(0, sp); }
    public void addScorePoints(int amount) { this.scorePoints = Math.max(0, this.scorePoints + amount); }
    public Set<String> getUnlockedKits() { return unlockedKits; }
    public boolean isKitUnlocked(String kitName) { return unlockedKits.contains(kitName); }
    public void unlockKit(String kitName) { unlockedKits.add(kitName); }
    public void lockKit(String kitName) { unlockedKits.remove(kitName); }
    public Map<String, CompoundTag> getSavedAttachments() { return savedAttachments; }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign != null ? callsign : ""; }
    public String getRankPrefix() { return rankPrefix; }
    public void setRankPrefix(String rankPrefix) { this.rankPrefix = rankPrefix != null ? rankPrefix : ""; }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
    public int getDonatTier() { return donatTier; }
    public void setDonatTier(int donatTier) { this.donatTier = Math.max(0, Math.min(3, donatTier)); }
    public String getPlayerTitle() { return playerTitle; }
    public void setPlayerTitle(String playerTitle) { this.playerTitle = playerTitle != null ? playerTitle : ""; }
    public String getLoadoutConfig() { return loadoutConfig; }
    public void setLoadoutConfig(String loadoutConfig) { this.loadoutConfig = loadoutConfig != null ? loadoutConfig : ""; }

    public void reset() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
        this.prefix = "";
        this.suffix = "";
        this.displayName = "";
        this.scorePoints = 0;
        this.callsign = "";
        this.rankPrefix = "";
        this.isAdmin = false;
        this.donatTier = 0;
        this.playerTitle = "";
        this.loadoutConfig = "";
    }

    // ===== NBT Serialization =====

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Team", team.ordinal());
        tag.putInt("Kills", kills);
        tag.putInt("Deaths", deaths);
        tag.putInt("SquadId", squadId);
        tag.putInt("RankOrdinal", rankOrdinal);
        tag.putString("Prefix", prefix);
        tag.putString("Suffix", suffix);
        tag.putString("DisplayName", displayName);
        tag.putInt("BattleCredits", battleCredits);
        tag.putString("Callsign", callsign);
        tag.putString("RankPrefix", rankPrefix);
        tag.putBoolean("IsAdmin", isAdmin);
        tag.putInt("DonatTier", donatTier);
        tag.putString("PlayerTitle", playerTitle);
        tag.putString("LoadoutConfig", loadoutConfig);
        {
            CompoundTag attTag = new CompoundTag();
            for (Map.Entry<String, CompoundTag> e : savedAttachments.entrySet()) {
                attTag.put(e.getKey(), e.getValue());
            }
            tag.put("SavedAttachments", attTag);
        }
        {
            ListTag kitList = new ListTag();
            for (String kit : unlockedKits) {
                kitList.add(StringTag.valueOf(kit));
            }
            tag.put("UnlockedKits", kitList);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.team = Team.fromOrdinal(tag.getInt("Team"));
        this.kills = tag.getInt("Kills");
        this.deaths = tag.getInt("Deaths");
        this.squadId = tag.getInt("SquadId");
        this.rankOrdinal = tag.getInt("RankOrdinal");
        this.prefix = tag.getString("Prefix");
        this.suffix = tag.getString("Suffix");
        this.displayName = tag.getString("DisplayName");
        this.battleCredits = tag.getInt("BattleCredits");
        this.callsign = tag.getString("Callsign");
        this.rankPrefix = tag.getString("RankPrefix");
        this.isAdmin = tag.getBoolean("IsAdmin");
        this.donatTier = tag.getInt("DonatTier");
        this.playerTitle = tag.getString("PlayerTitle");
        this.loadoutConfig = tag.contains("LoadoutConfig") ? tag.getString("LoadoutConfig") : "";
        if (tag.contains("SavedAttachments")) {
            CompoundTag attTag = tag.getCompound("SavedAttachments");
            savedAttachments.clear();
            for (String key : attTag.getAllKeys()) {
                savedAttachments.put(key, attTag.getCompound(key));
            }
        }
        if (tag.contains("UnlockedKits")) {
            ListTag kitList = tag.getList("UnlockedKits", Tag.TAG_STRING);
            unlockedKits.clear();
            for (Tag base : kitList) {
                unlockedKits.add(base.getAsString());
            }
        }
    }

    public static PlayerCombatData fromNBT(CompoundTag tag) {
        PlayerCombatData data = new PlayerCombatData();
        data.deserializeNBT(tag);
        return data;
    }

    @Override
    public String toString() {
        return String.format("PlayerCombatData{team=%s, kills=%d, deaths=%d, kd=%.2f, squad=%d, callsign='%s', rankPrefix='%s', displayName='%s'}",
                team.getName(), kills, deaths, getKDRatio(), squadId, callsign, rankPrefix, displayName);
    }
}
