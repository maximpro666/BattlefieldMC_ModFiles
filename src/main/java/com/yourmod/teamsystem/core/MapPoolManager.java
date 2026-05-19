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

import net.minecraft.network.chat.Component;
import static com.yourmod.teamsystem.core.ChatHelper.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MapPoolManager {
    private static final String CONFIG_FILE = "teamsystem_maps.json";
    private static final String SOURCES_DIR_NAME = "teamsystem_sources";
    public static final ResourceLocation MAP_DIMENSION_ID = new ResourceLocation("teamsystem", "map");
    public static final ResourceKey<Level> MAP_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION, MAP_DIMENSION_ID);
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

    // NOTE: after nextMatchId() is called once per match, use getMatchSequence() for idempotent reads
    public int getMatchSequence() {
        return matchSequence;
    }

    public Path getSourcesPath() {
        return getServerRoot().resolve(SOURCES_DIR_NAME);
    }

    private Path getMapRegionDir() {
        return getServerRoot().resolve("dimensions").resolve("teamsystem").resolve("map").resolve("region");
    }

    private boolean sourceFolderExists(String worldFolder) {
        if (worldFolder == null || worldFolder.isEmpty()) return false;
        return Files.isDirectory(getSourcesPath().resolve(worldFolder).resolve("region"));
    }

    public static ServerLevel getMapLevel(MinecraftServer server) {
        ServerLevel w = server.getLevel(MAP_DIMENSION_KEY);
        if (w == null) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "execute in teamsystem:map run say Loading map dimension");
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
            TeamSystem.LOGGER.info("Discovered {} maps from sources", maps.size());
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
                            TeamSystem.LOGGER.warn("Source folder missing for map {}, skipping", map.getName());
                        }
                    }
                }
            } else if (jsonElement.isJsonObject()) {
                com.google.gson.JsonObject root = jsonElement.getAsJsonObject();

                if (root.has("matchSequence")) {
                    matchSequence = root.get("matchSequence").getAsInt();
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
                                TeamSystem.LOGGER.warn("Source folder missing for map {}, skipping", map.getName());
                            }
                        }
                    }
                }
            }

            for (MapConfig srcMap : scanSourcesFolder()) {
                boolean exists = maps.stream()
                    .anyMatch(m -> m.getWorldFolder().equals(srcMap.getWorldFolder()));
                if (!exists) {
                    srcMap.setState(MapState.AVAILABLE);
                    maps.add(srcMap);
                }
            }

            saveConfig();
            TeamSystem.LOGGER.info("Loaded {} maps, next match id: {}", maps.size(), matchSequence);
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
                        autoDetectMapCenter(config);
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
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("matchSequence", matchSequence);
                root.put("maps", maps);
                GSON.toJson(root, writer);
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
    }

    // ========== Source → Live Copy & Delete ==========

    public boolean copyToLive(MapConfig map) {
        String folderName = map.getWorldFolder();
        if (folderName == null || folderName.isEmpty()) return false;

        Path sourceDir = getSourcesPath().resolve(folderName);
        if (!Files.isDirectory(sourceDir.resolve("region"))) {
            TeamSystem.LOGGER.warn("No source data for map {}", map.getName());
            return false;
        }

        try {
            ServerLevel mapLevel = getMapLevel(server);
            if (mapLevel == null) {
                TeamSystem.LOGGER.error("Map dimension not available");
                return false;
            }

            mapLevel.getPlayers(p -> true).forEach(p ->
                teleportToLobby(p));

            // Закрываем кеш region-файлов до save, чтобы save не открыл их снова
            closeRegionFileCache(mapLevel);
            forceGC();

            mapLevel.save(null, true, false);

            // Повторно закрываем — save мог открыть файлы
            closeRegionFileCache(mapLevel);
            forceGC();

            Path dimDir = getMapRegionDir().getParent();
            Path regionDir = dimDir.resolve("region");

            TeamSystem.LOGGER.info("CopyToLive: source={}, target={}", sourceDir.resolve("region"), regionDir);

            // Удаляем только регионы, data/ не трогаем
            if (Files.isDirectory(regionDir)) {
                deleteWithRetry(regionDir);
            }
            Files.createDirectories(regionDir);
            copyRegionFiles(sourceDir.resolve("region"), regionDir);

            // Тикаем chunk source чтобы выгрузить старые чанки
            for (int i = 0; i < 20; i++) {
                mapLevel.getChunkSource().tick(() -> true, true);
            }
            clearChunkCache(mapLevel);
            forceGC();

            Files.createDirectories(dimDir.resolve("data"));

            saveConfig();

            TeamSystem.LOGGER.info("Deployed map {} to teamsystem:map", map.getName());
            return true;
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to deploy map {}: {}", map.getName(), e.getMessage());
            return false;
        }
    }

    private static void forceGC() {
        try {
            System.gc();
            System.runFinalization();
            Thread.sleep(300);
            System.gc();
            System.runFinalization();
        } catch (InterruptedException ignored) {}
    }

    public boolean deleteLive(MapConfig map) {
        TeamSystem.LOGGER.info("Map {} deleted from live (no-op - dimension stays loaded)", map.getName());
        return true;
    }

    private void closeRegionFileCache(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            Class<?> cmc = chunkMap.getClass();
            Object storage = findFieldAnywhere(cmc, chunkMap,
                "chunkStorage", "f_$chunkStorage", "storage", "f_$storage");
            if (storage != null) {
                Class<?> rfsClass = storage.getClass();
                Object regionCache = findFieldAnywhere(rfsClass, storage,
                    "regionCache", "f_regionCache", "cachedRegionFiles", "f_cachedRegionFiles");
                if (regionCache instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> rcm) {
                    for (long key : new java.util.ArrayList<>(rcm.keySet())) {
                        Object rf = rcm.get(key);
                        if (rf instanceof java.io.Closeable c) {
                            try { c.close(); } catch (Exception ignored) {}
                        }
                    }
                    rcm.clear();
                }
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to close region file cache: {}", e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void clearChunkCache(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            Class<?> cmc = chunkMap.getClass();

            // 1) Save pending chunks before clearing
            try {
                java.lang.reflect.Method saveAll = findMethod(cmc, "saveAllChunks", boolean.class);
                if (saveAll != null) saveAll.invoke(chunkMap, false);
            } catch (Exception ignored) {}

            // 2) Drop each visible chunk via ChunkMap.drop(long) (properly cleans up ChunkHolder)
            Object visible = findFieldAnywhere(cmc, chunkMap,
                "visibleChunkMap", "visibleChunks", "f_visibleChunkMap");
            if (visible instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> vcm) {
                java.lang.reflect.Method dropMethod = findMethod(cmc, "drop", long.class);
                if (dropMethod != null) {
                    for (long pos : new java.util.ArrayList<>(vcm.keySet())) {
                        try { dropMethod.invoke(chunkMap, pos); } catch (Exception ignored) {}
                    }
                }
                vcm.clear();
            }

            // 3) Clear updating chunk map
            Object updating = findFieldAnywhere(cmc, chunkMap,
                "updatingChunkMap", "updatingChunks", "f_updatingChunkMap");
            if (updating instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> ucm) {
                ucm.clear();
            }

            // 4) Clear pending operations LongSets
            for (String name : new String[]{"pendingUnloads", "chunksToLoad", "pendingLoads",
                    "toDrop", "f_chunksToLoad", "f_pendingUnloads"}) {
                Object set = findFieldAnywhere(cmc, chunkMap, name);
                if (set instanceof it.unimi.dsi.fastutil.longs.LongSet ls) ls.clear();
            }

            // 5) Clear entity lookup + block-entity ticking lists to drop stale references
            for (String fn : new String[]{"entityMap", "entityByIdMap", "f_entityMap"}) {
                Object em = findFieldAnywhere(cmc, chunkMap, fn);
                if (em instanceof Map<?,?> m) m.clear();
            }
            // Also clear block entities pending ticks list in the level
            try {
                java.lang.reflect.Field beField = net.minecraft.server.level.ServerLevel.class.getDeclaredField("blockEntityTickers");
                beField.setAccessible(true);
                Object beTickers = beField.get(level);
                if (beTickers instanceof List<?> l) l.clear();
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Field beField = net.minecraft.server.level.ServerLevel.class.getDeclaredField("pendingBlockEntityTickers");
                beField.setAccessible(true);
                Object beTickers = beField.get(level);
                if (beTickers instanceof List<?> l) l.clear();
            } catch (Exception ignored) {}

            // 6) Nuke all DistanceManager tickets (public API + reflection fallback)
            Object dist = findFieldAnywhere(cmc, chunkMap,
                "distanceManager", "f_$distanceManager");
            if (dist != null) {
                java.lang.reflect.Method rmTicket = findMethod(dist.getClass(),
                    "removeTicket", long.class, net.minecraft.server.level.Ticket.class);
                Class<?> dmc = dist.getClass();
                for (String fn : new String[]{"tickets", "ticketMap", "f_$tickets", "ticketsByChunk"}) {
                    Object tmap = findFieldAnywhere(dmc, dist, fn);
                    if (tmap instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> lm) {
                        for (long key : new java.util.ArrayList<>(lm.keySet())) {
                            if (rmTicket != null) {
                                try {
                                    Object raw = lm.get(key);
                                    if (raw instanceof net.minecraft.util.SortedArraySet<?> set) {
                                        for (Object t : set) rmTicket.invoke(dist, key, t);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        lm.clear();
                    }
                }
                for (String fn : new String[]{"playerTickets", "f_$playerTickets"}) {
                    Object pt = findFieldAnywhere(dmc, dist, fn);
                    if (pt instanceof Map<?,?> m) m.clear();
                    else if (pt instanceof it.unimi.dsi.fastutil.objects.Object2ObjectMap<?,?> m2) m2.clear();
                }
            }

            // 7) Force chunk source to tick multiple times to flush pending operations
            for (int i = 0; i < 10; i++) {
                level.getChunkSource().tick(() -> true, true);
            }

            // 8) Clear RegionFileStorage region cache so fresh region files are read from disk
            try {
                Object storage = findFieldAnywhere(cmc, chunkMap,
                    "chunkStorage", "f_$chunkStorage", "storage", "f_$storage");
                if (storage != null) {
                    Class<?> rfsClass = storage.getClass();
                    Object regionCache = findFieldAnywhere(rfsClass, storage,
                        "regionCache", "f_regionCache", "cachedRegionFiles", "f_cachedRegionFiles");
                    if (regionCache instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> rcm) {
                        for (long key : new java.util.ArrayList<>(rcm.keySet())) {
                            Object rf = rcm.get(key);
                            if (rf instanceof java.io.Closeable c) {
                                try { c.close(); } catch (Exception ignored) {}
                            }
                        }
                        rcm.clear();
                    }
                }
            } catch (Exception ignored) {}

            TeamSystem.LOGGER.info("Chunk cache cleared for teamsystem:map");
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to clear chunk cache: {} (map may still work)", e.getMessage());
        }
    }

    /** Reflection helper to find a method by name in class hierarchy */
    private java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                java.lang.reflect.Method m = current.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            current = current.getSuperclass();
        }
        return null;
    }

    private void teleportToLobby(ServerPlayer p) {
        ServerLevel lobby = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation("teamsystem", "lobby")));
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
                Files.write(target, data, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE);
                return;
            } catch (IOException e) {
                if (attempt == 5) {
                    try {
                        try (var ch = java.nio.channels.FileChannel.open(target,
                                java.nio.file.StandardOpenOption.WRITE,
                                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                                java.nio.file.StandardOpenOption.CREATE)) {
                            ch.write(java.nio.ByteBuffer.wrap(data));
                            return;
                        }
                    } catch (Exception e2) {
                        if (attempt == 9) throw e2;
                    }
                }
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void autoDetectMapCenter(MapConfig map) {
        String folderName = map.getWorldFolder();
        if (folderName == null || folderName.isEmpty()) return;
        Path sourceRegionDir = getSourcesPath().resolve(folderName).resolve("region");
        if (!Files.isDirectory(sourceRegionDir)) return;

        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");
            java.util.List<Integer> xs = new java.util.ArrayList<>();
            java.util.List<Integer> zs = new java.util.ArrayList<>();

            try (var stream = java.nio.file.Files.list(sourceRegionDir)) {
                stream.forEach(path -> {
                    String name = path.getFileName().toString();
                    var m = pattern.matcher(name);
                    if (m.matches()) {
                        xs.add(Integer.parseInt(m.group(1)));
                        zs.add(Integer.parseInt(m.group(2)));
                    }
                });
            }

            if (xs.isEmpty()) return;

            xs.sort(null);
            zs.sort(null);
            int medianX = xs.get(xs.size() / 2);
            int medianZ = zs.get(zs.size() / 2);

            int centerX = medianX * 512 + 256;
            int centerZ = medianZ * 512 + 256;

            map.setWorldBorderCenterX(centerX);
            map.setWorldBorderCenterZ(centerZ);
            map.setNatoSpawn(new int[]{centerX, 64, centerZ});
            map.setRussiaSpawn(new int[]{centerX, 64, centerZ});

            TeamSystem.LOGGER.info("Auto-detected map center for {} at ({}, {}), spawn set to y=64",
                map.getName(), centerX, centerZ);
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to auto-detect map center: {}", e.getMessage());
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
            TeamSystem.LOGGER.warn("Could not delete {} files in {} (Windows lock after retry + truncation)", failed.size(), dir);
            for (Path p : failed) {
                TeamSystem.LOGGER.warn("  Locked: {}", p);
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

    private Object findFieldAnywhere(Class<?> clazz, Object instance, String... names) {
        for (String name : names) {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                try {
                    java.lang.reflect.Field f = current.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(instance);
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
                current = current.getSuperclass();
            }
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
            player.sendSystemMessage(error("You already voted! Use /map votes to see results."));
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

        maintenanceRunning = false;
        maintenanceCooldown = MAINTENANCE_INTERVAL_TICKS;

        TeamSystem.LOGGER.info("Maintenance finished (single-dimension mode - no cleanup needed)");
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
