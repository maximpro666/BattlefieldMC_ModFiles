package com.yourmod.teamsystem.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class MapPoolManager {
    private static final String CONFIG_FILE = "teamsystem_maps.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final MinecraftServer server;
    private final List<MapConfig> maps;
    private int currentMapIndex;
    private final Random random;

    public MapPoolManager(MinecraftServer server) {
        this.server = server;
        this.maps = new ArrayList<>();
        this.currentMapIndex = -1;
        this.random = new Random();
    }

    public void loadConfig() {
        maps.clear();

        scanDimensionsFolder();

        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            saveConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<MapConfig>>() {}.getType();
            List<MapConfig> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                for (MapConfig map : loaded) {
                    if (map.isEnabled() && !maps.contains(map)) {
                        maps.add(map);
                    }
                }
            }

            List<MapConfig> fromFolder = scanDimensionsFolder();
            boolean added = false;
            for (MapConfig folderMap : fromFolder) {
                boolean exists = maps.stream()
                    .anyMatch(m -> m.getWorldFolder().equalsIgnoreCase(folderMap.getWorldFolder()));
                if (!exists) {
                    maps.add(folderMap);
                    added = true;
                    TeamSystem.LOGGER.info("Auto-added map from dimensions folder: {}", folderMap.getName());
                }
            }

            if (added) {
                saveConfig();
            }

            TeamSystem.LOGGER.info("Loaded {} maps from config", maps.size());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to load map config: {}", e.getMessage());
        }
    }

    private List<MapConfig> scanDimensionsFolder() {
        List<MapConfig> found = new ArrayList<>();
        Path dimsDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem");

        if (!Files.isDirectory(dimsDir)) {
            return found;
        }

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
                        found.add(config);
                    }
                });
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to scan dimensions folder: {}", e.getMessage());
        }

        return found;
    }

    private void saveConfig() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(maps, writer);
            }
            TeamSystem.LOGGER.info("Saved map config to {}", configPath);
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

    public List<MapConfig> getMaps() {
        return new ArrayList<>(maps);
    }

    public Optional<MapConfig> getCurrentMap() {
        if (currentMapIndex >= 0 && currentMapIndex < maps.size()) {
            return Optional.of(maps.get(currentMapIndex));
        }
        return Optional.empty();
    }

    public boolean setCurrentMap(String name) {
        for (int i = 0; i < maps.size(); i++) {
            if (maps.get(i).getName().equalsIgnoreCase(name)) {
                currentMapIndex = i;
                TeamSystem.LOGGER.info("Current map set to: {}", name);
                return true;
            }
        }
        return false;
    }

    public boolean setCurrentMap(int index) {
        if (index >= 0 && index < maps.size()) {
            currentMapIndex = index;
            TeamSystem.LOGGER.info("Current map set to index: {} ({})", index, maps.get(index).getName());
            return true;
        }
        return false;
    }

    public MapConfig nextMap() {
        if (maps.isEmpty()) return null;
        currentMapIndex = (currentMapIndex + 1) % maps.size();
        TeamSystem.LOGGER.info("Rotated to next map: {}", maps.get(currentMapIndex).getName());
        return maps.get(currentMapIndex);
    }

    public MapConfig getRandomMap() {
        if (maps.isEmpty()) return null;
        currentMapIndex = random.nextInt(maps.size());
        return maps.get(currentMapIndex);
    }

    public String getMapListFormatted() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maps.size(); i++) {
            MapConfig map = maps.get(i);
            sb.append(i).append(". ").append(map.getName());
            sb.append(" (").append(map.getWorldFolder()).append(")");
            if (i == currentMapIndex) {
                sb.append(" [ACTIVE]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public MinecraftServer getServer() {
        return server;
    }
}
