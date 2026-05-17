package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.MarkerSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

public class MarkerManager {
    private final List<MarkerData> markers = new ArrayList<>();

    public MarkerData addMarker(String name, String label, ResourceLocation dimension,
                                double x, double y, double z,
                                int teamOrdinal, MarkerData.MarkerType type, UUID creatorUUID) {
        MarkerData marker = new MarkerData(name, label, dimension, x, y, z, teamOrdinal, type, creatorUUID);
        markers.add(marker);
        syncToAll();
        TeamSystem.LOGGER.info("Marker added: {} ({}) at ({}, {}, {})", name, label, x, y, z);
        return marker;
    }

    public MarkerData createPing(String label, ResourceLocation dimension,
                                 double x, double y, double z,
                                 int teamOrdinal, UUID creatorUUID) {
        String pingName = "ping_" + creatorUUID.toString() + "_" + System.currentTimeMillis();
        MarkerData ping = new MarkerData(pingName, label, dimension, x, y, z,
            teamOrdinal, MarkerData.MarkerType.POINT, creatorUUID);
        ping.setPing(true);
        ping.setExpiryTime(System.currentTimeMillis() + 5000);
        markers.add(ping);
        syncToAll();
        return ping;
    }

    public boolean removeMarker(String name) {
        boolean removed = markers.removeIf(m -> m.getName().equals(name));
        if (removed) {
            syncToAll();
            TeamSystem.LOGGER.info("Marker removed: {}", name);
        }
        return removed;
    }

    public void clearMarkers() {
        markers.clear();
        syncToAll();
        TeamSystem.LOGGER.info("All markers cleared");
    }

    public List<MarkerData> getMarkers() {
        return Collections.unmodifiableList(markers);
    }

    public MarkerData getMarker(String name) {
        for (MarkerData m : markers) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    public List<MarkerData> getMarkersVisibleToTeam(int teamOrdinal) {
        if (teamOrdinal == Team.SPECTATOR.ordinal()) {
            return getMarkers();
        }
        return markers.stream()
            .filter(m -> m.getTeamOrdinal() < 0 || m.getTeamOrdinal() == teamOrdinal)
            .collect(Collectors.toList());
    }

    public List<MarkerData> getMarkersForPlayer(ServerPlayer player) {
        Team team = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
        return getMarkersVisibleToTeam(team.ordinal());
    }

    public void syncToPlayer(ServerPlayer player) {
        List<MarkerData> visible = getMarkersForPlayer(player);
        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new MarkerSyncPacket(visible)
        );
    }

    public void autoExpiryTick() {
        long now = System.currentTimeMillis();
        boolean removed = markers.removeIf(m -> m.isExpired(now));
        if (removed) syncToAll();
    }

    public void syncToAll() {
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }
}
