package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ContributionManager {
    private final Map<UUID, ContributionData> contributions = new ConcurrentHashMap<>();

    public ContributionManager() {
    }

    public ContributionData getOrCreate(UUID playerUUID, String playerName) {
        return contributions.computeIfAbsent(playerUUID, id -> new ContributionData(id, playerName));
    }

    public void addKill(UUID playerUUID, String playerName) {
        getOrCreate(playerUUID, playerName).addKill();
    }

    public void addAssist(UUID playerUUID, String playerName) {
        getOrCreate(playerUUID, playerName).addAssist();
    }

    public void addDeath(UUID playerUUID, String playerName) {
        getOrCreate(playerUUID, playerName).addDeath();
    }

    public void addCapture(UUID playerUUID, String playerName) {
        getOrCreate(playerUUID, playerName).addCapture();
    }

    public void addDefense(UUID playerUUID, String playerName) {
        getOrCreate(playerUUID, playerName).addDefense();
    }

    public void addRevive(UUID playerUUID, String playerName) {
        getOrCreate(playerUUID, playerName).addRevive();
    }

    public void addSP(UUID playerUUID, String playerName, int amount) {
        getOrCreate(playerUUID, playerName).addSP(amount);
    }

    public ContributionData get(UUID playerUUID) {
        return contributions.get(playerUUID);
    }

    public List<ContributionData> getTopPlayers(int limit) {
        return contributions.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<ContributionData> getAll() {
        return new ArrayList<>(contributions.values());
    }

    public Map<Team, Integer> getTeamScores() {
        Map<Team, Integer> scores = new HashMap<>();
        for (ContributionData cd : contributions.values()) {
            Team team = PWP.getTeamManager().getOrCreatePlayerData(cd.getPlayerUUID()).getTeam();
            scores.merge(team, cd.getTotalScore(), Integer::sum);
        }
        return scores;
    }

    public void resetMatch() {
        contributions.clear();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ContributionData cd : contributions.values()) {
            list.add(cd.serializeNBT());
        }
        tag.put("Contributions", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        contributions.clear();
        ListTag list = tag.getList("Contributions", Tag.TAG_COMPOUND);
        for (Tag base : list) {
            ContributionData cd = ContributionData.fromNBT((CompoundTag) base);
            contributions.put(cd.getPlayerUUID(), cd);
        }
    }
}
