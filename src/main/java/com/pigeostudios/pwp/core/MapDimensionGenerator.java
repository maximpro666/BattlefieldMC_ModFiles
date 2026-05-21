package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapDimensionGenerator {

    private static final String MAP_DIMENSION = """
        {
          "type": "minecraft:overworld",
          "generator": {
            "type": "minecraft:flat",
            "settings": {
              "biome": "minecraft:the_void",
              "layers": [],
              "features": false,
              "structure_overrides": []
            }
          }
        }
        """;

    private static final String LOBBY_DIMENSION = """
        {
          "type": "pwp:lobby",
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
            "description": "PWP Dimensions"
          }
        }
        """;

    public static void generateDimensionDatapacks(MinecraftServer server) {
        Path datapacksDir = server.getWorldPath(LevelResource.ROOT).resolve("datapacks").resolve("pwp_maps");

        generatePackMeta(datapacksDir);
        generateLobbyDimension(datapacksDir);
        generateMapDimension(datapacksDir);
    }

    private static void generateMapDimension(Path datapacksDir) {
        Path dimDir = datapacksDir.resolve("data").resolve("pwp").resolve("dimension");
        try {
            Files.createDirectories(dimDir);
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to create dimension directory: {}", e.getMessage());
            return;
        }

        Path dimFile = dimDir.resolve("map.json");
        try {
            Files.writeString(dimFile, MAP_DIMENSION, StandardCharsets.UTF_8);
            PWP.LOGGER.info("Generated map dimension datapack");
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to generate map dimension: {}", e.getMessage());
        }
    }

    private static void generatePackMeta(Path datapacksDir) {
        Path packMcmeta = datapacksDir.resolve("pack.mcmeta");
        try {
            Files.createDirectories(packMcmeta.getParent());
            Files.writeString(packMcmeta, PACK_MCMETA, StandardCharsets.UTF_8);
            PWP.LOGGER.info("Generated datapack mcmeta");
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to generate pack.mcmeta: {}", e.getMessage());
        }
    }

    private static void generateLobbyDimension(Path datapacksDir) {
        Path dimTypeDir = datapacksDir.resolve("data").resolve("pwp").resolve("dimension_type");
        Path dimDir = datapacksDir.resolve("data").resolve("pwp").resolve("dimension");

        try {
            Files.createDirectories(dimTypeDir);
            Files.createDirectories(dimDir);
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to create dimension directories: {}", e.getMessage());
            return;
        }

        Path dimTypeFile = dimTypeDir.resolve("lobby.json");
        try {
            Files.writeString(dimTypeFile, LOBBY_DIMENSION_TYPE, StandardCharsets.UTF_8);
            PWP.LOGGER.info("Generated lobby dimension type datapack");
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to generate lobby dimension type: {}", e.getMessage());
        }

        Path dimFile = dimDir.resolve("lobby.json");
        try {
            Files.writeString(dimFile, LOBBY_DIMENSION, StandardCharsets.UTF_8);
            PWP.LOGGER.info("Generated lobby dimension datapack");
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to generate lobby dimension: {}", e.getMessage());
        }
    }
}
