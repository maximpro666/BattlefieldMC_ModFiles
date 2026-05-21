package com.pigeostudios.pwp.capture;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.ContributionManager;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.PWPConfig;
import com.pigeostudios.pwp.core.TicketManager;
import com.pigeostudios.pwp.network.CapturePointSyncPacket;
import com.pigeostudios.pwp.network.NotificationPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class CaptureProcessor {
    private static final double BASE_SPEED = 4.0;
    private static final double MAIN_MULTIPLIER = 1.5;
    private static final double DECAY_SPEED = 0.003;

    public static void tick(ServerLevel level, ZoneDataManager data) {
        if (!data.isActive()) return;
        String dimId = level.dimension().location().toString();
        List<CaptureZone> zones = data.getZones(dimId);
        if (zones.isEmpty()) return;

        // Pre-cache team data once per tick to avoid repeated NBT lookups
        Map<UUID, Team> teamCache = new HashMap<>();
        for (ServerPlayer p : level.players()) {
            teamCache.put(p.getUUID(),
                PWP.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam());
        }

        boolean changed = false;
        double natoOwned = 0;
        double russiaOwned = 0;

        for (CaptureZone zone : zones) {
            zone.setContested(false);
            Team prevCapturing = zone.getCapturingTeam();
            Map<Team, Integer> counts = countPlayersInZone(level, zone, teamCache);
            if (counts.isEmpty()) {
                if (zone.getOwnerTeam().isPlayable() && zone.getProgress() < 1.0f) {
                    zone.addProgress((float) -DECAY_SPEED);
                    changed = true;
                    if (zone.getProgress() <= 0) {
                        zone.setProgress(0);
                        zone.setOwnerTeam(Team.SPECTATOR);
                        broadcastNeutralized(zone, level);
                    }
                } else if (zone.getOwnerTeam() == Team.SPECTATOR && zone.getProgress() > 0) {
                    zone.addProgress((float) -DECAY_SPEED);
                    zone.setCapturingTeam(Team.SPECTATOR);
                    changed = true;
                    if (zone.getProgress() <= 0) {
                        zone.clearCaptureContributions();
                    }
                }
                double weight = zone.isMain() ? MAIN_MULTIPLIER : 1.0;
                if (zone.getOwnerTeam() == Team.NATO) natoOwned += weight;
                else if (zone.getOwnerTeam() == Team.RUSSIA) russiaOwned += weight;
                continue;
            }

            Team dominant = null;
            int dominantCount = 0;
            boolean contested = false;

            for (Map.Entry<Team, Integer> e : counts.entrySet()) {
                if (e.getValue() > dominantCount) {
                    dominant = e.getKey();
                    dominantCount = e.getValue();
                }
            }
            for (Map.Entry<Team, Integer> e : counts.entrySet()) {
                if (e.getKey() != dominant && e.getValue() > 0) {
                    contested = true;
                    break;
                }
            }

            if (contested) {
                zone.setContested(true);
                zone.setCapturingTeam(Team.SPECTATOR);
                changed = true;
                if (prevCapturing.isPlayable() && prevCapturing != zone.getOwnerTeam()) {
                    broadcastContested(zone, level);
                }
                double weight = zone.isMain() ? MAIN_MULTIPLIER : 1.0;
                if (zone.getOwnerTeam() == Team.NATO) natoOwned += weight;
                else if (zone.getOwnerTeam() == Team.RUSSIA) russiaOwned += weight;
                continue;
            }

            if (zone.getOwnerTeam() == dominant) {
                if (zone.getProgress() < 1.0f) {
                    zone.setCapturingTeam(dominant);
                    double speedMult = zone.isMain() ? MAIN_MULTIPLIER : 1.0;
                    zone.addProgress((float) (BASE_SPEED * speedMult * dominantCount / zone.getCaptureSeconds() / 20.0));
                    changed = true;
                    if (prevCapturing != dominant) broadcastCapturing(zone, dominant, level);
                    if (zone.getProgress() >= 1.0f) {
                        zone.setProgress(1.0f);
                        zone.setOwnerTeam(dominant);
                        zone.setCapturingTeam(Team.SPECTATOR);
                        broadcastCapture(zone, level);
                    }
                }
                double weight = zone.isMain() ? MAIN_MULTIPLIER : 1.0;
                if (zone.getOwnerTeam() == Team.NATO) natoOwned += weight;
                else if (zone.getOwnerTeam() == Team.RUSSIA) russiaOwned += weight;
                continue;
            }

            double speedMult = zone.isMain() ? MAIN_MULTIPLIER : 1.0;

            if (zone.getOwnerTeam().isPlayable()) {
                zone.setCapturingTeam(dominant);
                float neutralize = (float) (BASE_SPEED * speedMult * dominantCount / zone.getCaptureSeconds() / 20.0);
                trackCaptureProgress(zone, dominant, dominantCount, speedMult, level);
                zone.addProgress(-neutralize);
                changed = true;
                if (prevCapturing != dominant) broadcastNeutralizing(zone, dominant, level);
                if (zone.getProgress() <= 0) {
                    zone.setProgress(0);
                    zone.setOwnerTeam(Team.SPECTATOR);
                    zone.setCapturingTeam(Team.SPECTATOR);
                    zone.clearCaptureContributions();
                    broadcastNeutralized(zone, level);
                }
                continue;
            }

            zone.setCapturingTeam(dominant);
            float capSpeed = (float) (BASE_SPEED * speedMult * dominantCount / zone.getCaptureSeconds() / 20.0);
            trackCaptureProgress(zone, dominant, dominantCount, speedMult, level);
            zone.addProgress(capSpeed);
            changed = true;
            if (prevCapturing != dominant) broadcastCapturing(zone, dominant, level);
            if (zone.getProgress() >= 1.0f) {
                zone.setProgress(1.0f);
                zone.setOwnerTeam(dominant);
                zone.setCapturingTeam(Team.SPECTATOR);
                distributeCaptureRewards(zone, level, dominant);
                zone.clearCaptureContributions();
                broadcastCapture(zone, level);
            }
        }

        TicketManager tm = PWP.getTicketManager();
        if (tm != null) {
            tm.setBleedRate(Team.NATO, (int) Math.round(Math.max(0, russiaOwned - natoOwned)));
            tm.setBleedRate(Team.RUSSIA, (int) Math.round(Math.max(0, natoOwned - russiaOwned)));
        }

        if (changed) syncToAll(level, zones);
    }

    private static Map<Team, Integer> countPlayersInZone(ServerLevel level, CaptureZone zone, Map<UUID, Team> teamCache) {
        Map<Team, Integer> counts = new HashMap<>();
        for (ServerPlayer player : level.players()) {
            if (!zone.contains(player)) continue;
            Team team = teamCache.get(player.getUUID());
            if (team != null && team.isPlayable()) counts.merge(team, 1, Integer::sum);
        }
        return counts;
    }

    private static void notifyAll(ServerLevel level, String text, String type, int durationMs) {
        NotificationPacket pkt = new NotificationPacket(text, type, durationMs, "");
        for (net.minecraft.world.entity.player.Player p : level.players()) {
            if (p instanceof ServerPlayer sp) {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), pkt);
            }
        }
    }

    private static void broadcastCapturing(CaptureZone zone, Team team, ServerLevel level) {
        String type = team == Team.NATO ? "capture_nato" : "capture_russia";
        notifyAll(level, "\u25C9 " + zone.getName() + " \u0437\u0430\u0445\u0432\u0430\u0442\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 " + team.getName(), type, 3000);
    }

    private static void broadcastNeutralizing(CaptureZone zone, Team team, ServerLevel level) {
        String type = team == Team.NATO ? "capture_nato" : "capture_russia";
        notifyAll(level, "\u25C9 " + zone.getName() + " \u043d\u0435\u0439\u0442\u0440\u0430\u043b\u0438\u0437\u0443\u0435\u0442\u0441\u044f \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 " + team.getName(), type, 3000);
    }

    private static void broadcastNeutralized(CaptureZone zone, ServerLevel level) {
        notifyAll(level, "\u25CB " + zone.getName() + " \u043d\u0435\u0439\u0442\u0440\u0430\u043b\u0438\u0437\u043e\u0432\u0430\u043d\u0430", "capture_neutral", 3000);
    }

    private static void broadcastContested(CaptureZone zone, ServerLevel level) {
        notifyAll(level, "\u26A0 " + zone.getName() + " \u043e\u0441\u043f\u0430\u0440\u0438\u0432\u0430\u0435\u0442\u0441\u044f!", "warning", 3000);
    }

    private static void broadcastCapture(CaptureZone zone, ServerLevel level) {
        Team owner = zone.getOwnerTeam();
        String type = owner == Team.NATO ? "capture_nato" : "capture_russia";
        notifyAll(level, "\u25C9 " + zone.getName() + " \u0437\u0430\u0445\u0432\u0430\u0447\u0435\u043d\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 " + owner.getName(), type, 4000);
        GameManager gm = PWP.getGameManager();
        if (gm == null || !gm.isPlaying()) return;
        ContributionManager cm = PWP.getContributionManager();
        if (cm == null) return;
        for (ServerPlayer p : level.players()) {
            if (!zone.contains(p)) continue;
            Team t = PWP.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam();
            if (t == zone.getOwnerTeam()) {
                cm.addCapture(p.getUUID(), p.getName().getString());
            }
        }
    }

    private static void trackCaptureProgress(CaptureZone zone, Team capturingTeam, int playerCount, double speedMult, ServerLevel level) {
        double perPlayer = BASE_SPEED * speedMult / zone.getCaptureSeconds() / 20.0;
        for (ServerPlayer p : level.players()) {
            if (!zone.contains(p)) continue;
            Team t = PWP.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam();
            if (t == capturingTeam) {
                zone.addContribution(p.getUUID(), perPlayer);
            }
        }
    }

    private static void distributeCaptureRewards(CaptureZone zone, ServerLevel level, Team winningTeam) {
        GameManager gm = PWP.getGameManager();
        if (gm == null || !gm.isPlaying()) return;
        PWPConfig cfg = PWP.getConfig();
        if (cfg == null) return;
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();

        Map<UUID, Double> contribs = zone.getCaptureContributions();
        if (contribs.isEmpty()) return;

        int totalBC = cfg.getCaptureRewardBC();
        if (totalBC <= 0) return;

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(contribs.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double total = 0;
        for (Map.Entry<UUID, Double> e : sorted) total += e.getValue();
        if (total <= 0) return;

        for (Map.Entry<UUID, Double> e : sorted) {
            UUID uuid = e.getKey();
            runtime.addBC(uuid, totalBC);
            runtime.addActivity(uuid, BattlefieldRuntime.SCORE_CAPTURE);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                runtime.syncBC(player);
                NotificationPacket rewardPkt = new NotificationPacket("+" + totalBC + " BC \u0437\u0430 \u0437\u0430\u0445\u0432\u0430\u0442", "success", 3000, "");
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), rewardPkt);
            }
        }
    }

    public static void syncToAll(ServerLevel level, List<CaptureZone> zones) {
        CapturePointSyncPacket packet = buildPacket(zones);
        for (ServerPlayer player : PWP.getGameManager().getServer().getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void syncToPlayer(ServerPlayer player, List<CaptureZone> zones) {
        CapturePointSyncPacket packet = buildPacket(zones);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static CapturePointSyncPacket buildPacket(List<CaptureZone> zones) {
        List<Integer> ids = new ArrayList<>();
        List<Double> progress = new ArrayList<>();
        List<Integer> owners = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Integer> capturing = new ArrayList<>();
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        List<Double> zs = new ArrayList<>();
        List<Double> radii = new ArrayList<>();
        List<String> types = new ArrayList<>();

        for (CaptureZone z : zones) {
            ids.add(z.getId().hashCode());
            progress.add((double) z.getProgress());
            owners.add(z.getOwnerTeam().ordinal());
            names.add(z.getName());
            capturing.add(z.getCapturingTeam().ordinal());
            BlockPos center = z.getCenter();
            xs.add((double) center.getX());
            ys.add((double) center.getY());
            zs.add((double) center.getZ());
            double half = (z.getMax().getX() - z.getMin().getX()) / 2.0;
            radii.add(half);
            types.add(z.getPointType());
        }

        return new CapturePointSyncPacket(ids, progress, owners, names, capturing, xs, ys, zs, radii, types);
    }
}
