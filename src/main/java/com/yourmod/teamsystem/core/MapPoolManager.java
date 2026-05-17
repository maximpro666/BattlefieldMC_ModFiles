package com.yourmod.teamsystem.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class MapPoolManager extends SavedData {
    private static final String DATA_NAME = "teamsystem_mappool";
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

    public static MapPoolManager load(MinecraftServer server, CompoundTag nbt) {
        MapPoolManager manager = new MapPoolManager(server);
        manager.currentMapIndex = nbt.getInt("CurrentMapIndex");
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putInt("CurrentMapIndex", currentMapIndex);
        return nbt;
    }

    public void loadConfig() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<MapConfig>>() {}.getType();
            List<MapConfig> loaded = GSON.fromJson(reader, listType);
            maps.clear();
            if (loaded != null) {
                for (MapConfig map : loaded) {
                    if (map.isEnabled()) {
                        maps.add(map);
                    }
                }
            }
            TeamSystem.LOGGER.info("Loaded {} maps from config", maps.size());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to load map config: {}", e.getMessage());
        }
        setDirty();
    }

    private Path getConfigPath() {
        return server.getServerDirectory().toPath().resolve(CONFIG_FILE);
    }

    private void createDefaultConfig(Path path) {
        List<MapConfig> defaults = new ArrayList<>();
        MapConfig defaultMap = new MapConfig();
        defaultMap.setName("default");
        defaultMap.setWorldFolder("default_map");
        defaultMap.setEnabled(true);
        defaultMap.setHasRespawn(true);
        defaultMap.setHasCapturePoints(true);
        defaultMap.setHasRegen(true);
        defaultMap.setHasWorldBorder(true);
        defaultMap.setWorldBorderCenterX(0);
        defaultMap.setWorldBorderCenterZ(0);
        defaultMap.setWorldBorderSize(1000);
        defaultMap.setTickets(100);
        defaultMap.setLobbyWaitTime(30);
        defaults.add(defaultMap);

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(defaults, writer);
            }
            TeamSystem.LOGGER.info("Created default map config at {}", path);
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to create default map config: {}", e.getMessage());
        }
    }

    public void reloadConfig() {
        loadConfig();
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
                setDirty();
                TeamSystem.LOGGER.info("Current map set to: {}", name);
                return true;
            }
        }
        return false;
    }

    public boolean setCurrentMap(int index) {
        if (index >= 0 && index < maps.size()) {
            currentMapIndex = index;
            setDirty();
            TeamSystem.LOGGER.info("Current map set to index: {} ({})", index, maps.get(index).getName());
            return true;
        }
        return false;
    }

    public MapConfig nextMap() {
        if (maps.isEmpty()) return null;
        currentMapIndex = (currentMapIndex + 1) % maps.size();
        setDirty();
        TeamSystem.LOGGER.info("Rotated to next map: {}", maps.get(currentMapIndex).getName());
        return maps.get(currentMapIndex);
    }

    public MapConfig getRandomMap() {
        if (maps.isEmpty()) return null;
        currentMapIndex = random.nextInt(maps.size());
        setDirty();
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
