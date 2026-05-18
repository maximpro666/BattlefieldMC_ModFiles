package com.yourmod.teamsystem.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.blockentity.RespawnBeaconBlockEntity;
import net.minecraft.core.BlockPos;
import static com.yourmod.teamsystem.core.ChatHelper.*;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Collections;

public class RespawnManager {
    private static final String FILE_NAME = "teamsystem_respawn_beacons.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final MinecraftServer server;
    private final List<SavedBeacon> beacons = new ArrayList<>();
    private RespawnConfig config;
    private final Map<String, List<int[]>> capturePoints = new HashMap<>();

    public static class SavedBeacon {
        public String uuid;
        public String ownerName;
        public String name;
        public int teamOrdinal;
        public String dimension;
        public int x, y, z;
        public long placedTime;
    }

    public RespawnManager(MinecraftServer server) {
        this.server = server;
        this.config = RespawnConfig.load(server);
        load();
    }

    public RespawnConfig getConfig() { return config; }
    public void reloadConfig() { this.config = RespawnConfig.load(server); }

    public String placeBeacon(ServerPlayer player, String name) {
        UUID uuid = player.getUUID();
        BlockPos pos = player.blockPosition();

        int teamOrdinal = TeamSystem.getTeamManager().getOrCreatePlayerData(uuid).getTeam().ordinal();

        long count = beacons.stream().filter(b -> uuid.toString().equals(b.uuid)).count();
        if (count >= config.getMaxBeaconsPerPlayer()) {
            return "Maximum beacons reached (" + config.getMaxBeaconsPerPlayer() + ")";
        }

        String validation = validatePosition(player.serverLevel(), pos, teamOrdinal);
        if (validation != null) return validation;

        player.serverLevel().setBlock(pos, TeamSystem.RESPAWN_BEACON_BLOCK.get().defaultBlockState(), 3);
        if (player.serverLevel().getBlockEntity(pos) instanceof RespawnBeaconBlockEntity beacon) {
            beacon.setOwner(uuid, player.getGameProfile().getName(), teamOrdinal, name);
        }

        SavedBeacon sb = new SavedBeacon();
        sb.uuid = uuid.toString();
        sb.ownerName = player.getGameProfile().getName();
        sb.name = name;
        sb.teamOrdinal = teamOrdinal;
        sb.dimension = player.serverLevel().dimension().location().toString();
        sb.x = pos.getX();
        sb.y = pos.getY();
        sb.z = pos.getZ();
        sb.placedTime = player.serverLevel().getGameTime();
        beacons.add(sb);
        save();

        return null;
    }

    private String validatePosition(ServerLevel level, BlockPos pos, int teamOrdinal) {
        int minEnemy = config.getMinDistanceFromEnemyBase();
        int minCapture = config.getMinDistanceFromCapturePoint();
        int minOwn = config.getMinDistanceFromOwnBase();

        String dimStr = level.dimension().location().toString();
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();
        Optional<MapConfig> mapOpt = mapPool.getCurrentMap();

        if (mapOpt.isPresent()) {
            MapConfig map = mapOpt.get();
            String worldFolder = map.getWorldFolder();

            if (map.hasTeamSpawns()) {
                int[] natoSpawn = map.getNatoSpawn();
                int[] russiaSpawn = map.getRussiaSpawn();
                int baseRadius = map.getBaseRadius();

                if (teamOrdinal == Team.NATO.ordinal()) {
                    if (distanceTo(pos, natoSpawn) < minOwn) {
                        return "Too close to your own base! (" + minOwn + " blocks minimum)";
                    }
                } else if (teamOrdinal == Team.RUSSIA.ordinal()) {
                    if (distanceTo(pos, russiaSpawn) < minOwn) {
                        return "Too close to your own base! (" + minOwn + " blocks minimum)";
                    }
                }

                int[] enemySpawn = teamOrdinal == Team.NATO.ordinal() ? russiaSpawn : natoSpawn;
                if (distanceTo(pos, enemySpawn) < minEnemy) {
                    return "Too close to enemy base! (" + minEnemy + " blocks minimum)";
                }
            }
        }

        List<int[]> points = capturePoints.getOrDefault(dimStr, Collections.emptyList());
        for (int[] cp : points) {
            if (distanceTo(pos, cp) < minCapture) {
                return "Too close to a capture point! (" + minCapture + " blocks minimum)";
            }
        }

        return null;
    }

    private double distanceTo(BlockPos pos, int[] coords) {
        if (coords == null || coords.length < 2) return Double.MAX_VALUE;
        double dx = pos.getX() - coords[0];
        double dz = pos.getZ() - coords[1];
        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean removeBeacon(String name, ServerPlayer player) {
        boolean removed = beacons.removeIf(b ->
            b.uuid.equals(player.getUUID().toString()) && b.name.equals(name));
        if (removed) {
            removeBeaconBlocks(player.getUUID().toString(), name);
            save();
        }
        return removed;
    }

    public void removeAllBeacons(String playerUUID) {
        beacons.removeIf(b -> b.uuid.equals(playerUUID));
        removeBeaconBlocks(playerUUID, null);
        save();
    }

    private void removeBeaconBlocks(String playerUUID, String name) {
        for (SavedBeacon b : beacons) {
            if (b.uuid.equals(playerUUID) && (name == null || b.name.equals(name))) {
                ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(b.dimension)));
                if (level != null) {
                    BlockPos pos = new BlockPos(b.x, b.y, b.z);
                    if (level.getBlockEntity(pos) instanceof RespawnBeaconBlockEntity) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    public void onBeaconBroken(BlockPos pos, RespawnBeaconBlockEntity beacon) {
        String ownerUUID = beacon.getOwnerUUID() != null ? beacon.getOwnerUUID().toString() : "";
        beacons.removeIf(b ->
            b.uuid.equals(ownerUUID) && b.name.equals(beacon.getName()) &&
            b.x == pos.getX() && b.y == pos.getY() && b.z == pos.getZ());
        save();
    }

    public List<SavedBeacon> getBeaconsInDimension(String dimension) {
        return beacons.stream()
            .filter(b -> b.dimension.equals(dimension))
            .collect(Collectors.toList());
    }

    public List<SavedBeacon> getBeaconsForPlayer(UUID uuid) {
        return beacons.stream()
            .filter(b -> b.uuid.equals(uuid.toString()))
            .collect(Collectors.toList());
    }

    public SavedBeacon getBeaconByName(UUID uuid, String name) {
        return beacons.stream()
            .filter(b -> b.uuid.equals(uuid.toString()) && b.name.equals(name))
            .findFirst().orElse(null);
    }

    public void setCapturePointsForDimension(String dimStr, List<int[]> points) {
        capturePoints.put(dimStr, points);
    }

    public List<BlockPos> getRespawnPointsForPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Team team = TeamSystem.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
        if (!team.isPlayable()) return Collections.emptyList();

        String dimStr = player.serverLevel().dimension().location().toString();
        List<BlockPos> points = new ArrayList<>();

        // Add player's own beacons
        for (SavedBeacon b : beacons) {
            if (b.uuid.equals(uuid.toString()) && b.dimension.equals(dimStr)) {
                points.add(new BlockPos(b.x, b.y, b.z));
            }
        }

        // Add team FOBs
        FOBManager fobManager = TeamSystem.getFOBManager();
        if (fobManager != null) {
            points.addAll(fobManager.getRespawnPointsForTeam(team, dimStr));
        }

        return points;
    }

    public void respawnPlayerAtBeacon(ServerPlayer player, String beaconName) {
        SavedBeacon beacon = getBeaconByName(player.getUUID(), beaconName);
        if (beacon == null) return;

        Team team = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
        if (team.isPlayable()) {
            TicketManager tm = TeamSystem.getTicketManager();
            if (tm != null) tm.deductTicket(team);
            player.sendSystemMessage(error("-1 ticket for respawn"));
        }

        ServerLevel dest = server.getLevel(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new net.minecraft.resources.ResourceLocation(beacon.dimension)));
        if (dest == null) dest = server.overworld();

        BlockPos pos = new BlockPos(beacon.x, beacon.y, beacon.z);
        player.teleportTo(dest, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        player.fallDistance = 0;
    }

    private void load() {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<SavedBeacon>>() {}.getType();
            List<SavedBeacon> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                beacons.clear();
                beacons.addAll(loaded);
            }
            TeamSystem.LOGGER.info("Loaded {} respawn beacons", beacons.size());
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to load respawn beacons: {}", e.getMessage());
        }
    }

    private void save() {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(beacons, writer);
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to save respawn beacons: {}", e.getMessage());
        }
    }
}
