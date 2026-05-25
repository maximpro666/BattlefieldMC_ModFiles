package com.pigeostudios.pwp.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private int warCredits;
    private int wins;
    private int rating = 1000;
    private final Set<String> unlockedKits = ConcurrentHashMap.newKeySet();
    private final Set<String> certifications = ConcurrentHashMap.newKeySet();
    private final Map<String, CompoundTag> savedAttachments = new ConcurrentHashMap<>();

    private String callsign = "";
    private String rankPrefix = "";
    private boolean isAdmin = false;
    private int donatTier = 0;
    private String playerTitle = "";
    private String loadoutConfig = "";
    private String selectedKit = "";
    private String selectedRole = "";
    private String selectedLoadout = "";
    private final Set<String> unlockedRoles = ConcurrentHashMap.newKeySet();
    private final Set<String> unlockedLoadouts = ConcurrentHashMap.newKeySet();

    private boolean hasChosenTeam = false;
    private boolean hasReceivedDogTag = false;

    public boolean hasReceivedDogTag() { return hasReceivedDogTag; }
    public synchronized void setHasReceivedDogTag(boolean v) { this.hasReceivedDogTag = v; }

    public PlayerCombatData() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
        this.prefix = "";
        this.suffix = "";
        this.displayName = "";
        this.battleCredits = 0;
        this.warCredits = 0;
    }

    public Team getTeam() {
        return team;
    }

    public synchronized void setTeam(Team team) {
        this.team = team;
    }

    public boolean hasChosenTeam() { return hasChosenTeam; }
    public synchronized void setHasChosenTeam(boolean v) { this.hasChosenTeam = v; }

    public int getKills() {
        return kills;
    }

    public synchronized void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public synchronized void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public synchronized void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public synchronized void addDeath() {
        this.deaths++;
    }

    public int getSquadId() {
        return squadId;
    }

    public synchronized void setSquadId(int squadId) {
        this.squadId = squadId;
    }

    public boolean hasSquad() {
        return squadId >= 0;
    }

    public int getRankOrdinal() {
        return rankOrdinal;
    }

    public synchronized void setRankOrdinal(int rankOrdinal) {
        this.rankOrdinal = Math.max(0, rankOrdinal);
    }

    public String getPrefix() {
        return prefix;
    }

    public synchronized void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public String getSuffix() {
        return suffix;
    }

    public synchronized void setSuffix(String suffix) {
        this.suffix = suffix != null ? suffix : "";
    }

    public String getDisplayName() {
        return displayName;
    }

    public synchronized void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
    }

    public double getKDRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    public synchronized void resetStats() {
        this.kills = 0;
        this.deaths = 0;
    }

    public int getBattleCredits() { return battleCredits; }
    public synchronized void setBattleCredits(int bc) { this.battleCredits = Math.max(0, bc); }
    public synchronized void addBattleCredits(int amount) { this.battleCredits = Math.max(0, this.battleCredits + amount); }
    public synchronized boolean deductBattleCredits(int amount) {
        if (this.battleCredits < amount) return false;
        this.battleCredits -= amount;
        return true;
    }
    public int getWarCredits() { return warCredits; }
    public synchronized void setWarCredits(int wc) { this.warCredits = Math.max(0, wc); }
    public synchronized void addWarCredits(int amount) { this.warCredits = Math.max(0, this.warCredits + amount); }
    public synchronized boolean deductWarCredits(int amount) {
        if (this.warCredits < amount) return false;
        this.warCredits -= amount;
        return true;
    }
    public int getWins() { return wins; }
    public synchronized void setWins(int wins) { this.wins = Math.max(0, wins); }
    public synchronized void addWin() { this.wins++; }

    public int getRating() { return rating; }
    public synchronized void setRating(int rating) { this.rating = Math.max(0, rating); }

    public Set<String> getUnlockedKits() { return unlockedKits; }
    public boolean isKitUnlocked(String kitName) { return unlockedKits.contains(kitName); }
    public Set<String> getCertifications() { return certifications; }
    public boolean hasCertification(String cert) { return certifications.contains(cert); }
    public Map<String, CompoundTag> getSavedAttachments() { return savedAttachments; }

    public String getCallsign() { return callsign; }
    public synchronized void setCallsign(String callsign) { this.callsign = callsign != null ? callsign : ""; }
    public String getRankPrefix() { return rankPrefix; }
    public synchronized void setRankPrefix(String rankPrefix) { this.rankPrefix = rankPrefix != null ? rankPrefix : ""; }
    public boolean isAdmin() { return isAdmin; }
    public synchronized void setAdmin(boolean admin) { isAdmin = admin; }
    public int getDonatTier() { return donatTier; }
    public synchronized void setDonatTier(int donatTier) { this.donatTier = Math.max(0, Math.min(3, donatTier)); }
    public String getPlayerTitle() { return playerTitle; }
    public synchronized void setPlayerTitle(String playerTitle) { this.playerTitle = playerTitle != null ? playerTitle : ""; }
    public String getLoadoutConfig() { return loadoutConfig; }
    public synchronized void setLoadoutConfig(String loadoutConfig) { this.loadoutConfig = loadoutConfig != null ? loadoutConfig : ""; }
    public String getSelectedKit() { return selectedKit; }
    public synchronized void setSelectedKit(String selectedKit) { this.selectedKit = selectedKit != null ? selectedKit : ""; }

    public String getSelectedRole() { return selectedRole; }
    public synchronized void setSelectedRole(String role) { this.selectedRole = role != null ? role : ""; }
    public String getSelectedLoadout() { return selectedLoadout; }
    public synchronized void setSelectedLoadout(String loadout) { this.selectedLoadout = loadout != null ? loadout : ""; }
    public Set<String> getUnlockedRoles() { return unlockedRoles; }
    public boolean hasRole(String roleId) { return unlockedRoles.contains(roleId); }
    public Set<String> getUnlockedLoadouts() { return unlockedLoadouts; }
    public boolean hasLoadout(String loadoutId) { return unlockedLoadouts.contains(loadoutId); }
    public void unlockKit(String kitName) { unlockedKits.add(kitName); }
    public void lockKit(String kitName) { unlockedKits.remove(kitName); }
    public void grantCertification(String cert) { certifications.add(cert); }
    public void revokeCertification(String cert) { certifications.remove(cert); }
    public void unlockRole(String roleId) { unlockedRoles.add(roleId); }
    public void lockRole(String roleId) { unlockedRoles.remove(roleId); }
    public void unlockLoadout(String loadoutId) { unlockedLoadouts.add(loadoutId); }
    public void lockLoadout(String loadoutId) { unlockedLoadouts.remove(loadoutId); }

    public synchronized void reset() {
        this.team = Team.SPECTATOR;
        this.kills = 0;
        this.deaths = 0;
        this.squadId = -1;
        this.prefix = "";
        this.suffix = "";
        this.displayName = "";
        this.battleCredits = 0;
        this.callsign = "";
        this.rankPrefix = "";
        this.isAdmin = false;
        this.donatTier = 0;
        this.playerTitle = "";
        this.loadoutConfig = "";
        this.selectedKit = "";
        this.selectedRole = "";
        this.selectedLoadout = "";
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
        tag.putInt("WarCredits", warCredits);
        tag.putInt("Wins", wins);
        tag.putInt("Rating", rating);
        tag.putString("Callsign", callsign);
        tag.putString("RankPrefix", rankPrefix);
        tag.putBoolean("IsAdmin", isAdmin);
        tag.putInt("DonatTier", donatTier);
        tag.putString("PlayerTitle", playerTitle);
        tag.putString("LoadoutConfig", loadoutConfig);
        tag.putString("SelectedKit", selectedKit);
        tag.putString("SelectedRole", selectedRole);
        tag.putString("SelectedLoadout", selectedLoadout);
        tag.putBoolean("HasReceivedDogTag", hasReceivedDogTag);
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
        {
            ListTag roleList = new ListTag();
            for (String role : unlockedRoles) {
                roleList.add(StringTag.valueOf(role));
            }
            tag.put("UnlockedRoles", roleList);
        }
        {
            ListTag loadoutList = new ListTag();
            for (String ld : unlockedLoadouts) {
                loadoutList.add(StringTag.valueOf(ld));
            }
            tag.put("UnlockedLoadouts", loadoutList);
        }
        {
            ListTag certList = new ListTag();
            for (String cert : certifications) {
                certList.add(StringTag.valueOf(cert));
            }
            tag.put("Certifications", certList);
        }
        return tag;
    }

    public synchronized void deserializeNBT(CompoundTag tag) {
        this.team = Team.fromOrdinal(tag.getInt("Team"));
        this.kills = tag.getInt("Kills");
        this.deaths = tag.getInt("Deaths");
        this.squadId = tag.getInt("SquadId");
        this.rankOrdinal = tag.getInt("RankOrdinal");
        this.prefix = tag.getString("Prefix");
        this.suffix = tag.getString("Suffix");
        this.displayName = tag.getString("DisplayName");
        this.battleCredits = tag.getInt("BattleCredits");
        this.warCredits = tag.contains("WarCredits") ? tag.getInt("WarCredits") : 0;
        this.callsign = tag.getString("Callsign");
        this.wins = tag.contains("Wins") ? tag.getInt("Wins") : 0;
        this.rating = tag.contains("Rating") ? tag.getInt("Rating") : 1000;
        this.rankPrefix = tag.getString("RankPrefix");
        this.isAdmin = tag.getBoolean("IsAdmin");
        this.donatTier = tag.getInt("DonatTier");
        this.playerTitle = tag.getString("PlayerTitle");
        this.loadoutConfig = tag.contains("LoadoutConfig") ? tag.getString("LoadoutConfig") : "";
        this.selectedKit = tag.contains("SelectedKit") ? tag.getString("SelectedKit") : "";
        this.selectedRole = tag.contains("SelectedRole") ? tag.getString("SelectedRole") : "";
        this.selectedLoadout = tag.contains("SelectedLoadout") ? tag.getString("SelectedLoadout") : "";
        this.hasReceivedDogTag = tag.contains("HasReceivedDogTag") && tag.getBoolean("HasReceivedDogTag");
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
        if (tag.contains("UnlockedRoles")) {
            ListTag roleList = tag.getList("UnlockedRoles", Tag.TAG_STRING);
            unlockedRoles.clear();
            for (Tag base : roleList) {
                unlockedRoles.add(base.getAsString());
            }
        }
        if (tag.contains("UnlockedLoadouts")) {
            ListTag loadoutList = tag.getList("UnlockedLoadouts", Tag.TAG_STRING);
            unlockedLoadouts.clear();
            for (Tag base : loadoutList) {
                unlockedLoadouts.add(base.getAsString());
            }
        }
        if (tag.contains("Certifications")) {
            ListTag certList = tag.getList("Certifications", Tag.TAG_STRING);
            certifications.clear();
            for (Tag base : certList) {
                certifications.add(base.getAsString());
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
        return String.format("PlayerCombatData{team=%s, kills=%d, deaths=%d, kd=%.2f, wins=%d, rating=%d, squad=%d, callsign='%s', rankPrefix='%s', displayName='%s'}",
                team.getName(), kills, deaths, getKDRatio(), wins, rating, squadId, callsign, rankPrefix, displayName);
    }
}
