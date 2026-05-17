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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class GameManager {
    public enum GamePhase {
        LOBBY,
        PLAYING,
        ENDING
    }

    private static final ResourceLocation LOBBY_DIMENSION_ID = new ResourceLocation("teamsystem:lobby");
    private static final BlockPos LOBBY_SPAWN = new BlockPos(0, 64, 0);

    private final MinecraftServer server;
    private GamePhase currentPhase;
    private int endingTimer;
    private Team winningTeam;
    private int lobbyWaitTimer;
    private boolean countdownActive;
    private boolean lobbyLoaded;
    private MapConfig currentMap;

    public GameManager(MinecraftServer server) {
        this.server = server;
        this.currentPhase = GamePhase.LOBBY;
        this.endingTimer = 0;
        this.winningTeam = null;
        this.lobbyWaitTimer = 0;
        this.countdownActive = false;
        this.lobbyLoaded = false;
        this.currentMap = null;
    }

    public GamePhase getCurrentPhase() { return currentPhase; }
    public MinecraftServer getServer() { return server; }
    public Team getWinningTeam() { return winningTeam; }
    public boolean isPlaying() { return currentPhase == GamePhase.PLAYING; }
    public boolean isLobby() { return currentPhase == GamePhase.LOBBY; }

    // ========== Match Flow ==========

    public void startGame() {
        TeamManager teamManager = TeamSystem.getTeamManager();
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();

        if (!mapPool.hasAvailableMaps()) {
            broadcastToAll(Component.literal("No available maps! Use /map maintenance or wait for maintenance cycle.")
                .withStyle(ChatFormatting.RED));
            return;
        }

        MapConfig map = mapPool.getCurrentMap().orElse(null);
        if (map == null || map.getState() != MapState.IN_MATCH) {
            map = mapPool.pickNextAvailable();
            if (map == null) return;
        }

        currentPhase = GamePhase.PLAYING;
        winningTeam = null;
        currentMap = map;

        teamManager.resetTickets();
        teamManager.setTickets(Team.NATO, map.getTickets());
        teamManager.setTickets(Team.RUSSIA, map.getTickets());

        backupOnce(map);
        useMapWorld(map);
        applyMapConfig(map);
        teleportAllPlayersToMap(map);

        broadcastToAll(Component.literal("Game started on map: ")
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            .append(Component.literal(map.getName()).withStyle(ChatFormatting.YELLOW)));

        syncPhaseToAll();
        TeamSystem.LOGGER.info("Game started on map: {}", map.getName());
    }

    public void endGame(Team winner) {
        if (currentPhase != GamePhase.PLAYING) return;

        currentPhase = GamePhase.ENDING;
        winningTeam = winner;
        endingTimer = 100;

        broadcastToAll(Component.literal("=== GAME OVER ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        broadcastToAll(Component.literal("Team ").withStyle(ChatFormatting.WHITE)
            .append(winner.getColoredName())
            .append(Component.literal(" wins!").withStyle(ChatFormatting.WHITE)));

        syncPhaseToAll();
        TeamSystem.LOGGER.info("Game ended. Winner: {}", winner.getName());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (currentPhase == GamePhase.ENDING) {
            endingTimer--;
            if (endingTimer <= 0) {
                finishMatchCycle();
            }
        }

        if (currentPhase == GamePhase.LOBBY && countdownActive) {
            lobbyWaitTimer--;
            if (lobbyWaitTimer % 20 == 0 && lobbyWaitTimer > 0) {
                int seconds = lobbyWaitTimer / 20;
                if (seconds <= 5 || seconds % 10 == 0) {
                    broadcastToAll(Component.literal("Next round in " + seconds + " seconds...")
                        .withStyle(ChatFormatting.AQUA));
                }
            }
            if (lobbyWaitTimer <= 0) {
                countdownActive = false;
                startGame();
            }
        }
    }

    // ========== Match Lifecycle ==========

    private void finishMatchCycle() {
        MapConfig map = currentMap;

        // 1. Teleport all players to lobby
        teleportAllToLobby();

        // 2. Mark map DIRTY — removed from rotation, NOT regenerated
        if (map != null) {
            TeamSystem.getMapPoolManager().markDirty(map);
            broadcastToAll(Component.literal("Map '" + map.getName() + "' marked dirty (will be restored during maintenance)")
                .withStyle(ChatFormatting.GRAY));
        }

        // 3. Return to lobby with next available map
        returnToLobby();
    }

    private void returnToLobby() {
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();

        // Use vote winner if available, otherwise auto-pick
        String voted = mapPool.resolveVoteWinner();
        if (voted != null) {
            mapPool.selectMap(voted);
            broadcastToAll(Component.literal("Map '" + voted + "' won the vote!").withStyle(ChatFormatting.GOLD));
        } else if (mapPool.hasAvailableMaps()) {
            MapConfig next = mapPool.pickNextAvailable();
            if (next != null) {
                broadcastToAll(Component.literal("Next map: ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(next.getName()).withStyle(ChatFormatting.YELLOW)));
            }
        } else {
            broadcastToAll(Component.literal("No available maps! Maintenance may be required.")
                .withStyle(ChatFormatting.RED));
        }
        mapPool.clearVotes();

        currentPhase = GamePhase.LOBBY;
        winningTeam = null;
        endingTimer = 0;
        currentMap = null;

        teleportAllToLobby();
        applyLobbyRules();
        syncPhaseToAll();

        int waitTime = 30;
        startCountdown(waitTime);
        TeamSystem.LOGGER.info("Returned to lobby");
    }

    // ========== Voting ==========

    public void voteMap(ServerPlayer player, String mapName) {
        TeamSystem.getMapPoolManager().castVote(player, mapName);
    }

    // ========== Timer ==========

    public void startCountdown(int seconds) {
        if (currentPhase != GamePhase.LOBBY) return;
        lobbyWaitTimer = seconds * 20;
        countdownActive = true;
        broadcastToAll(Component.literal("Game starting in " + seconds + " seconds...")
            .withStyle(ChatFormatting.GREEN));
    }

    public void cancelCountdown() {
        countdownActive = false;
        lobbyWaitTimer = 0;
        broadcastToAll(Component.literal("Countdown cancelled.").withStyle(ChatFormatting.RED));
    }

    // ========== Backup ==========

    private void backupOnce(MapConfig map) {
        Path backupRegionDir = getBackupRegionDir(map);
        if (Files.isDirectory(backupRegionDir)) return;
        forceBackup(map);
    }

    public void forceBackup(MapConfig map) {
        ServerLevel level = getMapWorld(map);
        if (level == null) return;

        TeamSystem.LOGGER.info("Backing up map world: {}", map.getName());
        level.save(null, false, false);

        try {
            Path dimDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions").resolve("teamsystem").resolve(map.getWorldFolder());
            Path backupDir = dimDir.resolveSibling(map.getWorldFolder() + "_backup");
            Path regionDir = dimDir.resolve("region");

            if (!Files.isDirectory(regionDir)) return;

            Files.createDirectories(backupDir.resolve("region"));
            try (var files = Files.list(regionDir)) {
                for (Path src : (Iterable<Path>) files::iterator) {
                    Files.copy(src, backupDir.resolve("region").resolve(src.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
                }
            }

            backupSubDir(backupDir, dimDir, "poi");
            backupSubDir(backupDir, dimDir, "entities");
            backupSubDir(backupDir, dimDir, "data");

            TeamSystem.LOGGER.info("Backup created for map: {}", map.getName());
        } catch (java.io.IOException e) {
            TeamSystem.LOGGER.error("Failed to backup map {}: {}", map.getName(), e.getMessage());
        }
    }

    private void backupSubDir(Path backupDir, Path dimDir, String sub) throws java.io.IOException {
        Path src = dimDir.resolve(sub);
        if (!Files.isDirectory(src)) return;
        Path dst = backupDir.resolve(sub);
        Files.createDirectories(dst);
        try (var files = Files.list(src)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                Files.copy(f, dst.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path getBackupRegionDir(MapConfig map) {
        Path dimDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem").resolve(map.getWorldFolder());
        return dimDir.resolveSibling(map.getWorldFolder() + "_backup").resolve("region");
    }

    // ========== World Management ==========

    private void useMapWorld(MapConfig map) {
        ServerLevel level = getMapWorldNoFallback(map);
        if (level != null) clearWorldEntities(level);
    }

    private void clearWorldEntities(ServerLevel level) {
        List<Entity> all = new ArrayList<>();
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
        ServerLevel lobby = getLobbyWorld();
        if (lobby == null) return;
        setGameRule(lobby, "doMobSpawning", "false");
        setGameRule(lobby, "doDaylightCycle", "false");
        setGameRule(lobby, "doWeatherCycle", "false");
        setGameRule(lobby, "mobGriefing", "false");
        setGameRule(lobby, "doFireTick", "false");
        setGameRule(lobby, "keepInventory", "true");
        setGameRule(lobby, "naturalRegeneration", "true");
        setGameRule(lobby, "fallDamage", "false");
    }

    private void applyMapConfig(MapConfig map) {
        ServerLevel level = getMapWorld(map);
        if (level == null) return;

        setGameRule(level, "naturalRegeneration", map.hasRegen() ? "true" : "false");
        setGameRule(level, "doImmediateRespawn", map.hasRespawn() ? "false" : "true");
        setGameRule(level, "doMobSpawning", "true");
        setGameRule(level, "mobGriefing", "false");
        setGameRule(level, "doFireTick", "true");
        setGameRule(level, "keepInventory", "false");
        setGameRule(level, "fallDamage", "true");

        if (map.hasWorldBorder()) {
            level.getWorldBorder().setCenter(map.getWorldBorderCenterX(), map.getWorldBorderCenterZ());
            level.getWorldBorder().setSize(map.getWorldBorderSize());
        } else {
            level.getWorldBorder().setSize(6.0E7);
        }
    }

    private void setGameRule(ServerLevel level, String rule, String value) {
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(), "gamerule " + rule + " " + value);
    }

    // ========== Teleport ==========

    public void teleportAllPlayersToMap(MapConfig map) {
        ServerLevel target = getMapWorld(map);
        if (target == null) return;

        TeamManager tm = TeamSystem.getTeamManager();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (team == Team.SPECTATOR) continue;
            safeTeleport(player, target, 0.5, 65, 0.5);
        }
    }

    public void teleportAllToLobby() {
        ServerLevel lobby = getOrCreateLobbyWorld();
        if (lobby == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            safeTeleport(player, lobby, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5);
        }
    }

    public void teleportPlayerToLobby(ServerPlayer player) {
        ServerLevel lobby = getOrCreateLobbyWorld();
        if (lobby == null) return;
        safeTeleport(player, lobby, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5);
    }

    public void setLobbyRespawn(ServerPlayer player) {
        ServerLevel lobby = getOrCreateLobbyWorld();
        if (lobby != null)
            player.setRespawnPosition(lobby.dimension(), LOBBY_SPAWN, 0, false, false);
    }

    public void setLobbyRespawnAtPlayer(ServerPlayer player) {
        ServerLevel lobby = getOrCreateLobbyWorld();
        if (lobby != null)
            player.setRespawnPosition(lobby.dimension(), player.blockPosition(), player.getYRot(), false, false);
    }

    private void safeTeleport(ServerPlayer player, ServerLevel target, double x, double y, double z) {
        player.teleportTo(target, x, y, z, 0, 0);
        player.fallDistance = 0;
        player.setNoGravity(false);
        player.hurtMarked = true;
    }

    // ========== World Resolution ==========

    private ServerLevel getOrCreateLobbyWorld() {
        ResourceKey<Level> lobbyKey = ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID);
        ServerLevel world = server.getLevel(lobbyKey);
        if (world == null && !lobbyLoaded) {
            lobbyLoaded = true;
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "execute in teamsystem:lobby run say Lobby loaded");
            world = server.getLevel(lobbyKey);
        }
        return world != null ? world : server.overworld();
    }

    private ServerLevel getMapWorldNoFallback(MapConfig map) {
        if (map.getWorldFolder() == null || map.getWorldFolder().isEmpty() || map.getWorldFolder().equals("overworld"))
            return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("teamsystem", map.getWorldFolder()));
        return server.getLevel(key);
    }

    private ServerLevel getMapWorld(MapConfig map) {
        ServerLevel level = getMapWorldNoFallback(map);
        return level != null ? level : server.overworld();
    }

    private ServerLevel getLobbyWorld() {
        ResourceKey<Level> lobbyKey = ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID);
        ServerLevel world = server.getLevel(lobbyKey);
        return world != null ? world : server.overworld();
    }

    // ========== Networking ==========

    private void broadcastToAll(Component msg) {
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    public void syncPhaseToAll() {
        GameStateSyncPacket packet = new GameStateSyncPacket(
            currentPhase.ordinal(),
            currentMap != null ? currentMap.getName() : "",
            winningTeam != null ? winningTeam.ordinal() : -1
        );
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public void syncPhaseToPlayer(ServerPlayer player) {
        GameStateSyncPacket packet = new GameStateSyncPacket(
            currentPhase.ordinal(),
            currentMap != null ? currentMap.getName() : "",
            winningTeam != null ? winningTeam.ordinal() : -1
        );
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // ========== Legacy compat ==========

    public void reBackupCurrentMap() {
        if (currentMap != null) forceBackup(currentMap);
    }
}
