package com.yourmod.teamsystem.capture;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.EconomyManager;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.ContributionManager;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamSystemConfig;
import com.yourmod.teamsystem.core.TicketManager;
import com.yourmod.teamsystem.network.CapturePointSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
                TeamSystem.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam());
        }

        boolean changed = false;
        double natoOwned = 0;
        double russiaOwned = 0;

        for (CaptureZone zone : zones) {
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

        TicketManager tm = TeamSystem.getTicketManager();
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

    private static void broadcastCapturing(CaptureZone zone, Team team, ServerLevel level) {
        Component msg = Component.literal("§e[Точка] §f" + zone.getName() + " §eзахватывается командой " + team.getName());
        level.players().forEach(p -> p.displayClientMessage(msg, false));
    }

    private static void broadcastNeutralizing(CaptureZone zone, Team team, ServerLevel level) {
        Component msg = Component.literal("§e[Точка] §f" + zone.getName() + " §eнейтрализуется командой " + team.getName());
        level.players().forEach(p -> p.displayClientMessage(msg, false));
    }

    private static void broadcastNeutralized(CaptureZone zone, ServerLevel level) {
        Component msg = Component.literal("§7[Точка] §f" + zone.getName() + " §7нейтрализована");
        level.players().forEach(p -> p.displayClientMessage(msg, false));
    }

    private static void broadcastContested(CaptureZone zone, ServerLevel level) {
        Component msg = Component.literal("§c[Точка] §f" + zone.getName() + " §cЗахват остановлен! Враг рядом!");
        level.players().forEach(p -> p.displayClientMessage(msg, false));
    }

    private static void broadcastCapture(CaptureZone zone, ServerLevel level) {
        Component msg = Component.literal("§6[Точка] §f" + zone.getName() + " §6захвачена командой " + zone.getOwnerTeam().getName());
        level.players().forEach(p -> p.displayClientMessage(msg, false));
        GameManager gm = TeamSystem.getGameManager();
        if (gm == null || !gm.isPlaying()) return;
        ContributionManager cm = TeamSystem.getContributionManager();
        if (cm == null) return;
        for (ServerPlayer p : level.players()) {
            if (!zone.contains(p)) continue;
            Team t = TeamSystem.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam();
            if (t == zone.getOwnerTeam()) {
                cm.addCapture(p.getUUID(), p.getName().getString());
            }
        }
    }

    private static void trackCaptureProgress(CaptureZone zone, Team capturingTeam, int playerCount, double speedMult, ServerLevel level) {
        double perPlayer = BASE_SPEED * speedMult / zone.getCaptureSeconds() / 20.0;
        for (ServerPlayer p : level.players()) {
            if (!zone.contains(p)) continue;
            Team t = TeamSystem.getTeamManager().getOrCreatePlayerData(p.getUUID()).getTeam();
            if (t == capturingTeam) {
                zone.addContribution(p.getUUID(), perPlayer);
            }
        }
    }

    private static void distributeCaptureRewards(CaptureZone zone, ServerLevel level, Team winningTeam) {
        GameManager gm = TeamSystem.getGameManager();
        if (gm == null || !gm.isPlaying()) return;
        TeamSystemConfig cfg = TeamSystem.getConfig();
        if (cfg == null) return;
        EconomyManager econ = TeamSystem.getEconomyManager();
        if (econ == null) return;

        Map<UUID, Double> contribs = zone.getCaptureContributions();
        if (contribs.isEmpty()) return;

        int totalBC = cfg.getCaptureRewardBC();
        int totalSP = cfg.getCaptureRewardSP();
        if (totalBC <= 0 && totalSP <= 0) return;

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(contribs.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double total = 0;
        for (Map.Entry<UUID, Double> e : sorted) total += e.getValue();
        if (total <= 0) return;

        List<Integer> bcAmounts = new ArrayList<>();
        List<Integer> spAmounts = new ArrayList<>();
        int bcSum = 0, spSum = 0;

        for (Map.Entry<UUID, Double> e : sorted) {
            double share = e.getValue() / total;
            int bc = (int) (share * totalBC);
            int sp = (int) (share * totalSP);
            bcAmounts.add(bc);
            spAmounts.add(sp);
            bcSum += bc;
            spSum += sp;
        }

        int bcRem = totalBC - bcSum;
        int spRem = totalSP - spSum;
        if (bcRem > 0 && !bcAmounts.isEmpty()) bcAmounts.set(0, bcAmounts.get(0) + bcRem);
        if (spRem > 0 && !spAmounts.isEmpty()) spAmounts.set(0, spAmounts.get(0) + spRem);

        for (int i = 0; i < sorted.size(); i++) {
            UUID uuid = sorted.get(i).getKey();
            int bc = bcAmounts.get(i);
            int sp = spAmounts.get(i);
            if (bc > 0) econ.addBC(uuid, bc);
            if (sp > 0) econ.addSP(uuid, sp);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null && (bc > 0 || sp > 0)) {
                player.displayClientMessage(
                    Component.literal("§a+" + bc + " BC §7+" + sp + " SP §f(захват точки)"),
                    false
                );
            }
        }
    }

    public static void syncToAll(ServerLevel level, List<CaptureZone> zones) {
        CapturePointSyncPacket packet = buildPacket(zones);
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
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
        }

        return new CapturePointSyncPacket(ids, progress, owners, names, capturing, xs, ys, zs, radii);
    }
}
