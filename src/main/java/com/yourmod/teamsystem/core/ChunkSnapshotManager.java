package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.phys.AABB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkSnapshotManager {

    private final Map<String, Map<Long, byte[]>> snapshots = new HashMap<>();

    public boolean hasSnapshot(String mapName) {
        return snapshots.containsKey(mapName);
    }

    public void takeSnapshot(ServerLevel level, MapConfig map, int zOffset, java.nio.file.Path sourcesPath) {
        String key = map.getName();
        Map<Long, byte[]> chunkData = new LinkedHashMap<>();

        java.nio.file.Path sourceRegionDir = sourcesPath.resolve(map.getWorldFolder()).resolve("region");
        if (!java.nio.file.Files.isDirectory(sourceRegionDir)) {
            TeamSystem.LOGGER.warn("Source region folder not found for '{}', cannot take snapshot", key);
            return;
        }

        TeamSystem.LOGGER.info("Taking snapshot for '{}' (zOffset={})", key, zOffset);

        int chunkShift = zOffset >> 4;
        int count = 0;

        try (var files = java.nio.file.Files.list(sourceRegionDir)) {
            java.util.List<java.nio.file.Path> regionFiles = files
                .filter(f -> f.toString().endsWith(".mca"))
                .collect(java.util.stream.Collectors.toList());

            for (java.nio.file.Path srcPath : regionFiles) {
                String name = srcPath.getFileName().toString();
                String[] parts = name.replace(".mca", "").split("\\.");
                int rx = Integer.parseInt(parts[1]);
                int rz = Integer.parseInt(parts[2]);

                byte[] header = new byte[4096];
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(srcPath.toFile(), "r")) {
                    raf.readFully(header);
                }

                for (int lz = 0; lz < 32; lz++) {
                    for (int lx = 0; lx < 32; lx++) {
                        int entryIdx = lx * 32 + lz;
                        int offset = ((header[entryIdx * 4] & 0xFF) << 16)
                            | ((header[entryIdx * 4 + 1] & 0xFF) << 8)
                            | (header[entryIdx * 4 + 2] & 0xFF);
                        int count2 = header[entryIdx * 4 + 3] & 0xFF;
                        if (offset == 0 || count2 == 0) continue;

                        int absChunkX = rx * 32 + lx;
                        int absChunkZ = rz * 32 + lz + chunkShift;

                        LevelChunk chunk = level.getChunk(absChunkX, absChunkZ);
                        if (chunk == null) continue;

                        try {
                            CompoundTag tag = ChunkSerializer.write(level, chunk);

                            ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                            NbtIo.writeCompressed(tag, baos);
                            chunkData.put(ChunkPos.asLong(absChunkX, absChunkZ), baos.toByteArray());
                            count++;
                        } catch (Exception e) {
                            TeamSystem.LOGGER.error("Failed to snapshot chunk ({},{}): {}",
                                absChunkX, absChunkZ, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to scan source regions for '{}': {}", key, e.getMessage());
        }

        snapshots.put(key, chunkData);
        int totalKB = chunkData.values().stream().mapToInt(b -> b.length).sum() / 1024;
        TeamSystem.LOGGER.info("Snapshot saved for '{}' ({} chunks, ~{} KB)", key, count, totalKB);
    }

    public boolean restoreSnapshot(ServerLevel level, MapConfig map, int zOffset) {
        String key = map.getName();
        Map<Long, byte[]> chunkData = snapshots.get(key);
        if (chunkData == null) {
            TeamSystem.LOGGER.warn("No snapshot found for '{}'", key);
            return false;
        }

        TeamSystem.LOGGER.info("Restoring snapshot for '{}' ({} chunks)", key, chunkData.size());

        int count = 0;
        for (var entry : chunkData.entrySet()) {
            long chunkPosLong = entry.getKey();
            int cx = ChunkPos.getX(chunkPosLong);
            int cz = ChunkPos.getZ(chunkPosLong);

            try {
                LevelChunk existing = level.getChunk(cx, cz);
                if (existing == null) continue;

                CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(entry.getValue()));

                tag.putInt("xPos", cx);
                tag.putInt("zPos", cz);

                ProtoChunk proto = ChunkSerializer.read(level, level.getPoiManager(), new ChunkPos(cx, cz), tag);

                LevelChunkSection[] newSections = proto.getSections();
                LevelChunkSection[] oldSections = existing.getSections();
                for (int i = 0; i < Math.min(newSections.length, oldSections.length); i++) {
                    if (newSections[i] != null) {
                        oldSections[i] = newSections[i];
                    }
                }

                existing.getBlockEntities().clear();
                for (var beEntry : proto.getBlockEntities().entrySet()) {
                    try {
                        existing.setBlockEntity(beEntry.getValue());
                    } catch (Exception ignored) {
                    }
                }

                existing.setUnsaved(true);
                count++;
            } catch (Exception e) {
                TeamSystem.LOGGER.error("Failed to restore chunk ({},{}): {}", cx, cz, e.getMessage());
            }
        }

        AABB zone = new AABB(-2048, -64, zOffset - 16, 2048, 320, zOffset + MapOffsetManager.getStride() + 16);
        java.util.List<Entity> toRemove = new java.util.ArrayList<>();
        for (Entity e : level.getEntities().getAll()) {
            if (e != null && !(e instanceof Player) && zone.contains(e.getX(), e.getY(), e.getZ())) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove) e.discard();

        FOBManager fob = TeamSystem.getFOBManager();
        if (fob != null) fob.clearAll();
        RespawnManager rm = TeamSystem.getRespawnManager();
        if (rm != null) rm.clearAllBeacons();
        MarkerManager mm = TeamSystem.getMarkerManager();
        if (mm != null) mm.clearMarkers();

        TeamSystem.LOGGER.info("Snapshot restore complete for '{}' ({} chunks, {} entities removed)", key, count, toRemove.size());
        return true;
    }

    public void clearSnapshot(String mapName) {
        snapshots.remove(mapName);
    }
}
