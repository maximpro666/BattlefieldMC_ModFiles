package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.GameStateSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;

public class GameManager {
    public enum GamePhase {
        LOBBY,
        VOTING,
        PLAYING,
        ENDING
    }

    private static final ResourceLocation LOBBY_DIMENSION_ID = new ResourceLocation("teamsystem:lobby");
    private static final BlockPos LOBBY_SPAWN = new BlockPos(0, 64, 0);
    private static final int VOTE_SECONDS = 20;
    private static final int COUNTDOWN_SECONDS = 15;
    private static final int OVERTIME_TICKETS = 1;
    private static final int MAX_OVERTIME_TICKS = 20 * 60;

    private final MinecraftServer server;
    private GamePhase currentPhase;
    private int phaseTimer;
    private Team winningTeam;
    private boolean lobbyLoaded;
    private MapConfig currentMap;
    private boolean overtime;
    private int overtimeTicks;
    private int captureTicks;

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
    }

    public GamePhase getCurrentPhase() { return currentPhase; }
    public MinecraftServer getServer() { return server; }
    public Team getWinningTeam() { return winningTeam; }
    public boolean isPlaying() { return currentPhase == GamePhase.PLAYING; }
    public boolean isVoting() { return currentPhase == GamePhase.VOTING; }
    public boolean isLobby() { return currentPhase == GamePhase.LOBBY; }
    public boolean isOvertime() { return overtime; }
    public MapConfig getCurrentMap() { return currentMap; }

    // ========== Match Flow ==========

    public void startGame() {
        TeamManager teamManager = TeamSystem.getTeamManager();
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();

        if (!mapPool.hasAvailableMaps()) {
            broadcast("No available maps! Use /map maintenance or wait for maintenance cycle.", ChatFormatting.RED);
            return;
        }

        MapConfig map = mapPool.getCurrentMap().orElse(null);
        if (map == null || map.getState() != MapState.IN_MATCH) {
            map = mapPool.pickNextAvailable();
            if (map == null) return;
        }

        currentPhase = GamePhase.PLAYING;
        phaseTimer = 0;
        winningTeam = null;
        currentMap = map;
        overtime = false;
        overtimeTicks = 0;

        teamManager.resetTickets();
        teamManager.setTickets(Team.NATO, map.getTickets());
        teamManager.setTickets(Team.RUSSIA, map.getTickets());

        EconomyManager econ = TeamSystem.getEconomyManager();
        if (econ != null) {
            econ.resetAllSPForMatch();
            econ.syncSPToAll();
        }

        CapturePointManager cp = TeamSystem.getCapturePointManager();
        if (cp != null && map.hasCapturePoints()) {
            cp.loadFromMapConfig(map);
        } else if (cp != null) {
            cp.clearPoints();
        }

        ContributionManager contrib = TeamSystem.getContributionManager();
        if (contrib != null) contrib.resetMatch();

        try {
            TeamSystem.getMapPoolManager().copyToLive(map);
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to copy map {} from source: {}", map.getName(), e.getMessage());
        }
        useMapWorld(map);
        applyMapConfig(map);
        teleportAllPlayersToMap(map);

        gamerule(server.overworld(), "liberateAttachment", "true");

        broadcast("Game started on map: " + map.getName(), ChatFormatting.GREEN, ChatFormatting.BOLD);
        syncPhaseToAll();
        TeamSystem.LOGGER.info("Game started on map: {}", map.getName());
    }

    public void endGame(Team winner) {
        if (currentPhase != GamePhase.PLAYING) return;

        currentPhase = GamePhase.ENDING;
        winningTeam = winner;
        phaseTimer = 80;

        broadcast("=== GAME OVER ===", ChatFormatting.GOLD, ChatFormatting.BOLD);
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("Team ").withStyle(ChatFormatting.WHITE)
                .append(winner.getColoredName())
                .append(Component.literal(" wins!").withStyle(ChatFormatting.WHITE)), false);

        EconomyManager econ = TeamSystem.getEconomyManager();
        if (econ != null) {
            TeamManager tm = TeamSystem.getTeamManager();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                int baseSP = 50;
                int baseBC = 25;
                if (tm.getOrCreatePlayerData(p.getUUID()).getTeam() == winner) {
                    baseSP += 50;
                    baseBC += 50;
                }
                econ.addSP(p.getUUID(), baseSP);
                econ.addBC(p.getUUID(), baseBC);
                PlayerCombatData pcd = tm.getOrCreatePlayerData(p.getUUID());
                pcd.addScorePoints(baseSP);
                pcd.addBattleCredits(baseBC);
                econ.syncAll(p);
                p.sendSystemMessage(Component.literal("§6+ " + baseBC + " BC, + " + baseSP + " SP").withStyle(ChatFormatting.GOLD));
            }
            tm.setDirty();
        }

        captureTicks = 0;
        overtime = false;
        overtimeTicks = 0;

        syncPhaseToAll();
        TeamSystem.LOGGER.info("Game ended. Winner: {}", winner.getName());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (currentPhase == GamePhase.ENDING) {
            phaseTimer--;
            if (phaseTimer <= 0) finishMatchCycle();
        }

        if (currentPhase == GamePhase.VOTING) {
            phaseTimer--;
            if (phaseTimer <= 0) resolveVoting();

            if (phaseTimer % 20 == 0 && phaseTimer > 0) {
                int sec = phaseTimer / 20;
                if (sec <= 5 || sec % 5 == 0) {
                    broadcast("Voting ends in " + sec + "s. Vote: /map vote <name>", ChatFormatting.AQUA);
                }
            }
        }

        if (currentPhase == GamePhase.LOBBY && phaseTimer > 0) {
            phaseTimer--;
            if (phaseTimer % 20 == 0 && phaseTimer > 0) {
                int sec = phaseTimer / 20;
                if (sec <= 5 || sec % 10 == 0) {
                    broadcast("Game starts in " + sec + "s", ChatFormatting.AQUA);
                }
            }
            if (phaseTimer <= 0) startGame();
        }

        if (currentPhase == GamePhase.PLAYING) {
            captureTicks++;
            if (captureTicks % 20 == 0) {
                CapturePointManager cp = TeamSystem.getCapturePointManager();
                if (cp != null) {
                    MapConfig map = currentMap;
                    if (map != null) {
                        ServerLevel level = getMapWorld(map);
                        if (level != null) cp.tickCapturePoints(level);
                    }
                }
                TicketManager tm = TeamSystem.getTicketManager();
                if (tm != null) tm.tick();

                if (!overtime) {
                    int natoTickets = tm != null ? tm.getTickets(Team.NATO) : 0;
                    int russiaTickets = tm != null ? tm.getTickets(Team.RUSSIA) : 0;
                    if (natoTickets <= OVERTIME_TICKETS || russiaTickets <= OVERTIME_TICKETS) {
                        if (natoTickets > 0 && russiaTickets > 0) {
                            overtime = true;
                            overtimeTicks = 0;
                            broadcast("OVERTIME!", ChatFormatting.GOLD, ChatFormatting.BOLD);
                        }
                    }
                } else {
                    overtimeTicks++;
                    if (overtimeTicks >= MAX_OVERTIME_TICKS) {
                        int natoTk = tm != null ? tm.getTickets(Team.NATO) : 0;
                        int russiaTk = tm != null ? tm.getTickets(Team.RUSSIA) : 0;
                        Team winner = natoTk >= russiaTk ? Team.NATO : Team.RUSSIA;
                        endGame(winner);
                    }
                }
            }
        }
    }

    // ========== Match Lifecycle ==========

    private void finishMatchCycle() {
        MapConfig map = currentMap;
        teleportAllToLobby();

        MapPoolManager pool = TeamSystem.getMapPoolManager();

        if (map != null) {
            pool.markDirty(map);
            broadcast("Map '" + map.getName() + "' marked dirty", ChatFormatting.GRAY);
        }

        // If all maps are dirty → run maintenance with restart
        if (!pool.hasAvailableMaps() && pool.getDirtyCount() > 0) {
            broadcast("All maps used! Running maintenance... server will restart.", ChatFormatting.YELLOW);
            pool.runMaintenance();
            return;
        }

        startVoting();
    }

    private void startVoting() {
        currentPhase = GamePhase.VOTING;
        phaseTimer = VOTE_SECONDS * 20;
        winningTeam = null;
        currentMap = null;

        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.clearVotes();

        List<MapConfig> available = pool.getMapsByState(MapState.AVAILABLE);
        if (available.isEmpty()) {
            broadcast("No available maps! Use /map maintenance", ChatFormatting.RED);
            currentPhase = GamePhase.LOBBY;
            phaseTimer = 0;
            returnToLobby();
            return;
        }

        broadcast("=== VOTE FOR NEXT MAP ===", ChatFormatting.GOLD, ChatFormatting.BOLD);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < available.size(); i++) {
            sb.append("\n").append(i + 1).append(". ").append(available.get(i).getName());
        }
        broadcast(sb.toString(), ChatFormatting.YELLOW);
        broadcast("Use /map vote <name> to vote!", ChatFormatting.GREEN);

        applyLobbyRules();
        syncPhaseToAll();
        TeamSystem.LOGGER.info("Voting started. {} maps available", available.size());
    }

    private void resolveVoting() {
        MapPoolManager pool = TeamSystem.getMapPoolManager();

        String winner = pool.resolveVoteWinner();
        if (winner != null && pool.selectMap(winner)) {
            broadcast("Map '" + winner + "' won the vote!", ChatFormatting.GOLD);
        } else {
            MapConfig picked = pool.pickNextAvailable();
            if (picked == null) {
                broadcast("No available maps!", ChatFormatting.RED);
                startVoting();
                return;
            }
            broadcast("Auto-selected map: " + picked.getName(), ChatFormatting.YELLOW);
        }

        pool.clearVotes();
        currentPhase = GamePhase.LOBBY;
        phaseTimer = COUNTDOWN_SECONDS * 20;
        broadcast("Game starts in " + COUNTDOWN_SECONDS + "s on " + pool.getCurrentMap().map(MapConfig::getName).orElse("?") + "!", ChatFormatting.GREEN);
    }

    private void returnToLobby() {
        currentPhase = GamePhase.LOBBY;
        phaseTimer = COUNTDOWN_SECONDS * 20;
        winningTeam = null;
        currentMap = null;
        teleportAllToLobby();
        applyLobbyRules();
        syncPhaseToAll();

        MapPoolManager pool = TeamSystem.getMapPoolManager();
        if (pool.hasAvailableMaps()) {
            pool.pickNextAvailable();
        }
        broadcast("Game starts in " + COUNTDOWN_SECONDS + "s", ChatFormatting.GREEN);
        TeamSystem.LOGGER.info("Returned to lobby");
    }

    // ========== Voting API ==========

    public boolean voteMap(ServerPlayer player, String mapName) {
        if (currentPhase != GamePhase.VOTING) {
            player.sendSystemMessage(Component.literal("Not in voting phase!").withStyle(ChatFormatting.RED));
            return false;
        }
        MapPoolManager pool = TeamSystem.getMapPoolManager();

        boolean found = pool.getMapsByState(MapState.AVAILABLE).stream()
            .anyMatch(m -> m.getName().equalsIgnoreCase(mapName));
        if (!found) {
            player.sendSystemMessage(Component.literal("Map not available: " + mapName).withStyle(ChatFormatting.RED));
            return false;
        }

        return pool.castVote(player, mapName);
    }

    // ========== Timer Overrides ==========

    public void startCountdown(int seconds) {
        if (currentPhase != GamePhase.VOTING && currentPhase != GamePhase.LOBBY) return;
        phaseTimer = seconds * 20;
        broadcast("Countdown set to " + seconds + "s", ChatFormatting.GREEN);
    }

    public void cancelCountdown() {
        phaseTimer = 0;
        broadcast("Countdown cancelled.", ChatFormatting.RED);
    }

    // ========== World ==========

    private void useMapWorld(MapConfig map) {
        ServerLevel level = getMapWorldNoFallback(map);
        if (level != null) clearWorldEntities(level);
    }

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
        if (removed > 0) TeamSystem.LOGGER.info("Removed {} entities", removed);
    }

    // ========== Rules ==========

    private void applyLobbyRules() {
        ServerLevel l = getLobbyWorld();
        if (l == null) return;
        gamerule(l, "doMobSpawning", "false");
        gamerule(l, "doDaylightCycle", "false");
        gamerule(l, "doWeatherCycle", "false");
        gamerule(l, "mobGriefing", "false");
        gamerule(l, "doFireTick", "false");
        gamerule(l, "keepInventory", "true");
        gamerule(l, "naturalRegeneration", "true");
        gamerule(l, "fallDamage", "false");
    }

    private void applyMapConfig(MapConfig map) {
        ServerLevel l = getMapWorld(map);
        if (l == null) return;
        gamerule(l, "naturalRegeneration", map.hasRegen() ? "true" : "false");
        gamerule(l, "doImmediateRespawn", map.hasRespawn() ? "false" : "true");
        gamerule(l, "doMobSpawning", "true");
        gamerule(l, "mobGriefing", "false");
        gamerule(l, "doFireTick", "true");
        gamerule(l, "keepInventory", "false");
        gamerule(l, "fallDamage", "true");
        if (map.hasWorldBorder()) {
            l.getWorldBorder().setCenter(map.getWorldBorderCenterX(), map.getWorldBorderCenterZ());
            l.getWorldBorder().setSize(map.getWorldBorderSize());
        } else {
            l.getWorldBorder().setSize(6.0E7);
        }
    }

    private void gamerule(ServerLevel l, String rule, String val) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule " + rule + " " + val);
    }

    // ========== Teleport ==========

    public void teleportAllPlayersToMap(MapConfig map) {
        ServerLevel target = getMapWorld(map);
        if (target == null) return;
        TeamManager tm = TeamSystem.getTeamManager();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (tm.getOrCreatePlayerData(p.getUUID()).getTeam() == Team.SPECTATOR) continue;
            safeTeleport(p, target, 0.5, 65, 0.5);
        }
    }

    public void teleportAllToLobby() {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null)
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                safeTeleport(p, l, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5);
    }

    public void teleportPlayerToLobby(ServerPlayer p) {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) safeTeleport(p, l, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5);
    }

    public void setLobbyRespawn(ServerPlayer p) {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) p.setRespawnPosition(l.dimension(), LOBBY_SPAWN, 0, false, false);
    }

    public void setLobbyRespawnAtPlayer(ServerPlayer p) {
        ServerLevel l = getOrCreateLobbyWorld();
        if (l != null) p.setRespawnPosition(l.dimension(), p.blockPosition(), p.getYRot(), false, false);
    }

    private void safeTeleport(ServerPlayer p, ServerLevel target, double x, double y, double z) {
        p.teleportTo(target, x, y, z, 0, 0);
        p.fallDistance = 0;
        p.setNoGravity(false);
        p.hurtMarked = true;
    }

    // ========== World Resolution ==========

    private ServerLevel getOrCreateLobbyWorld() {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID);
        ServerLevel w = server.getLevel(key);
        if (w == null && !lobbyLoaded) {
            lobbyLoaded = true;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "execute in teamsystem:lobby run say Lobby loaded");
            w = server.getLevel(key);
        }
        return w != null ? w : server.overworld();
    }

    private boolean forceLoading = false;

    private ServerLevel getMapWorldNoFallback(MapConfig map) {
        String worldKey = MapConfig.sanitizeToResourcePath(map.getWorldFolder());
        if (worldKey.isEmpty() || worldKey.equals("overworld")) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("teamsystem", worldKey));
        ServerLevel w = server.getLevel(key);
        if (w == null && !forceLoading) {
            forceLoading = true;
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "execute in teamsystem:" + worldKey + " run say Loading dimension " + worldKey);
            w = server.getLevel(key);
            forceLoading = false;
        }
        return w;
    }

    private ServerLevel getMapWorld(MapConfig map) {
        ServerLevel l = getMapWorldNoFallback(map);
        return l != null ? l : server.overworld();
    }

    private ServerLevel getLobbyWorld() {
        ServerLevel w = server.getLevel(ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID));
        return w != null ? w : server.overworld();
    }

    // ========== Broadcast ==========

    private void broadcast(String msg, ChatFormatting... styles) {
        net.minecraft.network.chat.MutableComponent c = Component.literal(msg);
        c.withStyle(styles);
        server.getPlayerList().broadcastSystemMessage(c, false);
    }

    // ========== Networking ==========

    public void syncPhaseToAll() {
        GameStateSyncPacket pkt = new GameStateSyncPacket(
            currentPhase.ordinal(),
            currentMap != null ? currentMap.getName() : "",
            winningTeam != null ? winningTeam.ordinal() : -1);
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }

    public void syncPhaseToPlayer(ServerPlayer p) {
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
            new GameStateSyncPacket(currentPhase.ordinal(),
                currentMap != null ? currentMap.getName() : "",
                winningTeam != null ? winningTeam.ordinal() : -1));
    }

}
