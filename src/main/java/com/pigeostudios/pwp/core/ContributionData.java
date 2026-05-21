package com.pigeostudios.pwp.core;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class ContributionData {
    private final UUID playerUUID;
    private final String playerName;
    private int kills;
    private int assists;
    private int deaths;
    private int captures;
    private int defenses;
    private int revives;
    private int sp;

    public ContributionData(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public int getKills() { return kills; }
    public int getAssists() { return assists; }
    public int getDeaths() { return deaths; }
    public int getCaptures() { return captures; }
    public int getDefenses() { return defenses; }
    public int getRevives() { return revives; }
    public int getSP() { return sp; }

    public void addKill() { this.kills++; }
    public void addAssist() { this.assists++; }
    public void addDeath() { this.deaths++; }
    public void addCapture() { this.captures++; }
    public void addDefense() { this.defenses++; }
    public void addRevive() { this.revives++; }
    public void addSP(int amount) { this.sp = Math.max(0, this.sp + amount); }
    public void setSP(int sp) { this.sp = Math.max(0, sp); }

    public int getTotalScore() {
        return kills * 10 + assists * 5 + captures * 20 + defenses * 15 + revives * 15 - deaths * 5 + sp;
    }

    public void reset() {
        this.kills = 0;
        this.assists = 0;
        this.deaths = 0;
        this.captures = 0;
        this.defenses = 0;
        this.revives = 0;
        this.sp = 0;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PlayerUUID", playerUUID);
        tag.putString("PlayerName", playerName);
        tag.putInt("Kills", kills);
        tag.putInt("Assists", assists);
        tag.putInt("Deaths", deaths);
        tag.putInt("Captures", captures);
        tag.putInt("Defenses", defenses);
        tag.putInt("Revives", revives);
        tag.putInt("SP", sp);
        return tag;
    }

    public static ContributionData fromNBT(CompoundTag tag) {
        ContributionData data = new ContributionData(
            tag.getUUID("PlayerUUID"),
            tag.getString("PlayerName")
        );
        data.kills = tag.getInt("Kills");
        data.assists = tag.getInt("Assists");
        data.deaths = tag.getInt("Deaths");
        data.captures = tag.getInt("Captures");
        data.defenses = tag.getInt("Defenses");
        data.revives = tag.getInt("Revives");
        data.sp = tag.getInt("SP");
        return data;
    }
}
