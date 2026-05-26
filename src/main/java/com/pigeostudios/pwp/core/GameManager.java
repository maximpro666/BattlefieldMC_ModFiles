package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.commands.StartMatchCommand;
import com.pigeostudios.pwp.data.CentralDatabase;
import com.pigeostudios.pwp.service.EconomyService;
import com.pigeostudios.pwp.network.GameStateSyncPacket;
import com.pigeostudios.pwp.network.NotificationPacket;
import com.pigeostudios.pwp.network.OpenMatchResultsPacket;
import com.pigeostudios.pwp.network.OpenVoteScreenPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TransferPacket;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
import static com.pigeostudios.pwp.core.ChatHelper.*;
import static com.pigeostudios.pwp.core.PWPColors.*;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import com.pigeostudios.pwp.network.VoiceSpeakingStatePacket;
import com.pigeostudios.pwp.network.OpenTeamSelectionScreenPacket;
import com.pigeostudios.pwp.network.TeamBaseSyncPacket;
import com.pigeostudios.pwp.network.BorderSyncPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class GameManager {
    public enum GamePhase {
        LOBBY,
        VOTING,
        PLAYING,
        ENDING
    }

    private static final ResourceLocation LOBBY_DIMENSION_ID = new ResourceLocation("minecraft", "overworld");
    private static final BlockPos LOBBY_SPAWN = new BlockPos(136, 68, -140);
    private static final int VOTE_SECONDS = 30;
    private static final int COUNTDOWN_SECONDS = 15;
    private static final int MATCH_SECONDS = 1800;
    private static final int OVERTIME_TICKETS = 15;
    private static final int MAX_OVERTIME_TICKS = 20 * 60;

    private final MinecraftServer server;
    private GamePhase currentPhase;
    private int phaseTimer;
    private Team winningTeam;
    private boolean lobbyLoaded;
    private MapConfig currentMap;
    private ResourceKey<Level> currentDimKey;
    private boolean overtime;
    private int overtimeTicks;
    private int captureTicks;
    private int matchTimeRemaining;
    private Set<Integer> sentTimeWarnings;
    /** Match index used to determine team rotation parity */
    private int currentMatchIndex;
    private boolean mapReady;
    private BorderManager borderManager;
    private List<String> currentVoteCandidates = new ArrayList<>();
    private boolean pendingAutoStart = false;
    private String lobbyStatus = "";

    public void setLobbyStatus(String status) {
        this.lobbyStatus = status != null ? status : "";
        syncPhaseToAll();
    }

    public GameManager(MinecraftServer server) {
        this.server = server;
        this.currentPhase = GamePhase.LOBBY;
        this.phaseTimer = 0;
        this.winningTeam = null;
        this.lobbyLoaded = false;
        this.currentMap = null;
        this.overtime = false;
        this.overtimeTicks = 0;
        this.captureTicks = 0;
        this.matchTimeRemaining = 0;
        this.sentTimeWarnings = new HashSet<>();
        this.currentMatchIndex = 0;
        this.mapReady = false;
        this.borderManager = new BorderManager();
        applyLobbyRules();
    }

    public boolean isMapReady() { return mapReady; }

    public GamePhase getCurrentPhase() { return currentPhase; }
    public MinecraftServer getServer() { return server; }
    public Team getWinningTeam() { return winningTeam; }
    public boolean isPlaying() { return currentPhase == GamePhase.PLAYING; }
    public int getMatchTimeRemaining() { return matchTimeRemaining; }
    public boolean isVoting() { return currentPhase == GamePhase.VOTING; }
    public boolean isLobby() { return currentPhase == GamePhase.LOBBY; }
    public boolean isOvertime() { return overtime; }
    public MapConfig getCurrentMap() { return currentMap; }
    public ResourceKey<Level> getCurrentDimKey() { return currentDimKey; }

    public void startInitialCountdown() {
        if (currentPhase == GamePhase.LOBBY && phaseTimer <= 0) {
            phaseTimer = COUNTDOWN_SECONDS * 20;
            PWP.LOGGER.info("Initial countdown started: {} seconds", COUNTDOWN_SECONDS);
        }
    }

    // ========== Match Flow ==========

    public void startGame() {
        TeamManager teamManager = PWP.getTeamManager();
        MapPoolManager mapPool = PWP.getMapPoolManager();

        PWP.LOGGER.info("=== startGame: hasAvailableMaps={}, total maps={} ===", mapPool.hasAvailableMaps(), mapPool.getMaps().size());
        for (MapConfig mc : mapPool.getMaps()) {
            PWP.LOGGER.info("Map: {} enabled={} state={} nato={} rus={} hasCP={} cp={} wbs={} playable={}",
                mc.getName(), mc.isEnabled(), mc.getState(),
                mc.hasTeamSpawns() ? java.util.Arrays.toString(mc.getNatoSpawn()) : "null",
                mc.hasTeamSpawns() ? java.util.Arrays.toString(mc.getRussiaSpawn()) : "null",
                mc.hasCapturePoints(),
                mc.getCapturePoints() == null ? "null" : String.valueOf(mc.getCapturePoints().size()),
                mc.getWorldBorderSize(),
                mc.isPlayable());
        }

        if (!mapPool.hasAvailableMaps()) {
            broadcast("Нет доступных карт! Настройте карты.", CHAT_ERROR);
            currentPhase = GamePhase.LOBBY;
            phaseTimer = 0;
            return;
        }

        MapConfig map = mapPool.getCurrentMap().orElse(null);
        if (map == null) {
            map = mapPool.pickNextAvailable();
            if (map == null) {
                currentPhase = GamePhase.LOBBY;
                phaseTimer = 0;
                return;
            }
        }

        if (!mapPool.copyToLive(map)) {
            broadcast("Не удалось загрузить карту " + map.getName(), CHAT_ERROR);
            currentPhase = GamePhase.LOBBY;
            phaseTimer = 0;
            return;
        }

        mapReady = false;
        currentPhase = GamePhase.PLAYING;
        phaseTimer = 0;
        winningTeam = null;
        currentMap = map;
        overtime = false;
        overtimeTicks = 0;
        matchTimeRemaining = MATCH_SECONDS;
        sentTimeWarnings.clear();

        var fobMgr = PWP.getFOBManager();
        if (fobMgr != null) fobMgr.clearAll();

        var ticketSvc = PWP.getServiceRegistry().getTickets();
        if (ticketSvc != null) ticketSvc.resetTickets(map.getTickets());

        BattlefieldRuntime.getInstance().resetMatch();

        CapturePointManager cp = PWP.getCapturePointManager();
        if (cp != null) {
            if (map.hasCapturePoints()) {
                cp.loadFromMapConfig(map, 0);
                cp.setActive(true);
            } else {
                cp.clearPoints();
                cp.setActive(false);
            }
        }

        ContributionManager contrib = PWP.getContributionManager();
        if (contrib != null) contrib.resetMatch();

        currentDimKey = DynamicDimensionManager.getDimKey();
        ServerLevel mapLevel = server.getLevel(currentDimKey);
        if (mapLevel == null) {
            PWP.LOGGER.error("pwp:map dimension not available, aborting start");
            currentPhase = GamePhase.LOBBY;
            phaseTimer = 0;
            currentMap = null;
            currentDimKey = null;
            return;
        }

        clearWorldEntities(mapLevel);

        currentMatchIndex = PWP.getMapPoolManager().nextMatchId();
        boolean swapped = map.isTeamRotation() && (currentMatchIndex % 2 == 1);

        applyMapConfig(map, 0);

        if (map.hasBorderZones()) {
            borderManager.setZones(map.getBorderZones());
            PWP.LOGGER.info("Border zones set for map {}", map.getName());
        } else {
            borderManager.setZones(null);
        }

        unloadChunksOutsideZone(mapLevel, 0);
        autoAssignTeams(map);
        teleportAllPlayersToMap(map, 0, swapped);
        openLoadoutScreenForAllPlayers();

        gamerule(server.overworld(), "liberateAttachment", "false");

        mapReady = true;

        sendNotificationToAll("\u0418\u0433\u0440\u0430 \u043d\u0430\u0447\u0430\u043b\u0430\u0441\u044c \u043d\u0430 \u043a\u0430\u0440\u0442\u0435: " + map.getName(), "success", 5000);
        syncPhaseToAll();
        syncTeamBasesToAll(map);
        syncBordersToAll(map);
        if (cp != null && cp.isActive()) cp.syncToAll();
        PWP.LOGGER.info("Game started on map: {}", map.getName());
    }

    private void syncTeamBasesToAll(MapConfig map) {
        if (map == null || !map.hasTeamSpawns()) return;
        int[] nato = map.getNatoSpawn();
        int[] russia = map.getRussiaSpawn();
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
            new TeamBaseSyncPacket(
                nato[0], nato[1], nato[2],
                russia[0], russia[1], russia[2],
                map.getBaseRadius()
            ));
    }

    private void syncBordersToAll(MapConfig map) {
        if (map == null || !map.hasBorderZones()) return;
        List<BorderZone> zones = map.getBorderZones();
        List<byte[]> types = new ArrayList<>();
        List<double[]> data = new ArrayList<>();
        for (BorderZone z : zones) {
            if ("polygon".equals(z.getType())) {
                types.add(new byte[]{(byte)1});
                List<double[]> verts = z.getPolygon();
                double[] arr = new double[verts.size() * 2];
                for (int i = 0; i < verts.size(); i++) {
                    arr[i * 2] = verts.get(i)[0];
                    arr[i * 2 + 1] = verts.get(i)[1];
                }
                data.add(arr);
            } else {
                types.add(new byte[]{(byte)0});
                data.add(new double[]{z.getMinX(), z.getMinZ(), z.getMaxX(), z.getMaxZ()});
            }
        }
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
            new BorderSyncPacket(types, data));
    }

    public void endGame(Team winner) {
        if (currentPhase != GamePhase.PLAYING) return;

        currentPhase = GamePhase.ENDING;
        winningTeam = winner;
        phaseTimer = 80;

        var ticketSvc = PWP.getServiceRegistry().getTickets();
        int natoTk = ticketSvc != null ? ticketSvc.getTickets(Team.NATO) : 0;
        int russiaTk = ticketSvc != null ? ticketSvc.getTickets(Team.RUSSIA) : 0;

        if (currentMap != null) {
            com.pigeostudios.pwp.data.CentralDatabase.recordMatchPlayed(currentMap.getName(),
                winner != null ? winner.getName() : "DRAW", natoTk, russiaTk);
        }

        String winName = winner != null ? winner.getName() : "DRAW";
        String winMsg = "\u2694 " + winName + " \u043f\u043e\u0431\u0435\u0436\u0434\u0430\u0435\u0442!";
        sendNotificationToAll(winMsg, "match", 6000);

        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        PWPConfig cfg = PWP.getConfig();
        TeamManager tm = PWP.getTeamManager();

        Map<UUID, Integer> bcEarned = new HashMap<>();
        Map<UUID, Integer> wcEarned = new HashMap<>();
        List<UUID> winningPlayers = new ArrayList<>();
        List<UUID> losingPlayers = new ArrayList<>();

        double winnerAvgRating = 0;
        double loserAvgRating = 0;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int baseBC = cfg != null ? cfg.getWinRewardBC() : 25;
            int baseWC = cfg != null ? cfg.getWinRewardWC() : 50;
            PlayerCombatData pcd = tm.getOrCreatePlayerData(p.getUUID());
            boolean isWinner = winner != null && pcd.getTeam() == winner;
            if (isWinner) {
                baseBC += 50;
                baseWC += 50;
            }
            // Fix C6: use EconomyService (single add + persist), avoid pcd double-add
            if (eco != null) {
                eco.addBC(p.getUUID(), baseBC);
                eco.addWC(p.getUUID(), baseWC);
            } else {
                runtime.addBC(p.getUUID(), baseBC);
                runtime.addWC(p.getUUID(), baseWC);
            }
            pcd.setWarCredits(eco != null ? eco.getWC(p.getUUID()) : runtime.getWC(p.getUUID()));
            runtime.syncAll(p);
            bcEarned.put(p.getUUID(), baseBC);
            wcEarned.put(p.getUUID(), baseWC);

            String rewardMsg = "+" + baseBC + " BC, +" + baseWC + " WC";
            NotificationPacket rewardPkt = new NotificationPacket(rewardMsg, "success", 4000, "");
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), rewardPkt);

            if (pcd.getTeam().isPlayable()) {
                if (isWinner) {
                    winningPlayers.add(p.getUUID());
                    winnerAvgRating += pcd.getRating();
                } else if (winner != null) {
                    losingPlayers.add(p.getUUID());
                    loserAvgRating += pcd.getRating();
                }
            }
        }

        if (!winningPlayers.isEmpty()) winnerAvgRating /= winningPlayers.size();
        if (!losingPlayers.isEmpty()) loserAvgRating /= losingPlayers.size();

        // Update wins and rating for all playable players
        int K = 32;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerCombatData pcd = tm.getOrCreatePlayerData(p.getUUID());
            if (!pcd.getTeam().isPlayable() || winner == null) continue;
            boolean isWinner = pcd.getTeam() == winner;
            if (isWinner) {
                pcd.addWin();
                double opponentAvg = loserAvgRating > 0 ? loserAvgRating : 1000;
                double expected = 1.0 / (1.0 + Math.pow(10, (opponentAvg - pcd.getRating()) / 400.0));
                int ratingChange = (int) Math.round(K * (1.0 - expected));
                pcd.setRating(pcd.getRating() + ratingChange);
            } else {
                double opponentAvg = winnerAvgRating > 0 ? winnerAvgRating : 1000;
                double expected = 1.0 / (1.0 + Math.pow(10, (opponentAvg - pcd.getRating()) / 400.0));
                int ratingChange = (int) Math.round(K * (0.0 - expected));
                pcd.setRating(pcd.getRating() + ratingChange);
            }
        }

        tm.setDirty();

        CapturePointManager cp = PWP.getCapturePointManager();
        if (cp != null) cp.setActive(false);

        ContributionManager contrib = PWP.getContributionManager();
        List<OpenMatchResultsPacket.PlayerResultEntry> playerResults = new ArrayList<>();

        if (contrib != null) {
            List<ContributionData> allContribs = contrib.getAll();
            int maxScore = -1;
            UUID mvpUUID = null;

            for (ContributionData cd : allContribs) {
                if (cd.getKills() == 0 && cd.getAssists() == 0 && cd.getCaptures() == 0) continue;
                int score = cd.getTotalScore();
                if (score > maxScore) {
                    maxScore = score;
                    mvpUUID = cd.getPlayerUUID();
                }
            }

            Team natoTeam = Team.NATO;
            Team russiaTeam = Team.RUSSIA;
            int natoTotal = 0;
            int russiaTotal = 0;

            for (ContributionData cd : allContribs) {
                UUID uuid = cd.getPlayerUUID();
                Team team = tm.getOrCreatePlayerData(uuid).getTeam();
                int score = cd.getTotalScore();
                if (team == natoTeam) natoTotal += score;
                else if (team == russiaTeam) russiaTotal += score;

                playerResults.add(new OpenMatchResultsPacket.PlayerResultEntry(
                    cd.getPlayerName(),
                    team.ordinal(),
                    cd.getKills(),
                    cd.getDeaths(),
                    cd.getAssists(),
                    cd.getCaptures(),
                    score,
                    bcEarned.getOrDefault(uuid, 0),
                    wcEarned.getOrDefault(uuid, 0),
                    uuid.equals(mvpUUID)
                ));
            }

            contrib.resetMatch();

            OpenMatchResultsPacket resultsPkt = new OpenMatchResultsPacket(
                winner != null ? winner.ordinal() : -1,
                currentMap != null ? currentMap.getName() : "",
                MATCH_SECONDS - matchTimeRemaining,
                natoTk, russiaTk,
                natoTotal, russiaTotal,
                playerResults
            );

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), resultsPkt);
            }
        }

        captureTicks = 0;
        overtime = false;
        overtimeTicks = 0;

        syncPhaseToAll();
        PWP.LOGGER.info("Game ended. Winner: {}", winner != null ? winner.getName() : "DRAW");
    }

    private int voiceBroadcastTick = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        voiceBroadcastTick++;
        if (voiceBroadcastTick % 10 == 0) {
            broadcastVoiceSpeakers();
        }

        if (currentPhase == GamePhase.ENDING) {
            phaseTimer--;
            if (phaseTimer <= 0) finishMatchCycle();
        }

        if (currentPhase == GamePhase.VOTING) {
            phaseTimer--;
            if (phaseTimer <= 0) resolveVoting();

            if (phaseTimer > 0 && phaseTimer % 20 == 0) {
                int sec = phaseTimer / 20;
                MapPoolManager pool = PWP.getMapPoolManager();
                Map<String, Integer> tally = pool.getVoteTally();
                int[] votes = new int[currentVoteCandidates.size()];
                for (int i = 0; i < currentVoteCandidates.size(); i++) {
                    votes[i] = tally.getOrDefault(currentVoteCandidates.get(i).toLowerCase(), 0);
                }
                PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new com.pigeostudios.pwp.network.VoteSyncPacket(sec, votes));

                if (sec <= 5 || sec % 5 == 0) {
                    sendNotificationToAll("\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430\u0432\u0435\u0440\u0448\u0438\u0442\u0441\u044f \u0447\u0435\u0440\u0435\u0437 " + sec + "\u0441", "warning", 3000);
                }
            }
        }

        if (currentPhase == GamePhase.LOBBY && phaseTimer > 0) {
            phaseTimer--;
            if (phaseTimer % 20 == 0 && phaseTimer > 0) {
                int sec = phaseTimer / 20;
                if (sec <= 5 || sec % 10 == 0) {
                    sendNotificationToAll("Игра начнётся через " + sec + "с", "warning", 3000);
                    PWP.LOGGER.info("Game starts in {}s", sec);
                }
            }
            if (phaseTimer <= 0) startGame();
        }

        if (currentPhase == GamePhase.LOBBY && phaseTimer <= 0) {
            if (!ProxyMessenger.isMatchServer() && PWP.getConfig().isAutoStartVoting()) {
                int onlinePlayers = server.getPlayerList().getPlayers().size();
                int minPlayers = PWP.getConfig().getMinPlayersToStart();
                if (onlinePlayers >= minPlayers) {
                    PWP.LOGGER.info("Auto-starting voting ({} players >= {} min)", onlinePlayers, minPlayers);
                    startVotingWithAutoStart();
                }
            }
        }

        if (currentPhase == GamePhase.PLAYING) {
            captureTicks++;

            var ticketSvc = PWP.getServiceRegistry().getTickets();

            if (captureTicks % 20 == 0) {
                if (!overtime && matchTimeRemaining > 0) {
                    matchTimeRemaining--;
                }

                if (!overtime) {
                    int[] thresholds = {600, 300, 120, 60, 30, 10};
                    for (int t : thresholds) {
                        if (matchTimeRemaining == t && !sentTimeWarnings.contains(t)) {
                            sentTimeWarnings.add(t);
                            String key = t >= 120 ? "match_" + (t / 60) + "min" :
                                         t == 60 ? "match_1min" :
                                         t == 30 ? "match_30s" : "match_10s";
                            LifecycleNotifier.broadcastNotification(server, key, 3000);
                            String timeText = t >= 60 ? (t / 60) + " \u043c\u0438\u043d" : t + "\u0441";
                            sendNotificationToAll("\u041c\u0430\u0442\u0447 \u0437\u0430\u0432\u0435\u0440\u0448\u0438\u0442\u0441\u044f \u0447\u0435\u0440\u0435\u0437 " + timeText, "warning", 3000);
                        }
                    }

                    if (ticketSvc != null && matchTimeRemaining <= 0) {
                        int nTk = ticketSvc.getTickets(Team.NATO);
                        int rTk = ticketSvc.getTickets(Team.RUSSIA);
                        if (nTk > 0 && rTk > 0) {
                            overtime = true;
                            overtimeTicks = 0;
                            sendNotificationToAll("\u041e\u0412\u0415\u0420\u0422\u0410\u0419\u041c!", "match", 4000);
                            LifecycleNotifier.broadcastNotification(server, "match_overtime", 4000);
                        } else {
                            Team winner = nTk > rTk ? Team.NATO : (rTk > nTk ? Team.RUSSIA : Team.SPECTATOR);
                            endGame(winner);
                        }
                    }
                }

                CapturePointManager cp = PWP.getCapturePointManager();
                if (cp != null) {
                    MapConfig map = currentMap;
                    if (map != null) {
                        ServerLevel level = getMapWorld(map);
                        if (level != null) cp.tickCapturePoints(level);
                    }
                }

                if (borderManager != null && borderManager.hasZones()) {
                    ServerLevel mapLevel = getMapWorld(currentMap);
                    if (mapLevel != null) borderManager.tick(mapLevel);
                }

                if (ticketSvc != null) ticketSvc.tick(overtime);

                if (!overtime) {
                    var ts2 = PWP.getServiceRegistry().getTickets();
                    int natoTickets = ts2 != null ? ts2.getTickets(Team.NATO) : 0;
                    int russiaTickets = ts2 != null ? ts2.getTickets(Team.RUSSIA) : 0;
                    if (natoTickets <= OVERTIME_TICKETS || russiaTickets <= OVERTIME_TICKETS) {
                        if (natoTickets > 0 && russiaTickets > 0) {
                            overtime = true;
                            overtimeTicks = 0;
                            sendNotificationToAll("\u041e\u0412\u0415\u0420\u0422\u0410\u0419\u041c!", "match", 4000);
                            LifecycleNotifier.broadcastNotification(server, "match_overtime", 4000);
                        }
                    }
                } else {
                    overtimeTicks++;
                    if (overtimeTicks >= MAX_OVERTIME_TICKS) {
                        var ts2 = PWP.getServiceRegistry().getTickets();
                        int natoTk = ts2 != null ? ts2.getTickets(Team.NATO) : 0;
                        int russiaTk = ts2 != null ? ts2.getTickets(Team.RUSSIA) : 0;
                        Team winner = natoTk >= russiaTk ? Team.NATO : Team.RUSSIA;
                        endGame(winner);
                    }
                }
            }
        }
    }

    // ========== Match Lifecycle ==========

    private void finishMatchCycle() {
        if (ProxyMessenger.isMatchServer()) {
            // Player data and vehicle cooldowns are already synced to CentralDatabase
            // in real-time during the match. No explicit export needed.

            sendNotificationToAll("\u041c\u0430\u0442\u0447 \u0437\u0430\u0432\u0435\u0440\u0448\u0430\u0435\u0442\u0441\u044f, \u0432\u043e\u0437\u0432\u0440\u0430\u0442 \u0432 \u043b\u043e\u0431\u0431\u0438...", "match", 4000);
            // Send players back to lobby via TransferPacket
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new TransferPacket(PWP.getConfig().getProxyAddress()));
                sendNotificationToPlayer(p, "\u0412\u043e\u0437\u0432\u0440\u0430\u0449\u0435\u043d\u0438\u0435 \u0432 \u043b\u043e\u0431\u0431\u0438...", "info", 4000);
            }
            // Signal lobby server to auto-start next match
            try {
                Path flagFile = Path.of(System.getProperty("user.dir")).resolve("../launcher/match_cycle_done.flag").normalize();
                Files.write(flagFile, new byte[0]);
            } catch (Exception e) {
                PWP.LOGGER.error("Failed to write cycle flag", e);
            }
            server.execute(() -> {
                try { Thread.sleep(15000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                server.halt(false);
            });
            return;
        }

        teleportAllToLobby();
        applyLobbyRules();
        currentMap = null;

        startVoting();
    }

    public void startVotingWithAutoStart() {
        pendingAutoStart = true;
        startVoting();
    }

    public void startMatchServerGame() {
        MapPoolManager pool = PWP.getMapPoolManager();
        pool.clearVotes();

        // Read selected map from CentralDatabase (written by lobby server after voting)
        String mapName = CentralDatabase.getSelectedMap();
        if (mapName != null) {
            CentralDatabase.clearSelectedMap();
            if (!mapName.isEmpty() && pool.selectMap(mapName)) {
                PWP.LOGGER.info("Selected map from CentralDatabase: {}", mapName);
            } else {
                PWP.LOGGER.warn("CentralDatabase contained '{}' but selectMap failed, falling back", mapName);
            }
        }

        MapConfig map = pool.getCurrentMap().orElse(null);
        if (map == null) {
            map = pool.pickNextAvailable();
        }
        if (map == null) {
            PWP.LOGGER.error("No available maps on match server!");
            broadcast("Нет доступных карт! Настройте карты и включите их.", CHAT_ERROR);
            currentPhase = GamePhase.LOBBY;
            phaseTimer = 0;
            return;
        }

        currentPhase = GamePhase.LOBBY;
        phaseTimer = COUNTDOWN_SECONDS * 20;
        currentMap = null;
        winningTeam = null;
        currentVoteCandidates.clear();

        sendNotificationToAll("Игра начнётся через " + COUNTDOWN_SECONDS + "с на " + map.getName(), "success", 5000);
        applyLobbyRules();
        syncPhaseToAll();
        PWP.LOGGER.info("Match server started, game will begin on map: {}", map.getName());
    }

    public void startVoting() {
        currentPhase = GamePhase.VOTING;
        phaseTimer = VOTE_SECONDS * 20;
        winningTeam = null;
        currentMap = null;

        MapPoolManager pool = PWP.getMapPoolManager();
        pool.clearVotes();

        CentralDatabase.clearSelectedMap();

        List<String> recentPlayed;
        try {
            recentPlayed = com.pigeostudios.pwp.data.CentralDatabase.getRecentPlayedMaps(3);
        } catch (Exception e) {
            recentPlayed = new ArrayList<>();
        }
        List<MapConfig> candidates = pool.pickVotingCandidates(9, recentPlayed);
        if (candidates.isEmpty()) {
            broadcast("Нет доступных карт для голосования! Настройте карты.", CHAT_ERROR);
            currentPhase = GamePhase.LOBBY;
            phaseTimer = 0;
            pendingAutoStart = false;
            return;
        }

        currentVoteCandidates = new ArrayList<>();
        List<String> availNames = new ArrayList<>();
        int[] voteCounts = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            MapConfig m = candidates.get(i);
            currentVoteCandidates.add(m.getName());
            availNames.add(m.getName());
            voteCounts[i] = 0;
        }

        sendNotificationToAll("\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0435 \u0437\u0430 \u043d\u043e\u0432\u0443\u044e \u043a\u0430\u0440\u0442\u0443!", "match", 5000);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
            new OpenVoteScreenPacket(availNames, voteCounts, phaseTimer / 20));

        applyLobbyRules();
        syncPhaseToAll();
        PWP.LOGGER.info("Voting started with {} candidates", candidates.size());
    }

    private void resolveVoting() {
        MapPoolManager pool = PWP.getMapPoolManager();

        String winner = pool.resolveVoteWinner();
        if (winner != null && pool.selectMap(winner)) {
            sendNotificationToAll("\u041a\u0430\u0440\u0442\u0430 \"" + winner + "\" \u043f\u043e\u0431\u0435\u0434\u0438\u043b\u0430 \u0432 \u0433\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0438!", "success", 4000);
        } else {
            MapConfig picked = pool.pickNextAvailable();
            if (picked == null) {
                sendNotificationToAll("\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0445 \u043a\u0430\u0440\u0442!", "error", 4000);
                pendingAutoStart = false;
                currentPhase = GamePhase.LOBBY;
                phaseTimer = 0;
                return;
            }
            sendNotificationToAll("\u0410\u0432\u0442\u043e\u0432\u044b\u0431\u043e\u0440 \u043a\u0430\u0440\u0442\u044b: " + picked.getName(), "warning", 4000);
        }

        pool.clearVotes();
        currentPhase = GamePhase.LOBBY;
        phaseTimer = COUNTDOWN_SECONDS * 20;
        String mapName = pool.getCurrentMap().map(MapConfig::getName).orElse("?");
        sendNotificationToAll("\u0418\u0433\u0440\u0430 \u043d\u0430\u0447\u043d\u0435\u0442\u0441\u044f \u0447\u0435\u0440\u0435\u0437 " + COUNTDOWN_SECONDS + "\u0441 \u043d\u0430 " + mapName, "success", 4000);

        // Write selected map name to CentralDatabase for match server
        if (!mapName.equals("?")) {
            CentralDatabase.setSelectedMap(mapName);
            PWP.LOGGER.info("Wrote selected map '{}' to CentralDatabase", mapName);
        }

        if (pendingAutoStart) {
            pendingAutoStart = false;
            PWP.LOGGER.info("Auto-starting match server after voting...");
            StartMatchCommand.startMatchServerAsync(server);
            phaseTimer = 0;
        }
    }

    private void returnToLobby() {
        currentPhase = GamePhase.LOBBY;
        phaseTimer = COUNTDOWN_SECONDS * 20;
        winningTeam = null;
        currentMap = null;
        teleportAllToLobby();
        applyLobbyRules();
        syncPhaseToAll();

        MapPoolManager pool = PWP.getMapPoolManager();
        if (pool.hasAvailableMaps()) {
            pool.pickNextAvailable();
        }
        sendNotificationToAll("\u0418\u0433\u0440\u0430 \u043d\u0430\u0447\u043d\u0435\u0442\u0441\u044f \u0447\u0435\u0440\u0435\u0437 " + COUNTDOWN_SECONDS + "\u0441", "success", 4000);
        PWP.LOGGER.info("Returned to lobby");
    }

    // ========== Voting API ==========

    public boolean voteMap(ServerPlayer player, String mapName) {
        if (currentPhase != GamePhase.VOTING) {
            player.sendSystemMessage(error("Not in voting phase!"));
            return false;
        }
        MapPoolManager pool = PWP.getMapPoolManager();

        boolean found = pool.getPlayableMaps().stream()
            .anyMatch(m -> m.getName().equalsIgnoreCase(mapName));
        if (!found) {
            player.sendSystemMessage(error("Map not available: " + mapName));
            return false;
        }

        return pool.castVote(player, mapName);
    }

    // ========== Timer Overrides ==========

    public void sendVoteScreenToPlayer(ServerPlayer player) {
        if (currentPhase != GamePhase.VOTING) return;
        MapPoolManager pool = PWP.getMapPoolManager();
        Map<String, Integer> tally = pool.getVoteTally();
        int[] votes = new int[currentVoteCandidates.size()];
        for (int i = 0; i < currentVoteCandidates.size(); i++) {
            votes[i] = tally.getOrDefault(currentVoteCandidates.get(i).toLowerCase(), 0);
        }
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new OpenVoteScreenPacket(new ArrayList<>(currentVoteCandidates), votes, phaseTimer / 20));
    }

    public void startCountdown(int seconds) {
        if (currentPhase != GamePhase.VOTING && currentPhase != GamePhase.LOBBY) return;
        phaseTimer = seconds * 20;
        broadcast("Countdown set to " + seconds + "s", CHAT_SUCCESS);
    }

    public void cancelCountdown() {
        phaseTimer = 0;
        broadcast("Countdown cancelled.", CHAT_ERROR);
    }

    // ========== World ==========

    private void clearWorldEntities(ServerLevel level) {
        List<Entity> all = new java.util.ArrayList<>();
        for (Entity e : level.getEntities().getAll()) all.add(e);
        int removed = 0;
        for (Entity e : all) {
            if (e != null && !(e instanceof ServerPlayer) && !(e instanceof Player)) {
                e.discard();
                removed++;
            }
        }
        if (removed > 0) PWP.LOGGER.info("Removed {} entities", removed);
    }

    // ========== Rules ==========

    private void applyLobbyRules() {
        ServerLevel l = getLobbyWorld();
        if (l == null) return;
        gamerule(l, "doMobSpawning", "false");
        gamerule(l, "doPatrolSpawning", "false");
        gamerule(l, "doTraderSpawning", "false");
        gamerule(l, "doWardenSpawning", "false");
        gamerule(l, "doWeatherCycle", "false");
        gamerule(l, "mobGriefing", "false");
        gamerule(l, "doFireTick", "false");
        gamerule(l, "keepInventory", "true");
        gamerule(l, "naturalRegeneration", "true");
        gamerule(l, "fallDamage", "false");
        gamerule(l, "randomTickSpeed", "0");
        // World border to prevent new chunk generation beyond lobby
        if (ProxyMessenger.isMatchServer()) {
            l.getWorldBorder().setCenter(136, 116);
            l.getWorldBorder().setSize(500);
        } else {
            l.getWorldBorder().setCenter(136, -140);
            l.getWorldBorder().setSize(500);
        }
    }

    private void applyMapConfig(MapConfig map, int zOffset) {
        ServerLevel l = getMapWorld(map);
        if (l == null) return;
        gamerule(l, "naturalRegeneration", "false");
        gamerule(l, "doImmediateRespawn", map.hasRespawn() ? "false" : "true");
        gamerule(l, "doMobSpawning", "false");
        gamerule(l, "mobGriefing", "false");
        gamerule(l, "doFireTick", "true");
        gamerule(l, "doWeatherCycle", "false");
        gamerule(l, "keepInventory", "false");
        gamerule(l, "fallDamage", "true");
        l.getWorldBorder().setCenter(map.getWorldBorderCenterX(), map.getWorldBorderCenterZ() + zOffset);
        l.getWorldBorder().setSize(3.0E7);
    }

    private void gamerule(ServerLevel l, String rule, String val) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(l), "gamerule " + rule + " " + val);
    }

    // ========== Teleport ==========

    private void autoAssignTeams(MapConfig map) {
        TeamManager tm = PWP.getTeamManager();
        int natoCount = 0, russiaCount = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerCombatData data = tm.getOrCreatePlayerData(p.getUUID());
            if (!data.hasChosenTeam()) {
                Team assigned = (natoCount <= russiaCount) ? Team.NATO : Team.RUSSIA;
                data.setTeam(assigned);
                data.setHasChosenTeam(true);
                if (assigned == Team.NATO) natoCount++;
                else russiaCount++;
                tm.updatePlayerDisplayName(p);
                tm.syncPlayerTeam(p);
            }
        }
    }

    private void openLoadoutScreenForAllPlayers() {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            TeamManager tm = PWP.getTeamManager();
            if (tm.getOrCreatePlayerData(p.getUUID()).getTeam() == Team.SPECTATOR) continue;
            tm.syncKitConfig(p);
            tm.syncRank(p);
            tm.syncVehicles(p);
            BattlefieldRuntime.getInstance().syncAll(p);
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new com.pigeostudios.pwp.network.OpenLoadoutScreenPacket());
        }
    }

    public void teleportAllPlayersToMap(MapConfig map, int zOffset) {
        teleportAllPlayersToMap(map, zOffset, false);
    }

    public void teleportAllPlayersToMap(MapConfig map, int zOffset, boolean swapped) {
        ServerLevel target = getMapWorld(map);
        if (target == null) return;
        TeamManager tm = PWP.getTeamManager();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (tm.getOrCreatePlayerData(p.getUUID()).getTeam() == Team.SPECTATOR) continue;
            Team team = tm.getOrCreatePlayerData(p.getUUID()).getTeam();
            Team spawnTeam = swapped ? (team == Team.NATO ? Team.RUSSIA : Team.NATO) : team;
            double x = map.getWorldBorderCenterX() + 0.5;
            double y = 65;
            double z = map.getWorldBorderCenterZ() + 0.5 + zOffset;
            if (map.hasTeamSpawns()) {
                int[] spawn = spawnTeam == Team.NATO ? map.getNatoSpawn() : map.getRussiaSpawn();
                if (spawn != null && spawn.length >= 3) {
                    x = spawn[0] + 0.5; y = spawn[1]; z = spawn[2] + 0.5 + zOffset;
                }
            }
            safeTeleport(p, target, x, y, z);
        }
    }

    public boolean isInOwnBase(ServerPlayer player) {
        if (currentMap == null || !currentMap.hasTeamSpawns()) return false;
        TeamManager tm = PWP.getTeamManager();
        if (tm == null) return false;
        Team team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        if (!team.isPlayable()) return false;
        int[] spawn = team == Team.NATO ? currentMap.getNatoSpawn() : currentMap.getRussiaSpawn();
        if (spawn == null) return false;
        int radius = currentMap.getBaseRadius();
        double dx = player.getX() - spawn[0];
        double dz = player.getZ() - spawn[2];
        return dx * dx + dz * dz <= radius * radius;
    }

    public boolean isInAnyBase(BlockPos pos) {
        if (currentMap == null || !currentMap.hasTeamSpawns()) return false;
        int radius = currentMap.getBaseRadius();
        int radiusSq = radius * radius;
        int[] nato = currentMap.getNatoSpawn();
        if (nato != null) {
            double dx = pos.getX() - nato[0];
            double dz = pos.getZ() - nato[2];
            if (dx * dx + dz * dz <= radiusSq) return true;
        }
        int[] russia = currentMap.getRussiaSpawn();
        if (russia != null) {
            double dx = pos.getX() - russia[0];
            double dz = pos.getZ() - russia[2];
            if (dx * dx + dz * dz <= radiusSq) return true;
        }
        return false;
    }

    private BlockPos getLobbySpawn(ServerLevel level) {
        if (ProxyMessenger.isMatchServer()) {
            return level.getSharedSpawnPos();
        }
        return LOBBY_SPAWN;
    }

    public void teleportAllToLobby() {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) {
            BlockPos spawn = getLobbySpawn(l);
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                safeTeleport(p, l, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
    }

    public void teleportPlayerToLobby(ServerPlayer p) {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) {
            BlockPos spawn = getLobbySpawn(l);
            safeTeleport(p, l, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
    }

    public void setLobbyRespawn(ServerPlayer p) {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) {
            BlockPos spawn = getLobbySpawn(l);
            p.setRespawnPosition(l.dimension(), spawn, 0, false, false);
        }
    }

    public void setLobbyRespawnAtPlayer(ServerPlayer p) {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) p.setRespawnPosition(l.dimension(), p.blockPosition(), p.getYRot(), false, false);
    }

    private boolean isSwapped() {
        return currentMap != null && currentMap.isTeamRotation() && (currentMatchIndex % 2 == 1);
    }

    public void teleportPlayerToMapAtTeamSpawn(ServerPlayer p, Team team) {
        if (currentMap == null) return;
        int zOffset = 0;
        boolean swapped = isSwapped();
        Team spawnTeam = swapped ? (team == Team.NATO ? Team.RUSSIA : Team.NATO) : team;
        ServerLevel target = getMapWorld(currentMap);
        if (target == null) return;
        double x = currentMap.getWorldBorderCenterX() + 0.5;
        double y = 65;
        double z = currentMap.getWorldBorderCenterZ() + 0.5 + zOffset;
        if (currentMap.hasTeamSpawns()) {
            int[] spawn = spawnTeam == Team.NATO ? currentMap.getNatoSpawn() : currentMap.getRussiaSpawn();
            if (spawn != null && spawn.length >= 3) {
                x = spawn[0] + 0.5; y = spawn[1]; z = spawn[2] + 0.5 + zOffset;
            }
        }
        safeTeleport(p, target, x, y, z);
        com.pigeostudios.pwp.events.CombatEventHandler.applySpawnInvulnerability(p);
    }

    public void setMapRespawn(ServerPlayer p, Team team) {
        if (currentMap == null) return;
        int zOffset = 0;
        boolean swapped = isSwapped();
        Team spawnTeam = swapped ? (team == Team.NATO ? Team.RUSSIA : Team.NATO) : team;
        ServerLevel target = getMapWorld(currentMap);
        if (target == null) return;
        int x = currentMap.getWorldBorderCenterX();
        int y = 65;
        int z = currentMap.getWorldBorderCenterZ() + zOffset;
        if (currentMap.hasTeamSpawns()) {
            int[] spawn = spawnTeam == Team.NATO ? currentMap.getNatoSpawn() : currentMap.getRussiaSpawn();
            if (spawn != null && spawn.length >= 3) {
                x = spawn[0]; y = spawn[1]; z = spawn[2] + zOffset;
            }
        }
        p.setRespawnPosition(target.dimension(), new BlockPos(x, y, z), 0, false, false);
    }

    private void unloadChunksOutsideZone(ServerLevel level, int zOffset) {
        level.getChunkSource().tick(() -> true, true);
        level.getChunkSource().tick(() -> true, true);
    }

    private void clearEntitiesInZone(ServerLevel level, int zOffset) {
        int halfSize = 1024;
        AABB zone = new AABB(-halfSize, -64, zOffset - halfSize, halfSize, 320, zOffset + halfSize);
        List<Entity> toRemove = new java.util.ArrayList<>();
        for (Entity e : level.getEntities().getAll()) {
            if (e != null && !(e instanceof ServerPlayer) && !(e instanceof Player)) {
                if (zone.contains(e.getX(), e.getY(), e.getZ())) {
                    toRemove.add(e);
                }
            }
        }
        for (Entity e : toRemove) e.discard();
        if (!toRemove.isEmpty())
            PWP.LOGGER.info("Cleared {} entities in map zone zOffset={}", toRemove.size(), zOffset);
    }

    private void safeTeleport(ServerPlayer p, ServerLevel target, double x, double y, double z) {
        p.teleportTo(target, x, y, z, 0, 0);
        p.fallDistance = 0;
        p.setNoGravity(false);
        p.hurtMarked = true;
    }

    // ========== World Resolution ==========

    private ServerLevel getOrCreateLobbyWorld() {
        return server.overworld();
    }

    private ServerLevel getMapWorld(MapConfig map) {
        ServerLevel l = server.getLevel(DynamicDimensionManager.getDimKey());
        return l != null ? l : server.overworld();
    }

    private ServerLevel getLobbyWorld() {
        return server.overworld();
    }

    // ========== Broadcast ==========

    private void broadcast(String msg, ChatFormatting... styles) {
        net.minecraft.network.chat.MutableComponent c = Component.literal(msg);
        c.withStyle(styles);
        server.getPlayerList().broadcastSystemMessage(c, false);
    }

    private void sendNotificationToAll(String text, String type, int durationMs) {
        NotificationPacket pkt = new NotificationPacket(text, type, durationMs, "");
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
    }

    private void sendNotificationToPlayer(ServerPlayer player, String text, String type, int durationMs) {
        NotificationPacket pkt = new NotificationPacket(text, type, durationMs, "");
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);
    }

    // ========== Networking ==========

    public void syncPhaseToAll() {
        GameStateSyncPacket pkt = new GameStateSyncPacket(
            currentPhase.ordinal(),
            currentMap != null ? currentMap.getName() : "",
            winningTeam != null ? winningTeam.ordinal() : -1,
            lobbyStatus);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);

        if (currentPhase == GamePhase.LOBBY || currentPhase == GamePhase.VOTING) {
            TeamManager tm = PWP.getTeamManager();
            if (tm != null) {
                OpenTeamSelectionScreenPacket openPkt = new OpenTeamSelectionScreenPacket();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (tm.getOrCreatePlayerData(p.getUUID()).getTeam() == Team.SPECTATOR)
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), openPkt);
                }
            }
        }
    }

    public void syncPhaseToPlayer(ServerPlayer p) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
            new GameStateSyncPacket(currentPhase.ordinal(),
                currentMap != null ? currentMap.getName() : "",
                winningTeam != null ? winningTeam.ordinal() : -1,
                lobbyStatus));
    }

    private void broadcastVoiceSpeakers() {
        if (server == null) return;
        TeamVoicePlugin.cleanupStaleSpeakers();
        var activeSpeakers = TeamVoicePlugin.activeSpeakers;
        if (activeSpeakers.isEmpty()) return;

        TeamManager tm = PWP.getTeamManager();
        if (tm == null) return;

        for (Map.Entry<UUID, TeamVoicePlugin.VoiceSpeakerState> entry : activeSpeakers.entrySet()) {
            TeamVoicePlugin.VoiceSpeakerState state = entry.getValue();
            Team speakerTeam = tm.getOrCreatePlayerData(entry.getKey()).getTeam();
            if (!speakerTeam.isPlayable()) continue;

            VoiceSpeakingStatePacket pkt = new VoiceSpeakingStatePacket(entry.getKey(), state.name, state.channel);
            for (ServerPlayer listener : server.getPlayerList().getPlayers()) {
                Team listenerTeam = tm.getOrCreatePlayerData(listener.getUUID()).getTeam();
                if (listenerTeam != speakerTeam) continue;
                PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> listener), pkt);
            }
        }
    }

}
