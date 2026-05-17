package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.CapturePointSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;
import java.util.stream.Collectors;

public class CapturePointManager {
    private final List<CapturePointData> points;
    private static final double CAPTURE_SPEED_BASE = 1.0;
    private static final double CONTEST_THRESHOLD = 1.0;

    public CapturePointManager() {
        this.points = new ArrayList<>();
    }

    public boolean isCaptureEnabled() {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        return pool.getCurrentMap().map(MapConfig::hasCapturePoints).orElse(true);
    }

    public void clearPoints() {
        points.clear();
    }

    public void addPoint(CapturePointData point) {
        points.add(point);
    }

    public void removePoint(int id) {
        points.removeIf(p -> p.getId() == id);
    }

    public CapturePointData getPoint(int id) {
        return points.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    public List<CapturePointData> getAllPoints() {
        return Collections.unmodifiableList(points);
    }

    public List<CapturePointData> getPointsOwnedBy(Team team) {
        return points.stream()
            .filter(p -> p.getOwnerTeam() == team && p.isCaptured())
            .collect(Collectors.toList());
    }

    public List<CapturePointData> getPointsNotOwnedBy(Team team) {
        return points.stream()
            .filter(p -> p.getOwnerTeam() != team || !p.isCaptured())
            .collect(Collectors.toList());
    }

    public void resetAllPoints() {
        points.forEach(CapturePointData::reset);
    }

    public void tickCapturePoints(ServerLevel level) {
        if (!isCaptureEnabled() || points.isEmpty()) return;

        ResourceLocation currentDim = level.dimension().location();
        ResourceLocation mapDim = getCurrentMapDimension();
        if (mapDim != null && !mapDim.equals(currentDim)) return;

        boolean changed = false;

        for (CapturePointData point : points) {
            List<ServerPlayer> nearby = getPlayersInRadius(level, point.getPos(), point.getCaptureRadius());
            if (nearby.isEmpty()) {
                if (point.getState() == CapturePointData.CaptureState.CAPTURING ||
                    point.getState() == CapturePointData.CaptureState.CONTESTED) {
                    if (point.getProgress() > 0) {
                        point.addProgress(-CAPTURE_SPEED_BASE * 0.5);
                    }
                    if (point.getProgress() <= 0) {
                        point.setState(CapturePointData.CaptureState.NEUTRAL);
                        point.setCapturingTeam(Team.SPECTATOR);
                    }
                    changed = true;
                }
                continue;
            }

            Map<Team, Integer> teamCounts = new HashMap<>();
            for (ServerPlayer p : nearby) {
                Team t = TeamSystem.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam();
                if (t.isPlayable()) teamCounts.merge(t, 1, Integer::sum);
            }

            if (teamCounts.isEmpty()) continue;

            Team dominantTeam = teamCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get().getKey();
            int dominantCount = teamCounts.get(dominantTeam);

            boolean contested = teamCounts.size() > 1;
            boolean hasOtherTeam = teamCounts.entrySet().stream()
                .anyMatch(e -> e.getKey() != dominantTeam && e.getValue() > 0);

            if (point.getState() == CapturePointData.CaptureState.CAPTURED && point.getOwnerTeam() == dominantTeam) {
                continue;
            }

            if (contested && hasOtherTeam) {
                point.setState(CapturePointData.CaptureState.CONTESTED);
                changed = true;
                continue;
            }

            if (point.getOwnerTeam() == dominantTeam && point.isCaptured()) {
                continue;
            }

            double captureSpeed = point.getCaptureSpeed() * dominantCount;
            point.setState(CapturePointData.CaptureState.CAPTURING);
            point.setCapturingTeam(dominantTeam);
            point.addProgress(captureSpeed);
            changed = true;

            if (point.getProgress() >= 100.0) {
                point.setState(CapturePointData.CaptureState.CAPTURED);
                point.setOwnerTeam(dominantTeam);
                point.setCapturingTeam(Team.SPECTATOR);
                point.setProgress(100.0);
                onPointCaptured(point, level);
            }
        }

        if (changed) {
            updateBleedRates();
            syncToAll();
        }
    }

    private void onPointCaptured(CapturePointData point, ServerLevel level) {
        TeamSystem.LOGGER.info("Capture point '{}' captured by {}", point.getName(), point.getOwnerTeam().getName());
        for (ServerPlayer p : level.players()) {
            p.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    "§6[CAPTURED] " + point.getName() + " captured by " + point.getOwnerTeam().getName()),
                false);
        }
        ContributionManager cm = TeamSystem.getContributionManager();
        if (cm != null) {
            for (ServerPlayer p : getPlayersInRadius(level, point.getPos(), point.getCaptureRadius() * 2)) {
                Team t = TeamSystem.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam();
                if (t == point.getOwnerTeam()) {
                    cm.addCapture(p.getUUID(), p.getName().getString());
                }
            }
        }
    }

    private List<ServerPlayer> getPlayersInRadius(ServerLevel level, BlockPos center, double radius) {
        double radiusSq = radius * radius;
        return level.players().stream()
            .filter(p -> p.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) <= radiusSq)
            .collect(Collectors.toList());
    }

    private void updateBleedRates() {
        TicketManager tm = TeamSystem.getTicketManager();
        if (tm == null) return;

        int natoPoints = getPointsOwnedBy(Team.NATO).size();
        int russiaPoints = getPointsOwnedBy(Team.RUSSIA).size();

        int natoBleed = Math.max(0, russiaPoints - natoPoints);
        int russiaBleed = Math.max(0, natoPoints - russiaPoints);

        tm.setBleedRate(Team.NATO, natoBleed);
        tm.setBleedRate(Team.RUSSIA, russiaBleed);
    }

    private ResourceLocation getCurrentMapDimension() {
        MapConfig map = TeamSystem.getMapPoolManager().getCurrentMap().orElse(null);
        if (map != null && map.getWorldFolder() != null && !map.getWorldFolder().isEmpty()) {
            return new ResourceLocation("teamsystem", map.getWorldFolder());
        }
        return null;
    }

    public void syncToAll() {
        List<Integer> ids = new ArrayList<>();
        List<Double> progress = new ArrayList<>();
        List<Integer> owners = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (CapturePointData p : points) {
            ids.add(p.getId());
            progress.add(p.getProgress());
            owners.add(p.getOwnerTeam().ordinal());
            names.add(p.getName());
        }

        CapturePointSyncPacket packet = new CapturePointSyncPacket(ids, progress, owners, names);
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
