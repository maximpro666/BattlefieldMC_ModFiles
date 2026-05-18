package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

public class MapOffsetManager {

    private static final int MAP_STRIDE = 30720;

    public static int getStride() {
        return MAP_STRIDE;
    }

    public static int getMapIndex(MapConfig map, List<MapConfig> allMaps) {
        for (int i = 0; i < allMaps.size(); i++) {
            if (allMaps.get(i).getWorldFolder().equals(map.getWorldFolder()))
                return i;
        }
        return 0;
    }

    public static BlockPos toWorld(BlockPos local, int mapIndex) {
        return local.offset(0, 0, mapIndex * MAP_STRIDE);
    }

    public static BlockPos toLocal(BlockPos world, int mapIndex) {
        return world.offset(0, 0, -(mapIndex * MAP_STRIDE));
    }

    public static int getZOffset(int mapIndex) {
        return mapIndex * MAP_STRIDE;
    }

    public static void preloadAllMaps(MinecraftServer server, List<MapConfig> maps) {
        Path destDir = server.getWorldPath(LevelResource.ROOT)
            .resolve("dimensions").resolve("teamsystem").resolve("map").resolve("region");
        Path sourcesPath = server.getWorldPath(LevelResource.ROOT)
            .resolve("teamsystem_sources");

        if (maps.isEmpty()) return;

        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to create map region dir: {}", e.getMessage());
            return;
        }

        int loaded = 0;
        for (int i = 0; i < maps.size(); i++) {
            MapConfig map = maps.get(i);
            Path sourceRegionDir = sourcesPath.resolve(map.getWorldFolder()).resolve("region");
            if (!Files.isDirectory(sourceRegionDir)) {
                TeamSystem.LOGGER.warn("Source region folder missing for map {}, skipping preload", map.getName());
                continue;
            }

            int zRegionOffset = i * MAP_STRIDE / 512;

            try (Stream<Path> files = Files.list(sourceRegionDir)) {
                files.filter(f -> f.toString().endsWith(".mca"))
                    .filter(f -> !Files.isDirectory(f))
                    .forEach(src -> {
                        String name = src.getFileName().toString();
                        try {
                            String[] parts = name.replace(".mca", "").split("\\.");
                            int rx = Integer.parseInt(parts[1]);
                            int rz = Integer.parseInt(parts[2]);
                            int newRz = rz + zRegionOffset;
                            Path dest = destDir.resolve("r." + rx + "." + newRz + ".mca");
                            if (!Files.exists(dest)) {
                                Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
                            }
                        } catch (Exception e) {
                            TeamSystem.LOGGER.error("Failed to preload {}: {}", name, e.getMessage());
                        }
                    });
                loaded++;
            } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to scan source dir for {}: {}", map.getName(), e.getMessage());
            }
        }
        TeamSystem.LOGGER.info("Preloaded {} maps into teamsystem:map", loaded);
    }
}
