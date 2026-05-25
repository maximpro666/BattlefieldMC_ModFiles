package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.SquadSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SquadManager {
    private Map<Integer, Squad> squads = new ConcurrentHashMap<>();
    private int nextSquadId = 0;
    private Map<UUID, Set<Invitation>> invitations = new ConcurrentHashMap<>();
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
        PWP.LOGGER.info("Squad created: {} (id: {})", name, squad.getSquadId());
        return squad;
    }

    public void disbandSquad(int squadId) {
        Squad squad = squads.remove(squadId);
        if (squad != null) {
            PWP.LOGGER.info("Squad disbanded: {}", squad.getName());
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
        Set<Invitation> invites = invitations.computeIfAbsent(invitedUUID, k -> ConcurrentHashMap.newKeySet());
        invites.add(new Invitation(squad.getLeaderUUID(), squad.getSquadId()));
    }

    public Invitation getInvitation(UUID playerUUID, int squadId) {
        Set<Invitation> invites = invitations.get(playerUUID);
        if (invites == null) return null;
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
        Set<Invitation> invites = invitations.get(playerUUID);
        if (invites != null) {
            invites.removeIf(inv -> inv.squadId == squadId);
        }
    }

    public void leaveSquad(UUID playerId) {
        Squad squad = getPlayerSquad(playerId);
        if (squad != null) {
            boolean wasLeader = squad.isLeader(playerId);
            squad.removeMember(playerId);
            if (squad.getMemberCount() == 0) {
                disbandSquad(squad.getSquadId());
            } else if (wasLeader) {
                // Transfer leadership to the next member
                squad.getMembers().stream().findFirst().ifPresent(squad::setLeaderUUID);
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
        if (playerUUID.equals(newLeaderUUID)) return;
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

    public void removePlayer(UUID playerId) {
        leaveSquad(playerId);
        invitations.remove(playerId);
        for (Set<Invitation> invites : invitations.values()) {
            invites.removeIf(inv -> inv.inviterUUID.equals(playerId));
        }
        invitations.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void syncToPlayer(ServerPlayer player) {
        UUID puid = player.getUUID();
        Squad ps = getPlayerSquad(puid);
        int playerSquadId = ps != null ? ps.getSquadId() : -1;
        String playerSquadName = ps != null ? ps.getName() : "";

        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SquadSyncPacket(playerSquadId, playerSquadName)
        );
    }

    public void syncToAll(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }
}
