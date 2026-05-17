package com.yourmod.teamsystem.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class RespawnConfig {
    private static final String FILE_NAME = "teamsystem_respawn_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int minDistanceFromEnemyBase = 50;
    private int minDistanceFromCapturePoint = 30;
    private int minDistanceFromOwnBase = 10;
    private int maxBeaconsPerPlayer = 3;
    private int respawnDelay = 5;

    public int getMinDistanceFromEnemyBase() { return minDistanceFromEnemyBase; }
    public int getMinDistanceFromCapturePoint() { return minDistanceFromCapturePoint; }
    public int getMinDistanceFromOwnBase() { return minDistanceFromOwnBase; }
    public int getMaxBeaconsPerPlayer() { return maxBeaconsPerPlayer; }
    public int getRespawnDelay() { return respawnDelay; }

    public static RespawnConfig load(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RespawnConfig config = GSON.fromJson(reader, RespawnConfig.class);
                if (config != null) return config;
            } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to load respawn config: {}", e.getMessage());
            }
        }
        RespawnConfig defaults = new RespawnConfig();
        defaults.save(server);
        return defaults;
    }

    public void save(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to save respawn config: {}", e.getMessage());
        }
    }
}
