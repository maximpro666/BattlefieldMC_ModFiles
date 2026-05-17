package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.GameStateSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    public GameManager(MinecraftServer server) {
        this.server = server;
        this.currentPhase = GamePhase.LOBBY;
        this.endingTimer = 0;
        this.winningTeam = null;
        this.lobbyWaitTimer = 0;
        this.countdownActive = false;
        this.lobbyLoaded = false;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public Team getWinningTeam() {
        return winningTeam;
    }

    public boolean isPlaying() {
        return currentPhase == GamePhase.PLAYING;
    }

    public boolean isLobby() {
        return currentPhase == GamePhase.LOBBY;
    }

    public void startGame() {
        TeamManager teamManager = TeamSystem.getTeamManager();
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();

        if (mapPool.getMaps().isEmpty()) {
            broadcastToAll(Component.literal("No maps configured! Add maps to teamsystem_maps.json")
                .withStyle(ChatFormatting.RED));
            return;
        }

        MapConfig map = mapPool.getCurrentMap().orElse(null);
        if (map == null) {
            map = mapPool.nextMap();
            if (map == null) return;
        }

        currentPhase = GamePhase.PLAYING;
        winningTeam = null;
        teamManager.resetTickets();
        teamManager.setTickets(Team.NATO, map.getTickets());
        teamManager.setTickets(Team.RUSSIA, map.getTickets());

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
                regenerateAndReturnToLobby();
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
        broadcastToAll(Component.literal("Countdown cancelled.")
            .withStyle(ChatFormatting.RED));
    }

    private void regenerateAndReturnToLobby() {
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();
        mapPool.getCurrentMap().ifPresent(map -> {
            if (!map.getWorldFolder().equals("overworld")) {
                regenerateMapWorld(map);
            }
        });
        returnToLobby();
    }

    private void regenerateMapWorld(MapConfig map) {
        String worldFolder = map.getWorldFolder();
        if (worldFolder == null || worldFolder.isEmpty()) return;

        Path dimDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem").resolve(worldFolder);
        Path backupDir = dimDir.resolveSibling(worldFolder + "_backup");

        try {
            if (Files.exists(backupDir)) {
                deleteDirectory(dimDir);
                copyDirectory(backupDir, dimDir);
                TeamSystem.LOGGER.info("Restored map world from backup: {}", map.getName());
            }
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to regenerate map world {}: {}", map.getName(), e.getMessage());
        }
    }

    public static void backupMapWorld(MinecraftServer server, MapConfig map) {
        String worldFolder = map.getWorldFolder();
        if (worldFolder == null || worldFolder.isEmpty()) return;

        Path dimDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem").resolve(worldFolder);
        Path backupDir = dimDir.resolveSibling(worldFolder + "_backup");

        try {
            deleteDirectory(backupDir);
            if (Files.exists(dimDir)) {
                copyDirectory(dimDir, backupDir);
                TeamSystem.LOGGER.info("Backed up map world: {}", map.getName());
            }
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to backup map world {}: {}", map.getName(), e.getMessage());
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            }
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath);
                    }
                } catch (IOException ignored) {}
            });
        }
    }

    private void returnToLobby() {
        currentPhase = GamePhase.LOBBY;
        winningTeam = null;
        endingTimer = 0;

        MapPoolManager mapPool = TeamSystem.getMapPoolManager();
        MapConfig nextMap = mapPool.nextMap();
        if (nextMap != null) {
            broadcastToAll(Component.literal("Next map: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(nextMap.getName()).withStyle(ChatFormatting.YELLOW)));
        }

        teleportAllToLobby();
        applyLobbyRules();
        syncPhaseToAll();

        int waitTime = nextMap != null ? nextMap.getLobbyWaitTime() : 30;
        startCountdown(waitTime);

        if (nextMap != null) {
            backupMapWorld(server, nextMap);
        }

        TeamSystem.LOGGER.info("Returned to lobby");
    }

    private void applyLobbyRules() {
        ServerLevel lobbyWorld = getLobbyWorld();
        if (lobbyWorld == null) return;

        setGameRule(lobbyWorld, "doMobSpawning", "false");
        setGameRule(lobbyWorld, "doDaylightCycle", "false");
        setGameRule(lobbyWorld, "doWeatherCycle", "false");
        setGameRule(lobbyWorld, "mobGriefing", "false");
        setGameRule(lobbyWorld, "doFireTick", "false");
        setGameRule(lobbyWorld, "keepInventory", "true");
        setGameRule(lobbyWorld, "naturalRegeneration", "true");
        setGameRule(lobbyWorld, "fallDamage", "false");
    }

    private void setGameRule(ServerLevel level, String rule, String value) {
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "gamerule " + rule + " " + value + " " + level.dimension().location()
        );
    }

    public void teleportAllPlayersToMap(MapConfig map) {
        ServerLevel targetWorld = getMapWorld(map);
        if (targetWorld == null) {
            TeamSystem.LOGGER.error("Cannot find world for map: {}", map.getName());
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        TeamManager tm = TeamSystem.getTeamManager();
        BlockPos spawnPos = targetWorld.getSharedSpawnPos();

        for (ServerPlayer player : players) {
            Team team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (team == Team.SPECTATOR) continue;

            safeTeleport(player, targetWorld, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
        }
    }

    public void teleportAllToLobby() {
        ServerLevel lobbyWorld = getOrCreateLobbyWorld();
        if (lobbyWorld == null) {
            TeamSystem.LOGGER.error("Lobby world not found!");
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            safeTeleport(player, lobbyWorld, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5);
        }
    }

    public void teleportPlayerToLobby(ServerPlayer player) {
        ServerLevel lobbyWorld = getOrCreateLobbyWorld();
        if (lobbyWorld == null) return;

        safeTeleport(player, lobbyWorld, LOBBY_SPAWN.getX() + 0.5, LOBBY_SPAWN.getY(), LOBBY_SPAWN.getZ() + 0.5);
    }

    public void setLobbyRespawn(ServerPlayer player) {
        ServerLevel lobbyWorld = getOrCreateLobbyWorld();
        if (lobbyWorld == null) return;

        player.setRespawnPosition(lobbyWorld.dimension(), LOBBY_SPAWN, 0, false, false);
    }

    private void safeTeleport(ServerPlayer player, ServerLevel targetWorld, double x, double y, double z) {
        player.teleportTo(targetWorld, x, y, z, 0, 0);
        player.fallDistance = 0;
        player.setNoGravity(false);
        player.hurtMarked = true;
    }

    private ServerLevel getOrCreateLobbyWorld() {
        ResourceKey<Level> lobbyKey = ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID);
        ServerLevel world = server.getLevel(lobbyKey);

        if (world == null && !lobbyLoaded) {
            lobbyLoaded = true;
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(),
                "execute in teamsystem:lobby run say Lobby loaded"
            );
            world = server.getLevel(lobbyKey);
        }

        if (world == null) {
            world = server.overworld();
        }
        return world;
    }

    private ServerLevel getMapWorld(MapConfig map) {
        if (map.getWorldFolder() == null || map.getWorldFolder().isEmpty() || map.getWorldFolder().equals("overworld")) {
            return server.overworld();
        }

        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("teamsystem", map.getWorldFolder()));
        ServerLevel world = server.getLevel(worldKey);
        return world != null ? world : server.overworld();
    }

    private ServerLevel getLobbyWorld() {
        ResourceKey<Level> lobbyKey = ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID);
        ServerLevel world = server.getLevel(lobbyKey);
        return world != null ? world : server.overworld();
    }

    private void applyMapConfig(MapConfig map) {
        ServerLevel mapWorld = getMapWorld(map);
        if (mapWorld == null) return;

        setGameRule(mapWorld, "naturalRegeneration", map.hasRegen() ? "true" : "false");
        setGameRule(mapWorld, "doImmediateRespawn", map.hasRespawn() ? "false" : "true");
        setGameRule(mapWorld, "doMobSpawning", "true");
        setGameRule(mapWorld, "mobGriefing", "false");
        setGameRule(mapWorld, "doFireTick", "true");
        setGameRule(mapWorld, "keepInventory", "false");
        setGameRule(mapWorld, "fallDamage", "true");

        if (map.hasWorldBorder()) {
            mapWorld.getWorldBorder().setCenter(map.getWorldBorderCenterX(), map.getWorldBorderCenterZ());
            mapWorld.getWorldBorder().setSize(map.getWorldBorderSize());
        } else {
            mapWorld.getWorldBorder().setSize(6.0E7);
        }
    }

    private void broadcastToAll(Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    public void syncPhaseToAll() {
        GameStateSyncPacket packet = new GameStateSyncPacket(
            currentPhase.ordinal(),
            TeamSystem.getMapPoolManager().getCurrentMap().map(MapConfig::getName).orElse(""),
            winningTeam != null ? winningTeam.ordinal() : -1
        );
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public void syncPhaseToPlayer(ServerPlayer player) {
        GameStateSyncPacket packet = new GameStateSyncPacket(
            currentPhase.ordinal(),
            TeamSystem.getMapPoolManager().getCurrentMap().map(MapConfig::getName).orElse(""),
            winningTeam != null ? winningTeam.ordinal() : -1
        );
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
