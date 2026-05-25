package com.pigeostudios.pwp.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import com.pigeostudios.pwp.PWP;
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

import net.minecraft.network.chat.Component;
import static com.pigeostudios.pwp.core.ChatHelper.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MapPoolManager {
    private static final String CONFIG_FILE = "pwp_maps.json";
    private static final String SOURCES_DIR_NAME = "pwp_sources";
    public static final ResourceLocation MAP_DIMENSION_ID = new ResourceLocation("pwp", "map");
    public static final ResourceKey<Level> MAP_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION, MAP_DIMENSION_ID);
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(MapConfig.class, (InstanceCreator<MapConfig>) t -> new MapConfig())
        .create();
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
        this.maps = new CopyOnWriteArrayList<>();
        this.currentMapIndex = -1;
        this.random = new Random();
        this.votes = new ConcurrentHashMap<>();
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

    // NOTE: after nextMatchId() is called once per match, use getMatchSequence() for idempotent reads
    public int getMatchSequence() {
        return matchSequence;
    }

    public Path getSourcesPath() {
        Path serverRoot = server.getServerDirectory().toPath().resolve(SOURCES_DIR_NAME);
        if (Files.isDirectory(serverRoot)) return serverRoot;
        return getServerRoot().resolve(SOURCES_DIR_NAME);
    }

    private Path getMapRegionDir() {
        return getServerRoot().resolve("dimensions").resolve("pwp").resolve("map").resolve("region");
    }

    private boolean sourceFolderExists(String worldFolder) {
        if (worldFolder == null || worldFolder.isEmpty()) return false;
        return Files.isDirectory(getSourcesPath().resolve(worldFolder).resolve("region"));
    }

    public static ServerLevel getMapLevel(MinecraftServer server) {
        ServerLevel w = server.getLevel(MAP_DIMENSION_KEY);
        if (w == null) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "execute in pwp:map run say Loading map dimension");
            w = server.getLevel(MAP_DIMENSION_KEY);
        }
        return w;
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
            PWP.LOGGER.info("Discovered {} maps from sources", maps.size());
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            com.google.gson.JsonElement jsonElement = GSON.fromJson(reader, com.google.gson.JsonElement.class);
            if (jsonElement == null) jsonElement = new com.google.gson.JsonObject();

            if (jsonElement.isJsonArray()) {
                Type listType = new TypeToken<List<MapConfig>>() {}.getType();
                List<MapConfig> loaded = GSON.fromJson(jsonElement, listType);
                if (loaded != null) {
                    for (MapConfig map : loaded) {
                        if (!map.isEnabled()) continue;
                        if (map.getState() == null) map.setState(MapState.AVAILABLE);
                        if (sourceFolderExists(map.getWorldFolder())) {
                            maps.add(map);
                        } else {
                            PWP.LOGGER.warn("Source folder missing for map {}, skipping", map.getName());
                        }
                    }
                }
            } else if (jsonElement.isJsonObject()) {
                com.google.gson.JsonObject root = jsonElement.getAsJsonObject();

                if (root.has("matchSequence")) {
                    matchSequence = root.get("matchSequence").getAsInt();
                }

                if (root.has("currentMapIndex")) {
                    currentMapIndex = root.get("currentMapIndex").getAsInt();
                }

                if (root.has("maps") && root.get("maps").isJsonArray()) {
                    Type listType = new TypeToken<List<MapConfig>>() {}.getType();
                    List<MapConfig> loaded = GSON.fromJson(root.get("maps"), listType);
                    if (loaded != null) {
                        for (MapConfig map : loaded) {
                            if (!map.isEnabled()) continue;
                            if (map.getState() == null) map.setState(MapState.AVAILABLE);
                            if (sourceFolderExists(map.getWorldFolder())) {
                                maps.add(map);
                            } else {
                                PWP.LOGGER.warn("Source folder missing for map {}, skipping", map.getName());
                            }
                        }
                    }
                }
            }

            saveConfig();
            PWP.LOGGER.info("Loaded {} maps, next match id: {}", maps.size(), matchSequence);
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to load map config: {}", e.getMessage());
        }
    }

    private List<MapConfig> scanSourcesFolder() {
        List<MapConfig> found = new ArrayList<>();
        Path sourcesDir = getSourcesPath();
        if (!Files.isDirectory(sourcesDir)) {
            PWP.LOGGER.warn("Sources folder does not exist: {}", sourcesDir);
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
                        config.setHasCapturePoints(false);
                        config.setHasRegen(true);
                        config.setHasWorldBorder(true);
                        config.setWorldBorderCenterX(0);
                        config.setWorldBorderCenterZ(0);
                        config.setWorldBorderSize(1000);
                        config.setTickets(100);
                        config.setLobbyWaitTime(30);
                        config.setState(MapState.AVAILABLE);
                        autoDetectMapCenter(config);
                        found.add(config);
                    }
                });
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to scan sources folder: {}", e.getMessage());
        }
        return found;
    }

    public void saveConfig() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("matchSequence", matchSequence);
                root.put("currentMapIndex", currentMapIndex);
                root.put("maps", maps);
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to save map config: {}", e.getMessage());
        }
    }

    private Path getConfigPath() {
        return getServerRoot().resolve(CONFIG_FILE).normalize();
    }

    public void reloadConfig() {
        loadConfig();
    }

    // ========== Source → Live Copy & Delete ==========
    // LEGACY: single-server mode. In multi-server mode (pwp.mode=match),
    // the match server has a pre-loaded world from the template and copyToLive is not needed.

    public boolean copyToLive(MapConfig map) {
        String folderName = map.getWorldFolder();
        if (folderName == null || folderName.isEmpty()) return false;

        Path sourceDir = getSourcesPath().resolve(folderName);
        if (!Files.isDirectory(sourceDir.resolve("region"))) {
            PWP.LOGGER.warn("No source data for map {}", map.getName());
            return false;
        }

        try {
            ServerLevel mapLevel = getMapLevel(server);
            if (mapLevel == null) {
                PWP.LOGGER.error("Map dimension not available");
                return false;
            }

            mapLevel.getPlayers(p -> true).forEach(p -> teleportToLobby(p));

            mapLevel.save(null, true, false);

            Path dimDir = getMapRegionDir().getParent();
            Path regionDir = dimDir.resolve("region");

            PWP.LOGGER.info("CopyToLive: source={}, target={}", sourceDir.resolve("region"), regionDir);

            syncRegionFiles(sourceDir.resolve("region"), regionDir);

            for (int i = 0; i < 20; i++) {
                mapLevel.getChunkSource().tick(() -> true, true);
            }

            Files.createDirectories(dimDir.resolve("data"));

            saveConfig();

            PWP.LOGGER.info("Deployed map {} to pwp:map", map.getName());
            return true;
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to deploy map {}: {}", map.getName(), e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                PWP.LOGGER.error("  at {}", ste);
            }
            return false;
        }
    }

    public boolean deleteLive(MapConfig map) {
        PWP.LOGGER.info("Map {} deleted from live (no-op - dimension stays loaded)", map.getName());
        return true;
    }

    private void teleportToLobby(ServerPlayer p) {
        ServerLevel lobby = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation("pwp", "lobby")));
        if (lobby == null) lobby = server.overworld();
        p.teleportTo(lobby, 0, 100, 0, 0, 0);
    }


    private void copyRegionFiles(Path srcDir, Path dstDir) throws IOException {
        Files.createDirectories(dstDir);
        try (var stream = Files.list(srcDir)) {
            for (Path source : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(source)) continue;
                String fileName = source.getFileName().toString();
                if (!fileName.endsWith(".mca")) continue;
                Path target = dstDir.resolve(fileName);
                copyFileForce(source, target);
            }
        }
    }

    private void copyFileForce(Path source, Path target) throws IOException {
        byte[] data = Files.readAllBytes(source);
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                try (var ch = java.nio.channels.FileChannel.open(target,
                        java.nio.file.StandardOpenOption.WRITE,
                        java.nio.file.StandardOpenOption.READ,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                    ch.write(java.nio.ByteBuffer.wrap(data));
                }
                return;
            } catch (IOException e) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
        throw new IOException("Failed to write " + target + " after 10 attempts");
    }

    private void autoDetectMapCenter(MapConfig map) {
        String folderName = map.getWorldFolder();
        if (folderName == null || folderName.isEmpty()) return;
        Path sourceRegionDir = getSourcesPath().resolve(folderName).resolve("region");
        if (!Files.isDirectory(sourceRegionDir)) return;

        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");
            java.util.Map<Integer, java.util.List<Integer>> regionMap = new java.util.HashMap<>();

            try (var stream = java.nio.file.Files.list(sourceRegionDir)) {
                stream.forEach(path -> {
                    String name = path.getFileName().toString();
                    var m = pattern.matcher(name);
                    if (m.matches()) {
                        int rx = Integer.parseInt(m.group(1));
                        int rz = Integer.parseInt(m.group(2));
                        regionMap.computeIfAbsent(rx, k -> new java.util.ArrayList<>()).add(rz);
                    }
                });
            }

            if (regionMap.isEmpty()) return;

            int bestX = regionMap.entrySet().stream()
                .max(java.util.Comparator.comparingInt(e -> e.getValue().size()))
                .get().getKey();

            java.util.List<Integer> zs = regionMap.get(bestX);
            java.util.Map<Integer, Integer> zCount = new java.util.HashMap<>();
            for (int z : zs) zCount.merge(z, 1, Integer::sum);
            int bestZ = zCount.entrySet().stream()
                .max(java.util.Comparator.comparingInt(java.util.Map.Entry::getValue))
                .get().getKey();

            int centerX = bestX * 512 + 256;
            int centerZ = bestZ * 512 + 256;

            map.setWorldBorderCenterX(centerX);
            map.setWorldBorderCenterZ(centerZ);
            map.setNatoSpawn(new int[]{centerX - 10, 64, centerZ});
            map.setRussiaSpawn(new int[]{centerX + 10, 64, centerZ});

            PWP.LOGGER.info("Auto-detected map center for {} at ({}, {})",
                map.getName(), centerX, centerZ);
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to auto-detect map center: {}", e.getMessage());
        }
    }

    private void syncRegionFiles(Path srcDir, Path dstDir) throws IOException {
        Files.createDirectories(dstDir);
        Set<String> newFiles = new HashSet<>();
        if (Files.isDirectory(srcDir)) {
            try (var stream = Files.list(srcDir)) {
                for (Path source : (Iterable<Path>) stream::iterator) {
                    if (Files.isDirectory(source)) continue;
                    String fileName = source.getFileName().toString();
                    if (!fileName.endsWith(".mca")) continue;
                    newFiles.add(fileName);
                    copyFileForce(source, dstDir.resolve(fileName));
                }
            }
        }
        if (Files.isDirectory(dstDir)) {
            try (var stream = Files.list(dstDir)) {
                for (Path target : (Iterable<Path>) stream::iterator) {
                    if (Files.isDirectory(target)) continue;
                    String fileName = target.getFileName().toString();
                    if (!fileName.endsWith(".mca")) continue;
                    if (!newFiles.contains(fileName)) {
                        try {
                            Files.deleteIfExists(target);
                        } catch (java.nio.file.FileSystemException e) {
                            try (var ch = java.nio.channels.FileChannel.open(target,
                                    java.nio.file.StandardOpenOption.WRITE,
                                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                                ch.write(java.nio.ByteBuffer.allocate(0));
                            }
                        }
                    }
                }
            }
        }
    }

    private void deleteWithRetry(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        java.util.List<Path> failed = new java.util.ArrayList<>();
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                for (int attempt = 0; attempt < 10; attempt++) {
                    try {
                        Files.deleteIfExists(path);
                        failed.remove(path);
                        break;
                    } catch (java.nio.file.FileSystemException e) {
                        if (attempt == 0) failed.add(path);
                        // Попытка обнулить файл через FileChannel чтобы разорвать memory-mapping
                        if (attempt == 5 && !Files.isDirectory(path)) {
                            try {
                                try (var ch = java.nio.channels.FileChannel.open(path,
                                        java.nio.file.StandardOpenOption.WRITE,
                                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                                    ch.write(java.nio.ByteBuffer.allocate(0));
                                }
                            } catch (Exception ignored) {}
                        }
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    } catch (IOException e) {
                        break;
                    }
                }
            });
        }
        if (!failed.isEmpty()) {
            PWP.LOGGER.warn("Could not delete {} files in {} (Windows lock after retry + truncation)", failed.size(), dir);
            for (Path p : failed) {
                PWP.LOGGER.warn("  Locked: {}", p);
            }
        }
        // Если директория опустела — удаляем её
        try (var remaining = Files.list(dir)) {
            if (!remaining.findAny().isPresent()) {
                for (int attempt = 0; attempt < 5; attempt++) {
                    try {
                        Files.deleteIfExists(dir);
                        break;
                    } catch (java.nio.file.FileSystemException e) {
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    }
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

    // ========== State Queries ==========

    public List<MapConfig> getMaps() { return new ArrayList<>(maps); }

    public List<MapConfig> getMapsByState(MapState state) {
        return maps.stream().filter(m -> m.getState() == state).collect(Collectors.toList());
    }

    public List<MapConfig> getPlayableMaps() {
        return maps.stream().filter(MapConfig::isPlayable).collect(Collectors.toList());
    }

    public boolean hasAvailableMaps() {
        return maps.stream().anyMatch(m -> m.getState() == MapState.AVAILABLE && m.isPlayable());
    }

    public int getAvailableCount() {
        return (int) maps.stream().filter(m -> m.getState() == MapState.AVAILABLE && m.isPlayable()).count();
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
                if (!maps.get(i).isPlayable()) return false;
                currentMapIndex = i;
                saveConfig();
                return true;
            }
        }
        return false;
    }

    public boolean selectMap(int index) {
        if (index < 0 || index >= maps.size()) return false;
        if (!maps.get(index).isPlayable()) return false;
        currentMapIndex = index;
        saveConfig();
        return true;
    }

    public MapConfig pickNextAvailable() {
        List<MapConfig> available = getPlayableMaps();
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
        votes.put(uuid, mapName.toLowerCase());
        return true;
    }

    public List<MapConfig> pickVotingCandidates(int maxCount, List<String> excludeRecent) {
        List<MapConfig> playable = getPlayableMaps();
        if (playable.isEmpty()) return playable;

        List<MapConfig> pool = new ArrayList<>(playable);
        List<MapConfig> recentMaps = new ArrayList<>();

        if (excludeRecent != null && !excludeRecent.isEmpty()) {
            Iterator<MapConfig> it = pool.iterator();
            while (it.hasNext()) {
                MapConfig m = it.next();
                if (excludeRecent.stream().anyMatch(n -> n.equalsIgnoreCase(m.getName()))) {
                    recentMaps.add(m);
                    it.remove();
                }
            }
        }

        Collections.shuffle(pool, random);

        int slotsForNonRecent = Math.min(maxCount - Math.min(recentMaps.size(), maxCount), pool.size());
        List<MapConfig> candidates = new ArrayList<>(pool.subList(0, slotsForNonRecent));

        for (MapConfig recent : recentMaps) {
            if (candidates.size() < maxCount) {
                candidates.add(recent);
            }
        }

        return candidates;
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
        PWP.LOGGER.info("Maintenance cycle started");

        try {
            server.getPlayerList().saveAll();
            for (ServerLevel level : server.getAllLevels()) {
                try { level.save(null, true, false); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            PWP.LOGGER.error("Failed to save before maintenance: {}", e.getMessage());
        }

        maintenanceRunning = false;
        maintenanceCooldown = MAINTENANCE_INTERVAL_TICKS;

        PWP.LOGGER.info("Maintenance finished (single-dimension mode - no cleanup needed)");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (maintenanceCooldown > 0) {
            maintenanceCooldown--;
        }
    }

    // ========== Pool Management ==========

    public boolean addMap(String folderName) {
        Path sourceDir = getSourcesPath().resolve(folderName);
        if (!Files.isDirectory(sourceDir.resolve("region"))) {
            PWP.LOGGER.warn("Cannot add map '{}': source folder not found", folderName);
            return false;
        }
        if (maps.stream().anyMatch(m -> m.getWorldFolder().equals(folderName))) {
            PWP.LOGGER.warn("Cannot add map '{}': already in pool", folderName);
            return false;
        }
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
        autoDetectMapCenter(config);
        maps.add(config);
        saveConfig();
        PWP.LOGGER.info("Map '{}' added to pool", folderName);
        return true;
    }

    public boolean removeMap(String name) {
        for (int i = 0; i < maps.size(); i++) {
            if (maps.get(i).getName().equalsIgnoreCase(name)) {
                maps.remove(i);
                if (currentMapIndex == i) currentMapIndex = -1;
                else if (currentMapIndex > i) currentMapIndex--;
                saveConfig();
                PWP.LOGGER.info("Map '{}' removed from pool", name);
                return true;
            }
        }
        return false;
    }

    public boolean setMapState(String name, MapState state) {
        for (MapConfig m : maps) {
            if (m.getName().equalsIgnoreCase(name)) {
                m.setState(state);
                saveConfig();
                return true;
            }
        }
        return false;
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
        List<MapConfig> avail = getPlayableMaps();
        for (int i = 0; i < avail.size(); i++) {
            MapConfig m = avail.get(i);
            sb.append(i + 1).append(". ").append(m.getName()).append("\n");
        }
        return sb.toString();
    }
}
