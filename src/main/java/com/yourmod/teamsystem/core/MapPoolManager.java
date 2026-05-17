package com.yourmod.teamsystem.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class MapPoolManager {
    private static final String CONFIG_FILE = "teamsystem_maps.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAINTENANCE_INTERVAL_TICKS = 200;

    private final MinecraftServer server;
    private final List<MapConfig> maps;
    private int currentMapIndex;
    private final Random random;
    private final Map<String, String> votes;
    private boolean maintenanceRunning;
    private int maintenanceCooldown;
    private boolean restartAfterMaintenance;

    public MapPoolManager(MinecraftServer server) {
        this.server = server;
        this.maps = new ArrayList<>();
        this.currentMapIndex = -1;
        this.random = new Random();
        this.votes = new HashMap<>();
        this.maintenanceRunning = false;
        this.maintenanceCooldown = 0;
        this.restartAfterMaintenance = true;
        TeamSystem.LOGGER.info("restartAfterMaintenance = {}", restartAfterMaintenance);
    }

    public boolean isRestartAfterMaintenance() { return restartAfterMaintenance; }
    public void setRestartAfterMaintenance(boolean v) { this.restartAfterMaintenance = v; }

    // ========== Loading / Persistence ==========

    public void loadConfig() {
        maps.clear();
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            maps.addAll(scanDimensionsFolder());
            for (MapConfig m : maps) {
                if (m.getState() == null) m.setState(MapState.AVAILABLE);
            }
            saveConfig();
            TeamSystem.LOGGER.info("Discovered {} maps from dimensions folder", maps.size());
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<MapConfig>>() {}.getType();
            List<MapConfig> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                for (MapConfig map : loaded) {
                    if (!map.isEnabled()) continue;
                    if (map.getState() == null) map.setState(MapState.AVAILABLE);
                    String worldKey = MapConfig.sanitizeToResourcePath(map.getWorldFolder());
                    Path mapDir = server.getWorldPath(LevelResource.ROOT)
                        .resolve("dimensions").resolve("teamsystem").resolve(worldKey);
                    if (Files.isDirectory(mapDir.resolve("region"))) {
                        maps.add(map);
                    }
                }
            }

            for (MapConfig folderMap : scanDimensionsFolder()) {
                String folderKey = MapConfig.sanitizeToResourcePath(folderMap.getWorldFolder());
                boolean exists = maps.stream().anyMatch(m ->
                    MapConfig.sanitizeToResourcePath(m.getWorldFolder()).equals(folderKey));
                if (!exists) {
                    folderMap.setState(MapState.AVAILABLE);
                    maps.add(folderMap);
                }
            }

            saveConfig();
            TeamSystem.LOGGER.info("Loaded {} maps from config", maps.size());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to load map config: {}", e.getMessage());
        }
    }

    private List<MapConfig> scanDimensionsFolder() {
        List<MapConfig> found = new ArrayList<>();
        Path dimsDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem");
        if (!Files.isDirectory(dimsDir)) return found;

        try (var stream = Files.list(dimsDir)) {
            stream.filter(Files::isDirectory)
                .filter(dir -> !dir.getFileName().toString().endsWith("_backup"))
                .filter(dir -> !dir.getFileName().toString().equals("lobby"))
                .forEach(dir -> {
                    String folderName = dir.getFileName().toString();
                    Path regionDir = dir.resolve("region");
                    if (Files.isDirectory(regionDir)) {
                        MapConfig config = new MapConfig();
                        config.setName(folderName);
                        config.setWorldFolder(folderName);
                        config.setEnabled(true);
                        config.setHasRespawn(true);
                        config.setHasCapturePoints(true);
                        config.setHasRegen(true);
                        config.setHasWorldBorder(true);
                        config.setWorldBorderCenterX(0);
                        config.setWorldBorderCenterZ(0);
                        config.setWorldBorderSize(1000);
                        config.setTickets(100);
                        config.setLobbyWaitTime(30);
                        config.setState(MapState.AVAILABLE);
                        found.add(config);
                    }
                });
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to scan dimensions folder: {}", e.getMessage());
        }
        return found;
    }

    public void saveConfig() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(maps, writer);
            }
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to save map config: {}", e.getMessage());
        }
    }

    private Path getConfigPath() {
        return server.getServerDirectory().toPath().resolve(CONFIG_FILE);
    }

    public void reloadConfig() {
        loadConfig();
        MapDimensionGenerator.generateDimensionDatapacks(server);
    }

    // ========== State Queries ==========

    public List<MapConfig> getMaps() { return new ArrayList<>(maps); }

    public List<MapConfig> getMapsByState(MapState state) {
        return maps.stream().filter(m -> m.getState() == state).collect(Collectors.toList());
    }

    public boolean hasAvailableMaps() {
        return maps.stream().anyMatch(m -> m.getState() == MapState.AVAILABLE);
    }

    public int getAvailableCount() {
        return (int) maps.stream().filter(m -> m.getState() == MapState.AVAILABLE).count();
    }

    public int getDirtyCount() {
        return (int) maps.stream().filter(m -> m.getState() == MapState.DIRTY).count();
    }

    // ========== Map Selection ==========

    public Optional<MapConfig> getCurrentMap() {
        if (currentMapIndex >= 0 && currentMapIndex < maps.size()) {
            return Optional.of(maps.get(currentMapIndex));
        }
        return Optional.empty();
    }

    public boolean selectMap(String name) {
        for (int i = 0; i < maps.size(); i++) {
            if (maps.get(i).getName().equalsIgnoreCase(name)) {
                if (maps.get(i).getState() != MapState.AVAILABLE) return false;
                currentMapIndex = i;
                maps.get(i).setState(MapState.IN_MATCH);
                saveConfig();
                return true;
            }
        }
        return false;
    }

    public boolean selectMap(int index) {
        if (index < 0 || index >= maps.size()) return false;
        if (maps.get(index).getState() != MapState.AVAILABLE) return false;
        currentMapIndex = index;
        maps.get(index).setState(MapState.IN_MATCH);
        saveConfig();
        return true;
    }

    public MapConfig pickNextAvailable() {
        List<MapConfig> available = getMapsByState(MapState.AVAILABLE);
        if (available.isEmpty()) return null;

        int weightedPick = weightedRand(available);
        MapConfig picked = available.get(weightedPick);
        currentMapIndex = maps.indexOf(picked);
        picked.setState(MapState.IN_MATCH);
        saveConfig();
        return picked;
    }

    private int weightedRand(List<MapConfig> available) {
        return random.nextInt(available.size());
    }

    // ========== State Transitions ==========

    public void markInMatch(MapConfig map) {
        map.setState(MapState.IN_MATCH);
        saveConfig();
    }

    public void markDirty(MapConfig map) {
        map.setState(MapState.DIRTY);
        if (currentMapIndex >= 0 && currentMapIndex < maps.size() && maps.get(currentMapIndex) == map) {
            currentMapIndex = -1;
        }
        saveConfig();
    }

    public void markAvailable(MapConfig map) {
        map.setState(MapState.AVAILABLE);
        saveConfig();
    }

    public void markRegenerating(MapConfig map) {
        map.setState(MapState.REGENERATING);
        saveConfig();
    }

    // ========== Voting ==========

    public boolean castVote(ServerPlayer player, String mapName) {
        String uuid = player.getUUID().toString();
        if (votes.containsKey(uuid)) {
            player.sendSystemMessage(Component.literal("You already voted! Use /map votes to see results.").withStyle(ChatFormatting.RED));
            return false;
        }
        votes.put(uuid, mapName.toLowerCase());
        broadcastVoteUpdate();
        return true;
    }

    public void broadcastVoteUpdate() {
        Map<String, Integer> tally = getVoteTally();
        if (tally.isEmpty()) return;
        StringBuilder sb = new StringBuilder("§6=== Vote Results ===\n");
        List<Map.Entry<String, Integer>> sorted = tally.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());
        for (Map.Entry<String, Integer> e : sorted) {
            sb.append("§e").append(e.getKey()).append("§7: §f").append(e.getValue()).append(" votes\n");
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal(sb.toString()), false);
    }

    public Map<String, Integer> getVoteTally() {
        Map<String, Integer> tally = new HashMap<>();
        for (String vote : votes.values()) {
            tally.merge(vote, 1, Integer::sum);
        }
        return tally;
    }

    public String resolveVoteWinner() {
        if (votes.isEmpty()) return null;
        Map<String, Integer> tally = getVoteTally();
        return tally.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public void clearVotes() { votes.clear(); }

    public boolean hasVotes() { return !votes.isEmpty(); }

    // ========== Maintenance ==========

    public boolean isMaintenanceNeeded() {
        return getAvailableCount() <= 1 && getDirtyCount() > 0;
    }

    public boolean isMaintenanceRunning() { return maintenanceRunning; }

    public void runMaintenance() {
        if (maintenanceRunning) return;
        maintenanceRunning = true;
        TeamSystem.LOGGER.info("Maintenance cycle started");

        try {
            server.getPlayerList().saveAll();
            for (ServerLevel level : server.getAllLevels()) {
                try { level.save(null, true, level.noSave()); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to save before maintenance: {}", e.getMessage());
        }

        int restored = 0;
        int failed = 0;
        List<MapConfig> dirty = getMapsByState(MapState.DIRTY);
        for (MapConfig map : dirty) {
            try {
                if (!canRegenerateSafety(map)) {
                    TeamSystem.LOGGER.info("Skipping map {} (dimension still active)", map.getName());
                    continue;
                }
                if (regenerateFromBackup(map)) {
                    map.setState(MapState.AVAILABLE);
                    restored++;
                    TeamSystem.LOGGER.info("Restored map {}", map.getName());
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                TeamSystem.LOGGER.error("Failed to restore map {}: {}", map.getName(), e.getMessage());
            }
        }
        saveConfig();

        maintenanceRunning = false;
        maintenanceCooldown = MAINTENANCE_INTERVAL_TICKS;

        if (restartAfterMaintenance) {
            TeamSystem.LOGGER.info("Maintenance finished. Restored: {}, Failed: {}. Server will now restart.", restored, failed);
            server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("§6[Maintenance] Maps restored. Server restarting..."), false);
            try {
                server.getPlayerList().saveAll();
                for (ServerLevel level : server.getAllLevels()) {
                    try { level.save(null, true, level.noSave()); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                TeamSystem.LOGGER.error("Error during final save: {}", e.getMessage());
            }
            server.halt(false);
            return;
        }

        TeamSystem.LOGGER.info("Maintenance finished. Restored: {}, Failed: {}, Available: {}",
            restored, failed, getAvailableCount());
    }

    private boolean canRegenerateSafety(MapConfig map) {
        String worldKey = MapConfig.sanitizeToResourcePath(map.getWorldFolder());
        if (worldKey.isEmpty()) return true;
        ResourceLocation dimId = new ResourceLocation("teamsystem", worldKey);
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) return true;
        return level.players().isEmpty();
    }

    private boolean regenerateFromBackup(MapConfig map) {
        String worldKey = MapConfig.sanitizeToResourcePath(map.getWorldFolder());
        if (worldKey.isEmpty()) return false;
        try {
            Path dimDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions").resolve("teamsystem").resolve(worldKey);
            Path backupDir = dimDir.resolveSibling(worldKey + "_backup");

            if (!Files.isDirectory(backupDir.resolve("region"))) {
                TeamSystem.LOGGER.warn("No backup for map {}", map.getName());
                return false;
            }

            ServerLevel level = getMapLevel(map);

            if (level != null && level.players().isEmpty()) {
                map.setState(MapState.REGENERATING);
                saveConfig();

                setGameRule(level, "randomTickSpeed", "0");
                level.save(null, true, false);

                purgeDimensionCaches(level);
            }

            // Replace files
            restoreDirectory(dimDir.resolve("region"), backupDir.resolve("region"));
            restoreDirectory(dimDir.resolve("poi"), backupDir.resolve("poi"));
            restoreDirectory(dimDir.resolve("entities"), backupDir.resolve("entities"));

            Path backupDataDir = backupDir.resolve("data");
            if (Files.isDirectory(backupDataDir)) {
                Path dataDir = dimDir.resolve("data");
                Files.createDirectories(dataDir);
                try (var files = Files.list(backupDataDir)) {
                    for (Path src : (Iterable<Path>) files::iterator) {
                        Files.copy(src, dataDir.resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            TeamSystem.LOGGER.info("Restored map {} from backup", map.getName());
            return true;
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to regenerate map {}: {}", map.getName(), e.getMessage());
            return false;
        }
    }

    private void purgeDimensionCaches(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            Class<?> cmc = chunkMap.getClass();

            String[] storageNames = {"storage"};
            String[] poiMgrNames = {"poiManager"};
            String[] visibleNames = {"visibleChunkMap"};
            String[] updatingNames = {"updatingChunkMap"};
            String[] pendingNames = {"pendingUnloads"};
            String[] distNames = {"distanceManager"};

            Object storage = findField(cmc, chunkMap, storageNames);
            if (storage instanceof AutoCloseable c) c.close();

            Object poiMgr = findField(cmc, chunkMap, poiMgrNames);
            if (poiMgr instanceof AutoCloseable c) c.close();

            Object visible = findField(cmc, chunkMap, visibleNames);
            if (visible instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> m) m.clear();

            Object updating = findField(cmc, chunkMap, updatingNames);
            if (updating instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> m) m.clear();

            Object pending = findField(cmc, chunkMap, pendingNames);
            if (pending instanceof it.unimi.dsi.fastutil.longs.LongSet s) s.clear();

            Object dist = findField(cmc, chunkMap, distNames);
            if (dist != null) {
                Object tickets = findField(dist.getClass(), dist, new String[]{"tickets"});
                if (tickets instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> m) m.clear();
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to purge dimension caches: {}", e.getMessage());
        }
    }

    private Object findField(Class<?> clazz, Object instance, String... names) throws Exception {
        for (String name : names) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(instance);
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private void restoreDirectory(Path target, Path backup) throws IOException {
        if (!Files.isDirectory(backup)) return;
        Files.createDirectories(target);
        try (var files = Files.list(target)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (f.toString().endsWith(".mca")) Files.deleteIfExists(f);
            }
        }
        try (var files = Files.list(backup)) {
            for (Path src : (Iterable<Path>) files::iterator) {
                Files.copy(src, target.resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void setGameRule(ServerLevel level, String rule, String value) {
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(), "gamerule " + rule + " " + value);
    }

    private ServerLevel getMapLevel(MapConfig map) {
        String worldKey = MapConfig.sanitizeToResourcePath(map.getWorldFolder());
        if (worldKey.isEmpty()) return null;
        ResourceLocation dimId = new ResourceLocation("teamsystem", worldKey);
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
        return server.getLevel(dimKey);
    }

    // ========== Tick ==========

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (maintenanceCooldown > 0) {
            maintenanceCooldown--;
            return;
        }

        if (isMaintenanceNeeded() && !maintenanceRunning) {
            TeamSystem.LOGGER.info("Maintenance triggered: available={}, dirty={}",
                getAvailableCount(), getDirtyCount());
            runMaintenance();
        }
    }

    // ========== Legacy Helpers==========

    public MinecraftServer getServer() { return server; }

    public String getMapListFormatted() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maps.size(); i++) {
            MapConfig m = maps.get(i);
            sb.append(i).append(". ").append(m.getName());
            sb.append(" [").append(m.getState().name()).append("]");
            if (!m.isEnabled()) sb.append(" [DISABLED]");
            sb.append("\n");
        }
        return sb.toString();
    }

    public String getAvailableMapListFormatted() {
        StringBuilder sb = new StringBuilder();
        List<MapConfig> avail = getMapsByState(MapState.AVAILABLE);
        for (int i = 0; i < avail.size(); i++) {
            MapConfig m = avail.get(i);
            sb.append(i + 1).append(". ").append(m.getName()).append("\n");
        }
        return sb.toString();
    }
}
