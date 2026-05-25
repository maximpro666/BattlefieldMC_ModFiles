package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.capture.CaptureZone;
import com.pigeostudios.pwp.capture.ZoneDataManager;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.network.BCSyncPacket;
import com.pigeostudios.pwp.network.NotificationPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.VehicleCreditsSyncPacket;
import com.pigeostudios.pwp.network.WCSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyService {
    private final ConfigService config;
    private final PersistenceService persistence;

    // Thread-safe storage: BC (per-match), WC (persistent)
    private final ConcurrentHashMap<UUID, Integer> bc = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> wc = new ConcurrentHashMap<>();

    // VC: team-shared, per-match. Indexed by Team.ordinal()
    private final int[] vc = new int[2];
    private int vcMatchTotal; // track total VC generated this match for cap

    // Tick counters
    private int frontlineTickCounter;
    private int vcTickCounter;

    // D10: daily bonus tracking
    private final ConcurrentHashMap<UUID, String> lastDailyBonusDate = new ConcurrentHashMap<>();
    private static final int DAILY_LOGIN_BONUS = 10;

    private static final int FRONTLINE_RADIUS = 30;
    private static final int VC_INTERVAL_TICKS = 1200; // 60s

    public EconomyService(ConfigService config, PersistenceService persistence) {
        this.config = config;
        this.persistence = persistence;
    }

    // ════════════════════════════════════════
    // BC operations
    // ════════════════════════════════════════

    public int getBC(UUID uuid) {
        return bc.getOrDefault(uuid, 0);
    }

    public void setBC(UUID uuid, int amount) {
        int capped = Math.max(0, Math.min(amount, config.getBCCap()));
        bc.put(uuid, capped);
        persistence.saveBC(uuid, capped);
    }

    public boolean addBC(UUID uuid, int amount) {
        if (amount <= 0) return true;
        int cap = config.getBCCap();
        Integer[] result = new Integer[1];
        bc.compute(uuid, (k, v) -> {
            int current = v == null ? 0 : v;
            int newVal = Math.min(cap, current + amount);
            result[0] = newVal;
            return newVal;
        });
        if (result[0] != null) persistence.saveBC(uuid, result[0]);
        return true;
    }

    public boolean deductBC(UUID uuid, int amount) {
        if (amount <= 0) return true;
        Integer[] result = new Integer[1];
        bc.computeIfPresent(uuid, (k, v) -> {
            if (v >= amount) {
                result[0] = v - amount;
                return result[0];
            }
            result[0] = null;
            return v;
        });
        if (result[0] != null) persistence.saveBC(uuid, result[0]);
        return result[0] != null;
    }

    // ════════════════════════════════════════
    // WC operations
    // ════════════════════════════════════════

    public int getWC(UUID uuid) {
        return wc.getOrDefault(uuid, 0);
    }

    public void setWC(UUID uuid, int amount) {
        wc.put(uuid, Math.max(0, amount));
        persistence.saveWC(uuid, amount);
    }

    public boolean addWC(UUID uuid, int amount) {
        if (amount <= 0) return true;
        int matchCap = config.getWCPerMatchCap();
        wc.compute(uuid, (k, v) -> {
            int current = v == null ? 0 : v;
            int capped = Math.min(matchCap, current + amount);
            persistence.saveWC(uuid, capped);
            return capped;
        });
        return true;
    }

    public boolean deductWC(UUID uuid, int amount) {
        if (amount <= 0) return true;
        Integer[] result = new Integer[1];
        wc.computeIfPresent(uuid, (k, v) -> {
            if (v >= amount) {
                result[0] = v - amount;
                persistence.saveWC(uuid, result[0]);
                return result[0];
            }
            result[0] = null;
            return v;
        });
        return result[0] != null;
    }

    // ════════════════════════════════════════
    // VC operations
    // ════════════════════════════════════════

    public int getVC(Team team) {
        int idx = team.ordinal();
        if (idx < 0 || idx >= vc.length) return 0;
        return vc[idx];
    }

    public synchronized boolean addVC(Team team, int amount) {
        return addVC(team, amount, false);
    }

    public synchronized boolean addVC(Team team, int amount, boolean bypassRateLimit) {
        if (amount <= 0 || !team.isPlayable()) return true;
        int idx = team.ordinal();
        int matchCap = config.getVCPerMatchCap();
        if (vcMatchTotal >= matchCap) return false;
        int newVal = Math.min(vc[idx] + amount, matchCap);
        if (!bypassRateLimit) {
            int perMinCap = config.getEconomy().caps.vcPerMin;
            int intervalCap = perMinCap / (1200 / VC_INTERVAL_TICKS);
            if (newVal > vc[idx] + intervalCap) {
                newVal = vc[idx] + intervalCap;
            }
        }
        int added = newVal - vc[idx];
        vc[idx] = newVal;
        vcMatchTotal += added;
        if (vcMatchTotal > matchCap) {
            vcMatchTotal = matchCap;
        }
        syncVCToAll();
        return true;
    }

    public synchronized boolean deductVC(Team team, int amount) {
        if (amount <= 0) return true;
        int idx = team.ordinal();
        if (idx < 0 || idx >= vc.length) return false;
        if (vc[idx] < amount) return false;
        vc[idx] -= amount;
        syncVCToAll();
        return true;
    }

    public synchronized void setVC(Team team, int amount) {
        int idx = team.ordinal();
        if (idx < 0 || idx >= vc.length) return;
        vc[idx] = Math.max(0, amount);
        syncVCToAll();
    }

    // ════════════════════════════════════════
    // Distribution helpers
    // ════════════════════════════════════════

    public void distributeCaptureReward(Map<UUID, Double> contributions, int totalBC, ServerLevel level) {
        if (contributions == null || contributions.isEmpty() || totalBC <= 0) return;

        double totalContrib = 0;
        for (double v : contributions.values()) totalContrib += v;
        if (totalContrib <= 0) return;

        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            UUID uuid = entry.getKey();
            double share = entry.getValue() / totalContrib;
            int reward = (int) Math.round(totalBC * share);
            if (reward <= 0) reward = 1;
            if (reward > totalBC) reward = totalBC;

            addBC(uuid, reward);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                syncBC(player);
                NotificationPacket pkt = new NotificationPacket("+" + reward + " BC за захват", "success", 3000, "");
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);
            }
        }
    }

    // ════════════════════════════════════════
    // Tick system
    // ════════════════════════════════════════

    public void tick(ServerLevel level, TeamManager tm, GameManager gm) {
        if (level == null || tm == null || gm == null) return;

        tickFrontlineIncome(level, tm);
        tickVehicleCredits(level, tm);
    }

    private void tickFrontlineIncome(ServerLevel level, TeamManager tm) {
        frontlineTickCounter++;
        int interval = config.getEconomy().frontlineIncome.normalTicks;
        if (frontlineTickCounter < interval) return;
        frontlineTickCounter = 0;

        ZoneDataManager zones = getZoneData();
        if (zones == null) return;
        List<CaptureZone> allZones = zones.getAllZones();
        if (allZones == null || allZones.isEmpty()) return;

        int rate = config.getEconomy().frontlineIncome.ratePerInterval;

        for (ServerPlayer player : level.players()) {
            UUID uuid = player.getUUID();
            Team playerTeam = tm.getOrCreatePlayerData(uuid).getTeam();
            if (!playerTeam.isPlayable()) continue;

            // Anti-abuse #16: spawn-camp reduces income to 30%
            double incomeMod = 1.0;
            GameManager gm = PWP.getGameManager();
            if (gm != null && gm.getCurrentMap() != null) {
                int[] enemySpawn = playerTeam == Team.NATO
                    ? gm.getCurrentMap().getRussiaSpawn()
                    : gm.getCurrentMap().getNatoSpawn();
                if (enemySpawn != null && enemySpawn.length >= 3) {
                    double distToEnemySpawn = player.blockPosition().distManhattan(
                        new net.minecraft.core.BlockPos(enemySpawn[0], enemySpawn[1], enemySpawn[2]));
                    if (distToEnemySpawn < 50) incomeMod = 0.3;
                }
            }

            for (CaptureZone zone : allZones) {
                double dist = player.blockPosition().distManhattan(zone.getCenter());
                if (dist > FRONTLINE_RADIUS) continue;

                if (zone.getOwnerTeam() == playerTeam && zone.isCaptured()) {
                    int modifiedRate = incomeMod < 1.0 ? Math.max(1, (int)(rate * incomeMod)) : rate;
                    addBC(uuid, modifiedRate);
                    break;
                }
                if (zone.getOwnerTeam() == playerTeam
                    && zone.getCapturingTeam() == Team.SPECTATOR
                    && zone.getProgress() < 1.0f) {
                    int modifiedRate = incomeMod < 1.0 ? Math.max(1, (int)(rate * incomeMod)) : rate;
                    addBC(uuid, modifiedRate);
                    break;
                }
            }
        }
    }

    private void tickVehicleCredits(ServerLevel level, TeamManager tm) {
        vcTickCounter++;
        if (vcTickCounter < VC_INTERVAL_TICKS) return;
        vcTickCounter = 0;

        ZoneDataManager zones = getZoneData();
        if (zones == null) return;
        List<CaptureZone> allZones = zones.getAllZones();
        if (allZones == null) return;

        var vcConfig = config.getEconomy().vc;

        for (CaptureZone zone : allZones) {
            int rate;
            switch (zone.getPointType()) {
                case "major": rate = vcConfig.majorCpPerMin; break;
                case "medium": rate = vcConfig.mediumCpPerMin; break;
                default: rate = vcConfig.smallCpPerMin; break;
            }

            // Anti-abuse #11: undefended point = 0 VC — need at least one player nearby
            if (rate > 0 && !isPointDefended(zone, level, tm)) continue;

            if (zone.getOwnerTeam() == Team.NATO) {
                addVC(Team.NATO, rate);
            } else if (zone.getOwnerTeam() == Team.RUSSIA) {
                addVC(Team.RUSSIA, rate);
            }
        }
    }

    private boolean isPointDefended(CaptureZone zone, ServerLevel level, TeamManager tm) {
        Team owner = zone.getOwnerTeam();
        if (!owner.isPlayable()) return false;
        for (ServerPlayer player : level.players()) {
            if (tm.getOrCreatePlayerData(player.getUUID()).getTeam() != owner) continue;
            double dist = player.blockPosition().distManhattan(zone.getCenter());
            if (dist <= FRONTLINE_RADIUS) return true;
        }
        return false;
    }

    private ZoneDataManager getZoneData() {
        var cpManager = PWP.getCapturePointManager();
        if (cpManager == null) return null;
        return cpManager.getZoneData();
    }

    // ════════════════════════════════════════
    // Persistence
    // ════════════════════════════════════════

    public void loadFromTeamManager(TeamManager tm) {
        if (tm == null) return;
        var dataCopy = tm.getPlayerDataCopy();
        for (Map.Entry<UUID, PlayerCombatData> entry : dataCopy.entrySet()) {
            int bcVal = entry.getValue().getBattleCredits();
            int wcVal = entry.getValue().getWarCredits();
            if (bcVal > 0) bc.put(entry.getKey(), Math.min(bcVal, config.getBCCap()));
            if (wcVal > 0) wc.put(entry.getKey(), wcVal);
        }
    }

    public void loadForPlayer(UUID uuid) {
        int dbBc = persistence.loadBC(uuid);
        int dbWc = persistence.loadWC(uuid);
        int existingBc = bc.getOrDefault(uuid, 0);
        if (dbBc > 0 && dbBc > existingBc) {
            bc.put(uuid, Math.min(dbBc, config.getBCCap()));
        } else if (!bc.containsKey(uuid)) {
            bc.put(uuid, Math.min(dbBc, config.getBCCap()));
        }
        int existingWc = wc.getOrDefault(uuid, 0);
        if (dbWc > 0 && dbWc > existingWc) {
            wc.put(uuid, dbWc);
        } else if (!wc.containsKey(uuid)) {
            wc.put(uuid, dbWc);
        }
    }

    // D10: award daily login bonus if not yet claimed today (UTC)
    public boolean checkAndAwardDailyBonus(UUID uuid) {
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        String lastDate = lastDailyBonusDate.get(uuid);
        if (today.equals(lastDate)) return false;
        // Bypass match cap for daily bonus — add directly, not via addWC
        wc.compute(uuid, (k, v) -> {
            int current = v == null ? 0 : v;
            int newVal = current + DAILY_LOGIN_BONUS;
            persistence.saveWC(uuid, newVal);
            return newVal;
        });
        lastDailyBonusDate.put(uuid, today);
        return true;
    }

    public void flushPlayer(UUID uuid) {
        Integer b = bc.get(uuid);
        Integer w = wc.get(uuid);
        if (b != null) persistence.saveBC(uuid, b);
        if (w != null) persistence.saveWC(uuid, w);
    }

    public void flushToTeamManager(TeamManager tm) {
        if (tm == null) return;
        for (Map.Entry<UUID, Integer> entry : bc.entrySet()) {
            PlayerCombatData data = tm.getOrCreatePlayerData(entry.getKey());
            data.setBattleCredits(entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : wc.entrySet()) {
            PlayerCombatData data = tm.getOrCreatePlayerData(entry.getKey());
            data.setWarCredits(entry.getValue());
        }
    }

    // ════════════════════════════════════════
    // Client sync
    // ════════════════════════════════════════

    public void syncBC(ServerPlayer player) {
        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new BCSyncPacket(getBC(player.getUUID())));
    }

    public void syncWC(ServerPlayer player) {
        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new WCSyncPacket(getWC(player.getUUID())));
    }

    public void syncVC(ServerPlayer player) {
        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new VehicleCreditsSyncPacket(vc[Team.NATO.ordinal()], vc[Team.RUSSIA.ordinal()]));
    }

    public void syncVCToAll() {
        PacketHandler.CHANNEL.send(
            PacketDistributor.ALL.noArg(),
            new VehicleCreditsSyncPacket(vc[Team.NATO.ordinal()], vc[Team.RUSSIA.ordinal()]));
    }

    public void syncAll(ServerPlayer player) {
        syncBC(player);
        syncWC(player);
        syncVC(player);
    }

    public void syncBCToAll() {
        for (ServerPlayer player : PWP.getGameManager().getServer().getPlayerList().getPlayers()) {
            syncBC(player);
        }
    }

    // ════════════════════════════════════════
    // Match lifecycle
    // ════════════════════════════════════════

    public void resetMatch() {
        bc.clear();
        vc[0] = 0;
        vc[1] = 0;
        vcMatchTotal = 0;
        frontlineTickCounter = 0;
        vcTickCounter = 0;
    }

    public void resetForPlayer(UUID uuid) {
        bc.remove(uuid);
    }

    // ════════════════════════════════════════
    // Activity tracking (delegated for now)
    // ════════════════════════════════════════

    public void addBCAndActivity(UUID uuid, int bcAmount, int activityScore) {
        addBC(uuid, bcAmount);
        BattlefieldRuntime.getInstance().addActivity(uuid, activityScore);
    }
}
