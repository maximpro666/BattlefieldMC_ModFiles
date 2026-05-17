package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class SquadManager {
    private Map<Integer, Squad> squads = new HashMap<>();
    private int nextSquadId = 0;
    private Map<UUID, Set<Invitation>> invitations = new HashMap<>();
    private static final long INVITE_TIMEOUT_MS = 30000;

    public static class Invitation {
        public UUID inviterUUID;
        public int squadId;
        public long createdTime;

        public Invitation(UUID inviterUUID, int squadId) {
            this.inviterUUID = inviterUUID;
            this.squadId = squadId;
            this.createdTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdTime > INVITE_TIMEOUT_MS;
        }
    }

    public Squad createSquad(String name, Team team, UUID leaderUUID) {
        Squad squad = new Squad(nextSquadId++, name, team, leaderUUID);
        squads.put(squad.getSquadId(), squad);
        TeamSystem.LOGGER.info("Squad created: {} (id: {})", name, squad.getSquadId());
        return squad;
    }

    public void disbandSquad(int squadId) {
        Squad squad = squads.remove(squadId);
        if (squad != null) {
            TeamSystem.LOGGER.info("Squad disbanded: {}", squad.getName());
        }
    }

    public Squad getSquadById(int squadId) {
        return squads.get(squadId);
    }

    public Squad getPlayerSquad(UUID playerId) {
        for (Squad squad : squads.values()) {
            if (squad.hasMember(playerId)) {
                return squad;
            }
        }
        return null;
    }

    public List<Squad> getTeamSquads(Team team) {
        List<Squad> teamSquads = new ArrayList<>();
        for (Squad squad : squads.values()) {
            if (squad.getTeam() == team) {
                teamSquads.add(squad);
            }
        }
        return teamSquads;
    }

    public List<ServerPlayer> getSquadMembersOnline(int squadId, net.minecraft.server.MinecraftServer server) {
        Squad squad = getSquadById(squadId);
        if (squad == null) return new ArrayList<>();

        List<ServerPlayer> online = new ArrayList<>();
        for (UUID memberId : squad.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }

    public void invitePlayer(UUID invitedUUID, Squad squad) {
        Set<Invitation> invites = invitations.computeIfAbsent(invitedUUID, k -> new HashSet<>());
        invites.add(new Invitation(squad.getLeaderUUID(), squad.getSquadId()));
    }

    public Invitation getInvitation(UUID playerUUID, int squadId) {
        Set<Invitation> invites = invitations.getOrDefault(playerUUID, new HashSet<>());
        for (Invitation inv : invites) {
            if (inv.squadId == squadId && !inv.isExpired()) {
                return inv;
            }
        }
        return null;
    }

    public void acceptInvitation(UUID playerUUID, int squadId) {
        Squad squad = getSquadById(squadId);
        if (squad != null && !squad.isFull()) {
            squad.addMember(playerUUID);
            removeInvitation(playerUUID, squadId);
        }
    }

    public void removeInvitation(UUID playerUUID, int squadId) {
        Set<Invitation> invites = invitations.getOrDefault(playerUUID, new HashSet<>());
        invites.removeIf(inv -> inv.squadId == squadId);
    }

    public void leaveSquad(UUID playerId) {
        Squad squad = getPlayerSquad(playerId);
        if (squad != null) {
            squad.removeMember(playerId);
            if (squad.getMemberCount() == 0) {
                disbandSquad(squad.getSquadId());
            }
        }
    }

    public void kickMember(UUID playerUUID, UUID targetUUID) {
        Squad squad = getPlayerSquad(playerUUID);
        if (squad != null && squad.isLeader(playerUUID)) {
            squad.removeMember(targetUUID);
            if (squad.getMemberCount() == 0) {
                disbandSquad(squad.getSquadId());
            }
        }
    }

    public void promoteLeader(UUID playerUUID, UUID newLeaderUUID) {
        Squad squad = getPlayerSquad(playerUUID);
        if (squad != null && squad.isLeader(playerUUID)) {
            squad.setLeaderUUID(newLeaderUUID);
        }
    }

    public Collection<Squad> getAllSquads() {
        return squads.values();
    }

    public void clearExpiredInvitations() {
        invitations.forEach((uuid, invites) -> invites.removeIf(Invitation::isExpired));
        invitations.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
