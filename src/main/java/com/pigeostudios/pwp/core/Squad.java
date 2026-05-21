package com.pigeostudios.pwp.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class Squad {
    private int squadId;
    private String name;
    private Team team;
    private UUID leaderUUID;
    private List<UUID> members;
    private static final int MAX_MEMBERS = 6;

    public Squad(int squadId, String name, Team team, UUID leaderUUID) {
        this.squadId = squadId;
        this.name = name;
        this.team = team;
        this.leaderUUID = leaderUUID;
        this.members = new ArrayList<>();
        this.members.add(leaderUUID);
    }

    public int getSquadId() {
        return squadId;
    }

    public String getName() {
        return name;
    }

    public Team getTeam() {
        return team;
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public void setLeaderUUID(UUID newLeader) {
        if (members.contains(newLeader)) {
            this.leaderUUID = newLeader;
        }
    }

    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isLeader(UUID playerId) {
        return leaderUUID.equals(playerId);
    }

    public void addMember(UUID playerId) {
        if (!members.contains(playerId) && !isFull()) {
            members.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
        if (leaderUUID.equals(playerId) && !members.isEmpty()) {
            leaderUUID = members.get(0);
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("SquadId", squadId);
        tag.putString("Name", name);
        tag.putInt("Team", team.ordinal());
        tag.putUUID("LeaderUUID", leaderUUID);

        ListTag membersList = new ListTag();
        for (UUID uuid : members) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("UUID", uuid);
            membersList.add(uuidTag);
        }
        tag.put("Members", membersList);

        return tag;
    }

    public static Squad deserializeNBT(CompoundTag tag) {
        int squadId = tag.getInt("SquadId");
        String name = tag.getString("Name");
        Team team = Team.fromOrdinal(tag.getInt("Team"));
        UUID leaderUUID = tag.getUUID("LeaderUUID");

        Squad squad = new Squad(squadId, name, team, leaderUUID);
        squad.members.clear();

        ListTag membersList = tag.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < membersList.size(); i++) {
            CompoundTag uuidTag = membersList.getCompound(i);
            squad.members.add(uuidTag.getUUID("UUID"));
        }

        return squad;
    }
}
