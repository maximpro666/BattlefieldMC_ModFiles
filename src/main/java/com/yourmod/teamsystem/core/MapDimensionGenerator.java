package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MapDimensionGenerator {

    private static final String DIMENSION_TEMPLATE = """
        {
          "type": "minecraft:overworld",
          "generator": {
            "type": "minecraft:noise",
            "settings": "minecraft:overworld",
            "biome_source": {
              "type": "minecraft:multi_noise",
              "preset": "minecraft:overworld"
            }
          }
        }
        """;

    private static final String LOBBY_DIMENSION = """
        {
          "type": "teamsystem:lobby",
          "generator": {
            "type": "minecraft:flat",
            "settings": {
              "biome": "minecraft:plains",
              "layers": [
                {"block": "minecraft:stone", "height": 60},
                {"block": "minecraft:dirt", "height": 3},
                {"block": "minecraft:grass_block", "height": 1}
              ],
              "features": false,
              "structure_overrides": []
            }
          }
        }
        """;

    private static final String LOBBY_DIMENSION_TYPE = """
        {
          "ultrawarm": false,
          "natural": false,
          "piglin_safe": true,
          "respawn_anchor_works": false,
          "bed_works": false,
          "has_raids": false,
          "has_skylight": true,
          "has_ceiling": false,
          "coordinate_scale": 1.0,
          "ambient_light": 1.0,
          "fixed_time": 6000,
          "logical_height": 256,
          "effects": "minecraft:overworld",
          "infiniburn": "#minecraft:infiniburn_overworld",
          "min_y": 0,
          "height": 256,
          "monster_spawn_light_level": 15,
          "monster_spawn_block_light_limit": 15
        }
        """;

    private static final String PACK_MCMETA = """
        {
          "pack": {
            "pack_format": 15,
            "description": "TeamSystem Lobby + Map Dimensions"
          }
        }
        """;

    public static void generateDimensionDatapacks(MinecraftServer server) {
        Path datapacksDir = server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve("teamsystem_maps");

        generatePackMeta(datapacksDir);
        generateLobbyDimension(datapacksDir);
        generateMapDimensions(server, datapacksDir);
    }

    private static void generateMapDimensions(MinecraftServer server, Path datapacksDir) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        if (pool == null) return;

        List<MapConfig> maps = pool.getMaps();

        Path dimDir = datapacksDir.resolve("data").resolve("teamsystem").resolve("dimension");
        try {
            Files.createDirectories(dimDir);
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to create dimension directory: {}", e.getMessage());
            return;
        }

        // Delete dimension files for maps that no longer exist
        try (var files = Files.list(dimDir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!file.toString().endsWith(".json")) continue;
                String fileName = file.getFileName().toString();
                String keyName = fileName.substring(0, fileName.length() - 5);
                if (keyName.equals("lobby")) continue;
                boolean mapExists = maps.stream()
                    .anyMatch(m -> MapConfig.sanitizeToResourcePath(m.getWorldFolder()).equals(keyName));
                if (!mapExists) {
                    Files.deleteIfExists(file);
                    TeamSystem.LOGGER.info("Deleted stale dimension datapack: {}", fileName);
                }
            }
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to clean stale dimension files: {}", e.getMessage());
        }

        for (MapConfig map : maps) {
            String worldKey = MapConfig.sanitizeToResourcePath(map.getWorldFolder());
            if (worldKey.isEmpty() || worldKey.equals("overworld")) continue;

            Path dimensionFile = dimDir.resolve(worldKey + ".json");
            if (Files.exists(dimensionFile)) continue;

            try {
                Files.writeString(dimensionFile, DIMENSION_TEMPLATE, StandardCharsets.UTF_8);
                TeamSystem.LOGGER.info("Generated dimension datapack for map: {} -> {}",
                    map.getName(), dimensionFile);
            } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to generate dimension for map {}: {}",
                    map.getName(), e.getMessage());
            }
        }

        try {
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(),
                "reload"
            );
            TeamSystem.LOGGER.info("Server datapacks reloaded");
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Could not auto-reload datapacks: {}", e.getMessage());
        }

        TeamSystem.LOGGER.info("Map dimension datapacks generated for {} maps", maps.size());
    }

    private static void generatePackMeta(Path datapacksDir) {
        Path packMcmeta = datapacksDir.resolve("pack.mcmeta");
        if (!Files.exists(packMcmeta)) {
            try {
                Files.createDirectories(packMcmeta.getParent());
                Files.writeString(packMcmeta, PACK_MCMETA, StandardCharsets.UTF_8);
                TeamSystem.LOGGER.info("Generated datapack mcmeta");
            } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to generate pack.mcmeta: {}", e.getMessage());
            }
        }
    }

    private static void generateLobbyDimension(Path datapacksDir) {
        Path dimTypeDir = datapacksDir.resolve("data").resolve("teamsystem").resolve("dimension_type");
        Path dimDir = datapacksDir.resolve("data").resolve("teamsystem").resolve("dimension");

        try {
            Files.createDirectories(dimTypeDir);
            Files.createDirectories(dimDir);
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to create dimension directories: {}", e.getMessage());
            return;
        }

        Path dimTypeFile = dimTypeDir.resolve("lobby.json");
        if (!Files.exists(dimTypeFile)) {
            try {
                Files.writeString(dimTypeFile, LOBBY_DIMENSION_TYPE, StandardCharsets.UTF_8);
                TeamSystem.LOGGER.info("Generated lobby dimension type datapack");
            } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to generate lobby dimension type: {}", e.getMessage());
            }
        }

        Path dimFile = dimDir.resolve("lobby.json");
        if (!Files.exists(dimFile)) {
            try {
                Files.writeString(dimFile, LOBBY_DIMENSION, StandardCharsets.UTF_8);
                TeamSystem.LOGGER.info("Generated lobby dimension datapack");
            } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to generate lobby dimension: {}", e.getMessage());
            }
        }
    }
}
