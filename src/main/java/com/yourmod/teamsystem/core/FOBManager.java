package com.yourmod.teamsystem.core;

import com.google.gson.*;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.client.FOBData;
import com.yourmod.teamsystem.network.FOBSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FOBManager {
    private static final String FILE_NAME = "teamsystem_fobs.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float MAX_HEALTH = 200.0f;

    private final List<SavedFOB> fobs = new ArrayList<>();
    private int nextFobId = 0;
    private static final long FOB_COOLDOWN_MS = 30_000;
    private final Map<UUID, Long> lastFOBTime = new HashMap<>();

    public static class SavedFOB {
        public int fobId;
        public String ownerUUID;
        public String ownerName;
        public String name;
        public int teamOrdinal;
        public String dimension;
        public int x, y, z;
        public float health;
        public long placedTime;
    }

    public FOBManager() {
        load();
    }

    public void clearPlayerCooldown(UUID uuid) {
        lastFOBTime.remove(uuid);
    }

    public String canPlaceFOB(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Team team = TeamSystem.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
        if (!team.isPlayable()) return "§cYou must be on a team";

        Squad squad = TeamSystem.getSquadManager().getPlayerSquad(uuid);
        if (squad == null || !squad.isLeader(uuid)) return "§cOnly squad leaders can place FOBs";

        int maxFOBs = getMaxFOBsForCurrentMap();
        long teamCount = fobs.stream().filter(f -> f.teamOrdinal == team.ordinal()).count();
        if (teamCount >= maxFOBs) return "§cTeam FOB limit reached (" + maxFOBs + ")";

        BlockPos pos = player.blockPosition();
        return validatePosition(player.serverLevel(), pos, team.ordinal(), uuid.toString());
    }

    public String placeFOB(ServerPlayer player, String name) {
        UUID uuid = player.getUUID();
        Team team = TeamSystem.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
        if (!team.isPlayable()) return "§cYou must be on a team to place FOBs";

        Squad squad = TeamSystem.getSquadManager().getPlayerSquad(uuid);
        if (squad == null || !squad.isLeader(uuid)) return "§cOnly squad leaders can place FOBs";

        int maxFOBs = getMaxFOBsForCurrentMap();
        long teamCount = fobs.stream().filter(f -> f.teamOrdinal == team.ordinal()).count();
        if (teamCount >= maxFOBs) return "§cTeam FOB limit reached (" + maxFOBs + ")";

        BlockPos pos = player.blockPosition();
        String validation = validatePosition(player.serverLevel(), pos, team.ordinal(), uuid.toString());
        if (validation != null) return validation;

        // If squad leader already has a FOB on same team — remove old one first (move)
        SavedFOB existing = fobs.stream()
            .filter(f -> f.ownerUUID.equals(uuid.toString()) && f.teamOrdinal == team.ordinal())
            .findFirst().orElse(null);

        // Check cooldown
        Long lastTime = lastFOBTime.get(uuid);
        if (lastTime != null && System.currentTimeMillis() - lastTime < FOB_COOLDOWN_MS) {
            int remain = (int) ((FOB_COOLDOWN_MS - (System.currentTimeMillis() - lastTime)) / 1000);
            return "§cWait " + remain + "s before placing another FOB";
        }

        // Check economy cost
        int fobCost = TeamSystem.getConfig().getFOBCost();
        int requiredSP = existing != null ? fobCost / 2 : fobCost;
        EconomyManager econ = TeamSystem.getEconomyManager();
        if (requiredSP > 0 && econ != null && econ.getSP(uuid) < requiredSP) {
            return "§cNot enough SP! FOB costs " + requiredSP + " SP";
        }

        if (existing != null) {
            removeFOBBlock(existing);
            fobs.remove(existing);
            broadcastToTeam(team, "§e[FOB] §f" + existing.name + " §eremoved by §f" + player.getName().getString());
        }

        // Deduct cost
        if (requiredSP > 0 && econ != null) {
            econ.addSP(uuid, -requiredSP);
            econ.syncAll(player);
        }

        SavedFOB fob = new SavedFOB();
        fob.fobId = nextFobId++;
        fob.ownerUUID = uuid.toString();
        fob.ownerName = player.getGameProfile().getName();
        fob.name = name;
        fob.teamOrdinal = team.ordinal();
        fob.dimension = player.serverLevel().dimension().location().toString();
        fob.x = pos.getX();
        fob.y = pos.getY();
        fob.z = pos.getZ();
        fob.health = MAX_HEALTH;
        fob.placedTime = player.serverLevel().getGameTime();
        fobs.add(fob);
        save();

        // Place beacon block as visual
        player.serverLevel().setBlock(pos, TeamSystem.RESPAWN_BEACON_BLOCK.get().defaultBlockState(), 3);

        lastFOBTime.put(uuid, System.currentTimeMillis());

        String action = existing != null ? "moved" : "placed";
        broadcastToTeam(team, "§a[FOB] §f" + name + " §a" + action + " by §f" + player.getName().getString());
        syncAll();
        return null;
    }

    private String validatePosition(ServerLevel level, BlockPos pos, int teamOrdinal, String skipOwnerUUID) {
        TeamSystemConfig config = TeamSystem.getConfig();
        int minEnemy = config.getMinDistanceFromEnemyBase();
        int minOwn = config.getMinDistanceFromOwnBase();

        String dimStr = level.dimension().location().toString();

        // Check distances from other FOBs (skip own FOB for move case)
        for (SavedFOB fob : fobs) {
            if (!fob.dimension.equals(dimStr)) continue;
            if (skipOwnerUUID != null && fob.ownerUUID.equals(skipOwnerUUID)) continue;
            double dist = distanceTo(pos, fob.x, fob.z);
            if (fob.teamOrdinal == teamOrdinal && dist < minOwn) {
                return "§cToo close to your team's FOB! (" + (int)dist + " blocks, min " + minOwn + ")";
            }
            if (fob.teamOrdinal != teamOrdinal && dist < minEnemy) {
                return "§cToo close to enemy FOB! (" + (int)dist + " blocks, min " + minEnemy + ")";
            }
        }

        // Check distances from team bases
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();
        Optional<MapConfig> mapOpt = mapPool.getCurrentMap();
        if (mapOpt.isPresent()) {
            MapConfig map = mapOpt.get();
            if (map.hasTeamSpawns()) {
                int[] natoSpawn = map.getNatoSpawn();
                int[] russiaSpawn = map.getRussiaSpawn();

                if (teamOrdinal == Team.NATO.ordinal() && natoSpawn != null && distanceTo(pos, natoSpawn) < minOwn) {
                    return "§cToo close to your base!";
                }
                if (teamOrdinal == Team.RUSSIA.ordinal() && russiaSpawn != null && distanceTo(pos, russiaSpawn) < minOwn) {
                    return "§cToo close to your base!";
                }

                int[] enemySpawn = teamOrdinal == Team.NATO.ordinal() ? russiaSpawn : natoSpawn;
                if (enemySpawn != null && distanceTo(pos, enemySpawn) < minEnemy) {
                    return "§cToo close to enemy base!";
                }
            }
        }

        return null;
    }

    private int getMaxFOBsForCurrentMap() {
        GameManager game = TeamSystem.getGameManager();
        if (game != null) {
            MapConfig map = game.getCurrentMap();
            if (map != null && map.getMaxFOBs() > 0) {
                return map.getMaxFOBs();
            }
        }
        return TeamSystem.getConfig().getMaxFOBsPerTeam();
    }

    public boolean damageFOB(int fobId, float damage) {
        SavedFOB fob = getFOBById(fobId);
        if (fob == null) return false;
        fob.health = Math.max(0, fob.health - damage);
        if (fob.health <= 0) {
            removeFOB(fobId);
            broadcastToTeam(Team.fromOrdinal(fob.teamOrdinal), "§c[FOB] §f" + fob.name + " §cdestroyed!");
            return true;
        }
        save();
        syncAll();
        return false;
    }

    public boolean removeFOB(int fobId) {
        SavedFOB fob = getFOBById(fobId);
        if (fob == null) return false;
        fobs.removeIf(f -> f.fobId == fobId);
        removeFOBBlock(fob);
        save();
        syncAll();
        return true;
    }

    public boolean removeFOBByName(ServerPlayer player, String name) {
        UUID uuid = player.getUUID();
        boolean removed = fobs.removeIf(f ->
            f.ownerUUID.equals(uuid.toString()) && f.name.equals(name));
        if (removed) {
            save();
            syncAll();
        }
        return removed;
    }

    private void removeFOBBlock(SavedFOB fob) {
        ServerLevel level = TeamSystem.getTeamManager().getServer()
            .getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(fob.dimension)));
        if (level != null) {
            BlockPos pos = new BlockPos(fob.x, fob.y, fob.z);
            if (level.getBlockState(pos).getBlock() == TeamSystem.RESPAWN_BEACON_BLOCK.get()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public List<SavedFOB> getFOBs() {
        return new ArrayList<>(fobs);
    }

    public List<SavedFOB> getFOBsForTeam(Team team) {
        return fobs.stream()
            .filter(f -> f.teamOrdinal == team.ordinal())
            .collect(Collectors.toList());
    }

    public List<SavedFOB> getFOBsInDimension(String dimension) {
        return fobs.stream()
            .filter(f -> f.dimension.equals(dimension))
            .collect(Collectors.toList());
    }

    public SavedFOB getFOBById(int fobId) {
        return fobs.stream().filter(f -> f.fobId == fobId).findFirst().orElse(null);
    }

    public SavedFOB getFOBAt(BlockPos pos, String dimension) {
        return fobs.stream()
            .filter(f -> f.dimension.equals(dimension) && f.x == pos.getX() && f.y == pos.getY() && f.z == pos.getZ())
            .findFirst().orElse(null);
    }

    public List<BlockPos> getRespawnPointsForTeam(Team team, String dimension) {
        List<BlockPos> points = new ArrayList<>();
        for (SavedFOB fob : fobs) {
            if (fob.teamOrdinal == team.ordinal() && fob.dimension.equals(dimension) && fob.health > 0) {
                points.add(new BlockPos(fob.x, fob.y, fob.z));
            }
        }
        return points;
    }

    public void clearAll() {
        fobs.clear();
        save();
        syncAll();
        TeamSystem.LOGGER.info("All FOBs cleared");
    }

    public void tickFOBs() {
        // Periodic save and health regen could go here
    }

    public void syncAll() {
        List<FOBData> fobDataList = fobs.stream()
            .map(f -> new FOBData(f.fobId, f.name, f.x + 0.5, f.y + 1.0, f.z + 0.5,
                f.dimension, f.teamOrdinal, f.health))
            .collect(Collectors.toList());

        FOBSyncPacket packet = new FOBSyncPacket(fobDataList);
        for (ServerPlayer player : TeamSystem.getTeamManager().getServer().getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        List<FOBData> fobDataList = fobs.stream()
            .map(f -> new FOBData(f.fobId, f.name, f.x + 0.5, f.y + 1.0, f.z + 0.5,
                f.dimension, f.teamOrdinal, f.health))
            .collect(Collectors.toList());

        FOBSyncPacket packet = new FOBSyncPacket(fobDataList);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void onBeaconBroken(BlockPos pos, com.yourmod.teamsystem.blockentity.RespawnBeaconBlockEntity beacon) {
        String ownerUUID = beacon.getOwnerUUID() != null ? beacon.getOwnerUUID().toString() : "";
        String name = beacon.getName();
        fobs.removeIf(f ->
            f.ownerUUID.equals(ownerUUID) && f.name.equals(name) &&
            f.x == pos.getX() && f.y == pos.getY() && f.z == pos.getZ());
        if (fobs.removeIf(f -> f.x == pos.getX() && f.y == pos.getY() && f.z == pos.getZ())) {
            save();
            syncAll();
        }
    }

    private void broadcastToTeam(Team team, String message) {
        if (team == Team.SPECTATOR) return;
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(message);
        for (ServerPlayer player : TeamSystem.getTeamManager().getPlayersByTeam(team)) {
            player.displayClientMessage(msg, false);
        }
    }

    private double distanceTo(BlockPos pos, int[] coords) {
        if (coords == null || coords.length < 2) return Double.MAX_VALUE;
        double dx = pos.getX() - coords[0];
        double dz = pos.getZ() - coords[1];
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double distanceTo(BlockPos pos, int x, int z) {
        double dx = pos.getX() - x;
        double dz = pos.getZ() - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void load() {
        Path path = TeamSystem.getTeamManager().getServer()
            .getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonArray arr = JsonParser.parseReader(reader).getAsJsonArray();
            fobs.clear();
            for (JsonElement elem : arr) {
                JsonObject obj = elem.getAsJsonObject();
                SavedFOB fob = new SavedFOB();
                fob.fobId = obj.get("fobId").getAsInt();
                fob.ownerUUID = obj.get("ownerUUID").getAsString();
                fob.ownerName = obj.get("ownerName").getAsString();
                fob.name = obj.get("name").getAsString();
                fob.teamOrdinal = obj.get("teamOrdinal").getAsInt();
                fob.dimension = obj.get("dimension").getAsString();
                fob.x = obj.get("x").getAsInt();
                fob.y = obj.get("y").getAsInt();
                fob.z = obj.get("z").getAsInt();
                fob.health = obj.has("health") ? obj.get("health").getAsFloat() : MAX_HEALTH;
                fob.placedTime = obj.has("placedTime") ? obj.get("placedTime").getAsLong() : 0;
                fobs.add(fob);
                if (fob.fobId >= nextFobId) nextFobId = fob.fobId + 1;
            }
            TeamSystem.LOGGER.info("Loaded {} FOBs", fobs.size());
        } catch (IOException e) {
            TeamSystem.LOGGER.warn("Failed to load FOBs: {}", e.getMessage());
        }
    }

    private void save() {
        Path path = TeamSystem.getTeamManager().getServer()
            .getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            JsonArray arr = new JsonArray();
            for (SavedFOB fob : fobs) {
                JsonObject obj = new JsonObject();
                obj.addProperty("fobId", fob.fobId);
                obj.addProperty("ownerUUID", fob.ownerUUID);
                obj.addProperty("ownerName", fob.ownerName);
                obj.addProperty("name", fob.name);
                obj.addProperty("teamOrdinal", fob.teamOrdinal);
                obj.addProperty("dimension", fob.dimension);
                obj.addProperty("x", fob.x);
                obj.addProperty("y", fob.y);
                obj.addProperty("z", fob.z);
                obj.addProperty("health", fob.health);
                obj.addProperty("placedTime", fob.placedTime);
                arr.add(obj);
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(arr, writer);
            }
        } catch (IOException e) {
            TeamSystem.LOGGER.error("Failed to save FOBs: {}", e.getMessage());
        }
    }
}
