package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.BCSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.SPSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

public class EconomyManager {
    private final Map<UUID, Integer> battleCredits;
    private final Map<UUID, Integer> scorePoints;

    public EconomyManager() {
        this.battleCredits = new HashMap<>();
        this.scorePoints = new HashMap<>();
    }

    public int getBC(UUID playerUUID) {
        return battleCredits.getOrDefault(playerUUID, 0);
    }

    public void setBC(UUID playerUUID, int amount) {
        battleCredits.put(playerUUID, Math.max(0, amount));
    }

    public void addBC(UUID playerUUID, int amount) {
        setBC(playerUUID, getBC(playerUUID) + Math.max(0, amount));
    }

    public boolean deductBC(UUID playerUUID, int amount) {
        int current = getBC(playerUUID);
        if (current < amount) return false;
        setBC(playerUUID, current - amount);
        return true;
    }

    public int getSP(UUID playerUUID) {
        return scorePoints.getOrDefault(playerUUID, 0);
    }

    public void setSP(UUID playerUUID, int amount) {
        scorePoints.put(playerUUID, Math.max(0, amount));
    }

    public void addSP(UUID playerUUID, int amount) {
        setSP(playerUUID, getSP(playerUUID) + Math.max(0, amount));
    }

    public void resetSP() {
        scorePoints.clear();
    }

    public void resetAllSPForMatch() {
        scorePoints.clear();
    }

    public void syncBC(ServerPlayer player) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new BCSyncPacket(getBC(player.getUUID())));
    }

    public void syncSP(ServerPlayer player) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new SPSyncPacket(getSP(player.getUUID())));
    }

    public void syncAll(ServerPlayer player) {
        syncBC(player);
        syncSP(player);
    }

    public void syncBCToAll() {
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
            syncBC(player);
        }
    }

    public void syncSPToAll() {
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
            syncSP(player);
        }
    }

    public void loadFromTeamManager() {
        TeamManager tm = TeamSystem.getTeamManager();
        if (tm == null) return;
        battleCredits.clear();
        for (Map.Entry<UUID, PlayerCombatData> entry : tm.getPlayerDataCopy().entrySet()) {
            int bc = entry.getValue().getBattleCredits();
            if (bc > 0) {
                battleCredits.put(entry.getKey(), bc);
            }
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag bcList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : battleCredits.entrySet()) {
            if (entry.getValue() <= 0) continue;
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("UUID", entry.getKey());
            entryTag.putInt("BC", entry.getValue());
            bcList.add(entryTag);
        }
        tag.put("BattleCredits", bcList);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        battleCredits.clear();
        ListTag bcList = tag.getList("BattleCredits", Tag.TAG_COMPOUND);
        for (Tag base : bcList) {
            CompoundTag entryTag = (CompoundTag) base;
            battleCredits.put(entryTag.getUUID("UUID"), entryTag.getInt("BC"));
        }
    }
}
