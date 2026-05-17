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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import net.minecraftforge.event.level.ChunkEvent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.IOException;
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
    private final Map<ResourceKey<Level>, Set<ChunkPos>> trackedChunks = new HashMap<>();

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

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public MinecraftServer getServer() {
        return server;
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
            broadcastToAll(Component.literal("No maps configured!").withStyle(ChatFormatting.RED));
            return;
        }

        MapConfig map = mapPool.getCurrentMap().orElse(null);
        if (map == null) {
            map = mapPool.nextMap();
            if (map == null) return;
        }

        currentPhase = GamePhase.PLAYING;
        winningTeam = null;
        currentMap = map;
        teamManager.resetTickets();
        teamManager.setTickets(Team.NATO, map.getTickets());
        teamManager.setTickets(Team.RUSSIA, map.getTickets());

        backupMapWorld(map);
        ServerLevel gameLevel = getMapWorldNoFallback(map);
        if (gameLevel != null) {
            clearWorldEntities(gameLevel);
        }
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
                resetAndReturnToLobby();
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

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        ChunkAccess chunk = event.getChunk();
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimKey = serverLevel.dimension();
            if (dimKey.location().getNamespace().equals("teamsystem") && !dimKey.location().getPath().equals("lobby")) {
                trackedChunks.computeIfAbsent(dimKey, k -> new HashSet<>()).add(chunk.getPos());
            }
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel serverLevel) {
            ResourceKey<Level> dimKey = serverLevel.dimension();
            Set<ChunkPos> chunks = trackedChunks.get(dimKey);
            if (chunks != null) {
                chunks.remove(event.getChunk().getPos());
            }
        }
    }

    private void resetAndReturnToLobby() {
        if (currentMap != null && !currentMap.getWorldFolder().equals("overworld")) {
            MapConfig map = currentMap;
            teleportAllToLobby();
            hardResetMapWorld(map);
        }
        returnToLobby();
    }

    private void hardResetMapWorld(MapConfig map) {
        ServerLevel level = getMapWorldNoFallback(map);
        if (level == null) return;

        ResourceKey<Level> dimKey = level.dimension();
        TeamSystem.LOGGER.info("Hard-resetting map world: {}", map.getName());

        Path dimDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem").resolve(map.getWorldFolder());
        Path backupDir = dimDir.resolveSibling(map.getWorldFolder() + "_backup");

        // === Validate backup ===
        Path backupRegionDir = backupDir.resolve("region");
        if (!Files.isDirectory(backupRegionDir)) {
            TeamSystem.LOGGER.warn("No backup found for map: {}", map.getName());
            return;
        }

        // === 1. Teleport all players out of this dimension ===
        for (ServerPlayer player : level.players()) {
            teleportPlayerToLobby(player);
        }
        clearWorldEntities(level);

        // === 2. Disable random ticks so chunks stay clean ===
        setGameRule(level, "randomTickSpeed", "0");

        // === 3. Save all chunks with flush ===
        level.save(null, true, false);

        // === 4. Unload ALL loaded chunks ===
        Set<ChunkPos> chunkPositions = trackedChunks.getOrDefault(dimKey, Collections.emptySet());
        List<ChunkPos> chunkSnapshot = new ArrayList<>(chunkPositions);
        int unloaded = 0;
        for (ChunkPos pos : chunkSnapshot) {
            LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
            if (chunk != null) {
                chunk.setUnsaved(false);
                level.unload(chunk);
                unloaded++;
            }
        }
        trackedChunks.remove(dimKey);
        TeamSystem.LOGGER.info("Unloaded {} chunks from {}", unloaded, map.getName());

        // === 5. Purge ChunkMap state ===
        // Closes storage, clears chunk holders, visible/updating chunk maps,
        // distance manager tickets, and pending unloads.
        // Ensures next chunk load comes from disk, NOT from memory.
        purgeChunkState(level);

        // === 6. Restore ALL dimension files from backup ===
        // region/, poi/, entity/, data/ — anything the backup contains
        try {
            restoreDirectoryFromBackup(dimDir.resolve("region"), backupDir.resolve("region"));
            restoreDirectoryFromBackup(dimDir.resolve("poi"), backupDir.resolve("poi"));
            restoreDirectoryFromBackup(dimDir.resolve("entities"), backupDir.resolve("entities"));
            // data/ folder (scoreboard, raid data, etc) — skip unless backup has it
            Path backupDataDir = backupDir.resolve("data");
            if (Files.isDirectory(backupDataDir)) {
                Path dataDir = dimDir.resolve("data");
                Files.createDirectories(dataDir);
                try (var files = Files.list(backupDataDir)) {
                    for (Path src : (Iterable<Path>) files::iterator) {
                        Path dst = dataDir.resolve(src.getFileName());
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            TeamSystem.LOGGER.info("Restored dimension files from backup for map: {}", map.getName());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to restore map files: {}", e.getMessage());
        }

        TeamSystem.LOGGER.info("Hard reset complete for map: {}", map.getName());
    }

    private void purgeChunkState(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            Class<?> cmc = chunkMap.getClass();

            String[] visibleMapNames = {"visibleChunkMap"};
            String[] updatingMapNames = {"updatingChunkMap"};
            String[] pendingUnloadNames = {"pendingUnloads"};
            String[] distMgrNames = {"distanceManager"};
            String[] storageNames = {"storage"};
            String[] poiMgrNames = {"poiManager"};

            // 1. Close storage (RegionFileStorage) — releases .mca file handles
            Object storage = findFieldValue(cmc, chunkMap, storageNames);
            if (storage instanceof AutoCloseable c) { c.close(); }

            // 2. Close POI storage
            Object poiMgr = findFieldValue(cmc, chunkMap, poiMgrNames);
            if (poiMgr instanceof AutoCloseable c) { c.close(); }

            // 3. Clear visibleChunkMap — removes ALL ChunkHolder references
            Object visibleMap = findFieldValue(cmc, chunkMap, visibleMapNames);
            if (visibleMap instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> map) {
                int before = map.size();
                map.clear();
                TeamSystem.LOGGER.info("Cleared visibleChunkMap ({} holders removed)", before);
            }

            // 4. Clear updatingChunkMap — removes pending update references
            Object updatingMap = findFieldValue(cmc, chunkMap, updatingMapNames);
            if (updatingMap instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> map) {
                int before = map.size();
                map.clear();
                TeamSystem.LOGGER.info("Cleared updatingChunkMap ({} holders removed)", before);
            }

            // 5. Clear pendingUnloads
            Object pendingSet = findFieldValue(cmc, chunkMap, pendingUnloadNames);
            if (pendingSet instanceof it.unimi.dsi.fastutil.longs.LongSet set) {
                set.clear();
            }

            // 6. Clear DistanceManager tickets — prevents chunk reload from old state
            Object distMgr = findFieldValue(cmc, chunkMap, distMgrNames);
            if (distMgr != null) {
                String[] ticketNames = {"tickets"};
                Object ticketsMap = findFieldValue(distMgr.getClass(), distMgr, ticketNames);
                if (ticketsMap instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> map) {
                    map.clear();
                }
            }

            TeamSystem.LOGGER.info("Purged ChunkMap state for dimension: {}", level.dimension().location());
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to purge ChunkMap state: {}", e.getMessage());
        }
    }

    private Object findFieldValue(Class<?> clazz, Object instance, String... candidates) throws Exception {
        for (String name : candidates) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(instance);
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private void restoreDirectoryFromBackup(Path targetDir, Path backupDir) throws IOException {
        if (!Files.isDirectory(backupDir)) return;
        Files.createDirectories(targetDir);
        // Delete existing .mca files
        if (Files.isDirectory(targetDir)) {
            try (var files = Files.list(targetDir)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    if (file.toString().endsWith(".mca")) {
                        Files.deleteIfExists(file);
                    }
                }
            }
        }
        // Copy backup .mca files
        try (var files = Files.list(backupDir)) {
            for (Path src : (Iterable<Path>) files::iterator) {
                Path dst = targetDir.resolve(src.getFileName());
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void backupMapWorld(MapConfig map) {
        Path backupRegionDir = getBackupRegionDir(map);
        if (Files.isDirectory(backupRegionDir)) {
            TeamSystem.LOGGER.info("Backup already exists for map: {}", map.getName());
            return;
        }
        forceBackupMapWorld(map);
    }

    public void forceBackupMapWorld(MapConfig map) {
        ServerLevel level = getMapWorldNoFallback(map);
        if (level == null) return;

        TeamSystem.LOGGER.info("Backing up map world: {}", map.getName());
        level.save(null, false, false);

        try {
            Path dimDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions").resolve("teamsystem").resolve(map.getWorldFolder());
            Path backupDir = dimDir.resolveSibling(map.getWorldFolder() + "_backup");
            Path regionDir = dimDir.resolve("region");

            if (!Files.isDirectory(regionDir)) {
                TeamSystem.LOGGER.warn("Region dir not found for map: {}", map.getName());
                return;
            }

            Files.createDirectories(backupDir.resolve("region"));

            try (var files = Files.list(regionDir)) {
                for (Path src : (Iterable<Path>) files::iterator) {
                    Path dst = backupDir.resolve("region").resolve(src.getFileName());
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            TeamSystem.LOGGER.info("Backed up region files for map: {}", map.getName());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to backup map {}: {}", map.getName(), e.getMessage());
        }
    }

    public void reBackupCurrentMap() {
        if (currentMap != null) {
            forceBackupMapWorld(currentMap);
        }
    }

    private Path getBackupRegionDir(MapConfig map) {
        Path dimDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem").resolve(map.getWorldFolder());
        return dimDir.resolveSibling(map.getWorldFolder() + "_backup").resolve("region");
    }

    private void clearWorldEntities(ServerLevel level) {
        List<Entity> allEntities = new ArrayList<>();
        for (Entity e : level.getEntities().getAll()) {
            allEntities.add(e);
        }

        int removed = 0;
        for (Entity entity : allEntities) {
            if (entity == null) continue;
            if (!(entity instanceof ServerPlayer) && !(entity instanceof Player)) {
                entity.discard();
                removed++;
            }
        }
        if (removed > 0) {
            TeamSystem.LOGGER.info("Removed {} entities", removed);
        }
    }

    private void returnToLobby() {
        currentPhase = GamePhase.LOBBY;
        winningTeam = null;
        endingTimer = 0;
        currentMap = null;

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
            "gamerule " + rule + " " + value
        );
    }

    public void teleportAllPlayersToMap(MapConfig map) {
        ServerLevel targetWorld = getMapWorld(map);
        if (targetWorld == null) return;

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
        if (lobbyWorld == null) return;

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

    public void setLobbyRespawnAtPlayer(ServerPlayer player) {
        ServerLevel lobbyWorld = getOrCreateLobbyWorld();
        if (lobbyWorld == null) return;
        BlockPos pos = player.blockPosition();
        player.setRespawnPosition(lobbyWorld.dimension(), pos, player.getYRot(), false, false);
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

        if (world == null) world = server.overworld();
        return world;
    }

    private ServerLevel getMapWorldNoFallback(MapConfig map) {
        if (map.getWorldFolder() == null || map.getWorldFolder().isEmpty() || map.getWorldFolder().equals("overworld"))
            return null;
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("teamsystem", map.getWorldFolder()));
        return server.getLevel(worldKey);
    }

    private ServerLevel getMapWorld(MapConfig map) {
        if (map.getWorldFolder() == null || map.getWorldFolder().isEmpty() || map.getWorldFolder().equals("overworld"))
            return server.overworld();

        ResourceLocation dimId = new ResourceLocation("teamsystem", map.getWorldFolder());
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, dimId);
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
}
