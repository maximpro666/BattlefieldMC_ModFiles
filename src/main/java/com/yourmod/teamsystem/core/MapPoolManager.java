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
    private static final String SOURCES_DIR_NAME = "teamsystem_sources";
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
    private int matchSequence;

    public MapPoolManager(MinecraftServer server) {
        this.server = server;
        this.maps = new ArrayList<>();
        this.currentMapIndex = -1;
        this.random = new Random();
        this.votes = new HashMap<>();
        this.maintenanceRunning = false;
        this.maintenanceCooldown = 0;
        this.restartAfterMaintenance = true;
        this.matchSequence = 0;
    }

    public boolean isRestartAfterMaintenance() { return restartAfterMaintenance; }
    public void setRestartAfterMaintenance(boolean v) { this.restartAfterMaintenance = v; }

    // ========== Path Helpers ==========

    private Path getServerRoot() {
        return server.getWorldPath(LevelResource.ROOT);
    }

    public int nextMatchId() {
        return matchSequence++;
    }

    public Path getSourcesPath() {
        return getServerRoot().resolve(SOURCES_DIR_NAME);
    }

    private Path getLivePath(String instanceKey) {
        return getServerRoot().resolve("dimensions").resolve("teamsystem").resolve(instanceKey);
    }

    private String sanitize(String name) {
        return MapConfig.sanitizeToResourcePath(name);
    }

    // ========== Loading / Persistence ==========

    public void loadConfig() {
        maps.clear();
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            maps.addAll(scanSourcesFolder());
            for (MapConfig m : maps) {
                if (m.getState() == null) m.setState(MapState.AVAILABLE);
            }
            saveConfig();
            TeamSystem.LOGGER.info("Discovered {} maps from sources", maps.size());
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<MapConfig>>() {}.getType();
            List<MapConfig> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                for (MapConfig map : loaded) {
                    if (!map.isEnabled()) continue;
                    if (map.getState() == null) map.setState(MapState.AVAILABLE);
                    String key = sanitize(map.getWorldFolder());
                    if (Files.isDirectory(getSourcesPath().resolve(key).resolve("region"))) {
                        maps.add(map);
                    } else {
                        TeamSystem.LOGGER.warn("Source folder missing for map {}, skipping", map.getName());
                    }
                }
            }

            for (MapConfig srcMap : scanSourcesFolder()) {
                String key = sanitize(srcMap.getWorldFolder());
                boolean exists = maps.stream().anyMatch(m -> sanitize(m.getWorldFolder()).equals(key));
                if (!exists) {
                    srcMap.setState(MapState.AVAILABLE);
                    maps.add(srcMap);
                }
            }

            saveConfig();
            TeamSystem.LOGGER.info("Loaded {} maps from config", maps.size());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to load map config: {}", e.getMessage());
        }
    }

    private List<MapConfig> scanSourcesFolder() {
        List<MapConfig> found = new ArrayList<>();
        Path sourcesDir = getSourcesPath();
        if (!Files.isDirectory(sourcesDir)) {
            TeamSystem.LOGGER.warn("Sources folder does not exist: {}", sourcesDir);
            return found;
        }

        try (var stream = Files.list(sourcesDir)) {
            stream.filter(Files::isDirectory)
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
            TeamSystem.LOGGER.error("Failed to scan sources folder: {}", e.getMessage());
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

    // ========== Source → Live Copy & Delete ==========

    public boolean copyToLive(MapConfig map) {
        String worldKey = sanitize(map.getWorldFolder());
        if (worldKey.isEmpty()) return false;

        String instanceKey = worldKey + "_" + matchSequence++;
        map.setMatchInstance(instanceKey);

        Path sourceDir = getSourcesPath().resolve(worldKey);
        Path liveDir = getLivePath(instanceKey);

        if (!Files.isDirectory(sourceDir.resolve("region"))) {
            TeamSystem.LOGGER.warn("No source data for map {}", map.getName());
            return false;
        }

        try {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("teamsystem", instanceKey));
            ServerLevel level = server.getLevel(dimKey);
            if (level != null) {
                level.save(null, true, false);
                purgeDimensionCaches(level);
            }

            if (Files.isDirectory(liveDir)) {
                deleteDirectory(liveDir);
            }

            Files.createDirectories(liveDir.getParent());
            copyDirectory(sourceDir, liveDir);

            MapDimensionGenerator.generateInstanceDatapack(server, instanceKey);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");

            TeamSystem.LOGGER.info("Copied map {} to live instance {} (key={})", map.getName(), map.getMatchInstance(), instanceKey);
            return true;
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to copy map {} to live: {}", map.getName(), e.getMessage());
            return false;
        }
    }

    public boolean deleteLive(MapConfig map) {
        String instanceKey = map.getMatchInstance();
        if (instanceKey == null || instanceKey.isEmpty()) {
            instanceKey = sanitize(map.getWorldFolder());
            if (instanceKey.isEmpty()) return false;
        }

        Path liveDir = getLivePath(instanceKey);

        try {
            if (Files.isDirectory(liveDir)) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("teamsystem", instanceKey));
                ServerLevel level = server.getLevel(dimKey);
                if (level != null) {
                    level.save(null, true, false);
                    purgeDimensionCaches(level);
                }
                deleteDirectory(liveDir);
            }

            MapDimensionGenerator.removeInstanceDatapack(server, instanceKey);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            map.setMatchInstance(null);
            saveConfig();

            TeamSystem.LOGGER.info("Deleted live instance {} for map {}", instanceKey, map.getName());
            return true;
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to delete live instance {} for map {}: {}", instanceKey, map.getName(), e.getMessage());
            return false;
        }
    }

    private void copyDirectory(Path src, Path dst) throws IOException {
        Files.createDirectories(dst);
        try (var stream = Files.walk(src)) {
            for (Path source : (Iterable<Path>) stream::iterator) {
                Path target = dst.resolve(src.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.walk(dir)) {
            Iterator<Path> it = stream.sorted(Comparator.reverseOrder()).iterator();
            while (it.hasNext()) {
                Files.deleteIfExists(it.next());
            }
        }
    }

    private void purgeDimensionCaches(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            Class<?> cmc = chunkMap.getClass();

            Object storage = findField(cmc, chunkMap, "storage");
            if (storage instanceof AutoCloseable c) c.close();

            Object poiMgr = findField(cmc, chunkMap, "poiManager");
            if (poiMgr instanceof AutoCloseable c) c.close();

            Object visible = findField(cmc, chunkMap, "visibleChunkMap");
            if (visible instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> m) m.clear();

            Object updating = findField(cmc, chunkMap, "updatingChunkMap");
            if (updating instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> m) m.clear();

            Object pending = findField(cmc, chunkMap, "pendingUnloads");
            if (pending instanceof it.unimi.dsi.fastutil.longs.LongSet s) s.clear();

            Object dist = findField(cmc, chunkMap, "distanceManager");
            if (dist != null) {
                Object tickets = findField(dist.getClass(), dist, "tickets");
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
        saveConfig();
        return true;
    }

    public MapConfig pickNextAvailable() {
        List<MapConfig> available = getMapsByState(MapState.AVAILABLE);
        if (available.isEmpty()) return null;

        int weightedPick = random.nextInt(available.size());
        MapConfig picked = available.get(weightedPick);
        currentMapIndex = maps.indexOf(picked);
        saveConfig();
        return picked;
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

    // ========== Maintenance (legacy - kept for manual use) ==========

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

        int cleaned = 0;
        int failed = 0;
        for (MapConfig map : maps) {
            String inst = map.getMatchInstance();
            if (inst == null || inst.isEmpty()) continue;
            try {
                if (deleteLive(map)) cleaned++;
                else failed++;
            } catch (Exception e) {
                failed++;
                TeamSystem.LOGGER.error("Failed to clean instance {} for map {}: {}", inst, map.getName(), e.getMessage());
            }
        }
        saveConfig();

        maintenanceRunning = false;
        maintenanceCooldown = MAINTENANCE_INTERVAL_TICKS;

        TeamSystem.LOGGER.info("Maintenance finished. Cleaned: {}, Failed: {}", cleaned, failed);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (maintenanceCooldown > 0) {
            maintenanceCooldown--;
        }
    }

    // ========== Helpers ==========

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
