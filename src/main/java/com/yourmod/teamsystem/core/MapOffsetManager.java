package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.GZIPInputStream;

public class MapOffsetManager {

    private static final java.util.concurrent.atomic.AtomicBoolean preloadDone = new java.util.concurrent.atomic.AtomicBoolean(false);

    public static boolean isPreloadDone() { return preloadDone.get(); }

    private static final int MAP_STRIDE = 30720;
    private static final int CHUNK_SHIFT = MAP_STRIDE / 16;
    private static final int SECTOR_BYTES = 4096;
    private static final int ENTRIES = 1024;

    public static int getStride() { return MAP_STRIDE; }
    public static int getZOffset(int mapIndex) { return mapIndex * MAP_STRIDE; }

    public static int getMapIndex(MapConfig map, java.util.List<MapConfig> allMaps) {
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

    public static void preloadAllMaps(MinecraftServer server, java.util.List<MapConfig> maps) {
        try {
            Path destDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions").resolve("teamsystem").resolve("map").resolve("region");
            Path sourcesPath = server.getWorldPath(LevelResource.ROOT).resolve("teamsystem_sources");

            if (maps.isEmpty()) return;

            try { Files.createDirectories(destDir); } catch (IOException e) {
                TeamSystem.LOGGER.error("Failed to create region dir: {}", e.getMessage());
                return;
            }

            int loaded = 0;
            for (int i = 0; i < maps.size(); i++) {
                MapConfig map = maps.get(i);
                Path sourceRegionDir = sourcesPath.resolve(map.getWorldFolder()).resolve("region");
                if (!Files.isDirectory(sourceRegionDir)) {
                    TeamSystem.LOGGER.warn("No source region folder for {}, skipping", map.getName());
                    continue;
                }

                int chunkShift = i * CHUNK_SHIFT;

                try (var files = Files.list(sourceRegionDir)) {
                    java.util.List<java.nio.file.Path> mcaFiles = files
                        .filter(f -> f.toString().endsWith(".mca"))
                        .collect(java.util.stream.Collectors.toList());
                    TeamSystem.LOGGER.info("Preloading {} ({} region files, zOffset={})...",
                        map.getName(), mcaFiles.size(), i * MAP_STRIDE);
                    int processed = 0;
                    for (var srcPath : mcaFiles) {
                        try {
                            rewriteRegionFile(srcPath, destDir, chunkShift);
                            processed++;
                            if (processed % 10 == 0 || processed == mcaFiles.size()) {
                                TeamSystem.LOGGER.info("  [{}/{}] {} preloaded", processed, mcaFiles.size(), map.getName());
                            }
                        } catch (Exception e) {
                            TeamSystem.LOGGER.error("Failed to process {}: {}", srcPath.getFileName(), e.getMessage());
                        }
                    }
                    loaded++;
                } catch (IOException e) {
                    TeamSystem.LOGGER.error("Failed to scan {}: {}", map.getName(), e.getMessage());
                }
            }
            TeamSystem.LOGGER.info("Preloaded {} maps with coordinate rewrite", loaded);
        } finally {
            preloadDone.set(true);
        }
    }

    private static void rewriteRegionFile(Path srcPath, Path destDir, int chunkShift) throws IOException {
        String name = srcPath.getFileName().toString();
        String[] parts = name.replace(".mca", "").split("\\.");
        int rx = Integer.parseInt(parts[1]);
        int rz = Integer.parseInt(parts[2]);

        long fileLen = srcPath.toFile().length();
        int headerSectors = 2;
        int numSectors = (int) (fileLen / SECTOR_BYTES);

        try (RandomAccessFile src = new RandomAccessFile(srcPath.toFile(), "r")) {
            byte[] header = new byte[SECTOR_BYTES];
            src.readFully(header);

            Map<Integer, byte[]> chunksToWrite = new LinkedHashMap<>();

            for (int localZ = 0; localZ < 32; localZ++) {
                for (int localX = 0; localX < 32; localX++) {
                    int entryIdx = localX * 32 + localZ;
                    int offset = readInt3(header, entryIdx * 4);
                    int count = header[entryIdx * 4 + 3] & 0xFF;
                    if (offset == 0 || count == 0) continue;

                    long chunkDataStart = (long) offset * SECTOR_BYTES;
                    src.seek(chunkDataStart);
                    int dataLen = src.readInt();
                    byte compressionType = src.readByte();
                    byte[] compressedData = new byte[dataLen - 1];
                    src.readFully(compressedData);

                    CompoundTag nbt;
                    if (compressionType == 2) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                        InflaterInputStream iis = new InflaterInputStream(bais);
                        nbt = NbtIo.read(new DataInputStream(iis));
                        iis.close();
                    } else if (compressionType == 1) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                        java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bais);
                        nbt = NbtIo.read(new DataInputStream(gzis));
                        gzis.close();
                    } else {
                        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                        nbt = NbtIo.read(new DataInputStream(bais));
                    }

                    int absChunkX = rx * 32 + localX;
                    int absChunkZ = rz * 32 + localZ;
                    nbt.putInt("xPos", absChunkX);
                    nbt.putInt("zPos", absChunkZ + chunkShift);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
                    DataOutputStream dos = new DataOutputStream(baos);
                    NbtIo.write(nbt, dos);
                    dos.close();
                    byte[] rawNbt = baos.toByteArray();

                    ByteArrayOutputStream compBaos = new ByteArrayOutputStream(4096);
                    DeflaterOutputStream dosComp = new DeflaterOutputStream(compBaos);
                    dosComp.write(rawNbt);
                    dosComp.close();
                    byte[] compressedOut = compBaos.toByteArray();

                    int newAbsZ = absChunkZ + chunkShift;
                    int destRegionZ = newAbsZ >> 5;
                    int destLocalZ = newAbsZ & 31;
                    int flatIdx = localX * 32 + destLocalZ;
                    int destKey = (destRegionZ << 16) | flatIdx;

                    byte[] entryData = new byte[5 + compressedOut.length];
                    entryData[0] = (byte) compressionType;
                    entryData[1] = (byte) (destRegionZ >> 8);
                    entryData[2] = (byte) destRegionZ;
                    entryData[3] = (byte) localX;
                    entryData[4] = (byte) destLocalZ;
                    System.arraycopy(compressedOut, 0, entryData, 5, compressedOut.length);

                    chunksToWrite.merge(destKey, entryData, (a, b) -> b);
                }
            }

            java.util.Map<Integer, java.util.List<byte[]>> byDestRegion = new java.util.HashMap<>();
            for (var entry : chunksToWrite.entrySet()) {
                int key = entry.getKey();
                int destRz = key >> 16;
                int regionKey = destRz;
                byDestRegion.computeIfAbsent(regionKey, k -> new java.util.ArrayList<>()).add(entry.getValue());
            }

            for (var regionEntry : byDestRegion.entrySet()) {
                int destRz = regionEntry.getKey();
                Path destPath = destDir.resolve("r." + rx + "." + destRz + ".mca");
                writeRegionFile(destPath, regionEntry.getValue());
            }
        }
    }

    private static void writeRegionFile(Path destPath, java.util.List<byte[]> chunkEntries) throws IOException {
        if (chunkEntries.isEmpty()) return;

        boolean exists = Files.exists(destPath);
        byte[] existingHeader = new byte[SECTOR_BYTES];
        java.util.List<Integer> freeSectors = new java.util.ArrayList<>();

        try (RandomAccessFile dst = exists ? new RandomAccessFile(destPath.toFile(), "rw") : null) {
            if (dst != null && dst.length() >= SECTOR_BYTES) {
                dst.readFully(existingHeader);
                for (int i = 2; i < dst.length() / SECTOR_BYTES; i++) {
                    boolean used = false;
                    for (int j = 0; j < ENTRIES; j++) {
                        int off = readInt3(existingHeader, j * 4);
                        int cnt = existingHeader[j * 4 + 3] & 0xFF;
                        if (off <= i && off + cnt > i) { used = true; break; }
                    }
                    if (!used) freeSectors.add(i);
                }
            }

            try (RandomAccessFile dest = new RandomAccessFile(destPath.toFile(), "rw")) {
                if (dest.length() < SECTOR_BYTES) {
                    dest.setLength(SECTOR_BYTES);
                } else {
                    dest.seek(0);
                    dest.readFully(existingHeader);
                }

                for (byte[] entryData : chunkEntries) {
                    byte compressionType = entryData[0];
                    int destRz = ((entryData[1] & 0xFF) << 8) | (entryData[2] & 0xFF);
                    int localX = entryData[3] & 0xFF;
                    int localZ = entryData[4] & 0xFF;
                    byte[] compressedOut = new byte[entryData.length - 5];
                    System.arraycopy(entryData, 5, compressedOut, 0, compressedOut.length);

                    int entryIdx = localX * 32 + localZ;
                    int dataLen = 1 + compressedOut.length;
                    int sectorsNeeded = (dataLen + 4 + SECTOR_BYTES - 1) / SECTOR_BYTES;

                    int sectorPos = -1;
                    for (int fi = 0; fi < freeSectors.size(); fi++) {
                        int fs = freeSectors.get(fi);
                        if (fs >= 2) {
                            long fsDataStart = (long) fs * SECTOR_BYTES;
                            if (fsDataStart + (long) sectorsNeeded * SECTOR_BYTES <= dest.length()) {
                                boolean freeRange = true;
                                for (int s = fs; s < fs + sectorsNeeded; s++) {
                                    if (!freeSectors.contains(s)) { freeRange = false; break; }
                                }
                                if (freeRange) {
                                    sectorPos = fs;
                                    for (int s = fs; s < fs + sectorsNeeded; s++)
                                        freeSectors.remove(Integer.valueOf(s));
                                    break;
                                }
                            }
                        }
                    }

                    if (sectorPos < 0) {
                        sectorPos = (int) (dest.length() / SECTOR_BYTES);
                    }

                    long writePos = (long) sectorPos * SECTOR_BYTES;
                    if (writePos + (long) sectorsNeeded * SECTOR_BYTES > dest.length())
                        dest.setLength(writePos + (long) sectorsNeeded * SECTOR_BYTES);

                    dest.seek(writePos);
                    dest.writeInt(dataLen);
                    dest.writeByte(compressionType);
                    dest.write(compressedOut);

                    for (int s = sectorPos; s < sectorPos + sectorsNeeded; s++) {
                        if (s != sectorPos) freeSectors.remove(Integer.valueOf(s));
                    }

                    dest.seek(entryIdx * 4);
                    writeInt3(dest, sectorPos);
                    dest.writeByte(sectorsNeeded);
                }

                dest.seek(SECTOR_BYTES);
                for (int i = 0; i < ENTRIES; i++) {
                    dest.writeInt((int) (System.currentTimeMillis() / 1000L));
                }
            }
        }
    }

    private static int readInt3(byte[] buf, int off) {
        return ((buf[off] & 0xFF) << 16) | ((buf[off + 1] & 0xFF) << 8) | (buf[off + 2] & 0xFF);
    }

    private static void writeInt3(RandomAccessFile raf, int val) throws IOException {
        raf.writeByte((val >> 16) & 0xFF);
        raf.writeByte((val >> 8) & 0xFF);
        raf.writeByte(val & 0xFF);
    }
}
