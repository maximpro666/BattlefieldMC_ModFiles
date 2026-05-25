package com.pigeostudios.pwp.core;

import com.google.gson.*;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.ammo.AmmoService;
import com.pigeostudios.pwp.ammo.FOBAmmoProvider;
import com.pigeostudios.pwp.client.FOBData;
import com.pigeostudios.pwp.network.FOBSyncPacket;
import com.pigeostudios.pwp.network.PacketHandler;
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
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FOBManager {
    private static final String FILE_NAME = "pwp_fobs.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float MAX_HEALTH = 200.0f;

    private final List<SavedFOB> fobs = new ArrayList<>();
    private int nextFobId = 0;
    private static final long FOB_COOLDOWN_MS = 30_000;
    private final Map<UUID, Long> lastFOBTime = new ConcurrentHashMap<>();
    private final Map<Integer, FOBAmmoProvider> fobAmmoProviders = new ConcurrentHashMap<>();

    public FOBManager() {
        load();
        for (SavedFOB fob : fobs) {
            registerAmmoProviderForFOB(fob);
        }
    }

    private void registerAmmoProviderForFOB(SavedFOB fob) {
        FOBAmmoProvider provider = new FOBAmmoProvider(
            Team.fromOrdinal(fob.teamOrdinal),
            new BlockPos(fob.x, fob.y, fob.z),
            fob.name
        );
        fobAmmoProviders.put(fob.fobId, provider);
        BattlefieldRuntime.getInstance().getAmmoService().registerProvider(provider);
    }

    private void unregisterAmmoProviderForFOB(int fobId) {
        FOBAmmoProvider provider = fobAmmoProviders.remove(fobId);
        if (provider != null) {
            BattlefieldRuntime.getInstance().getAmmoService().unregisterProvider(provider);
        }
    }

    public void clearPlayerCooldown(UUID uuid) {
        lastFOBTime.remove(uuid);
    }

    // Used by FOBService — places FOB without duplicate validation/BC checks
    public synchronized String placeFOBWithoutChecks(ServerPlayer player, String name, Team team, String validatedOwnerUUID) {
        UUID uuid = player.getUUID();
        SavedFOB existing = fobs.stream()
            .filter(f -> f.ownerUUID.equals(validatedOwnerUUID) && f.teamOrdinal == team.ordinal())
            .findFirst().orElse(null);

        if (existing != null) {
            removeFOBBlock(existing);
            fobs.remove(existing);
        }

        BlockPos pos = player.blockPosition();
        SavedFOB fob = new SavedFOB();
        fob.fobId = nextFobId++;
        fob.ownerUUID = validatedOwnerUUID;
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
        registerAmmoProviderForFOB(fob);
        save();
        player.serverLevel().setBlock(pos, PWP.RESPAWN_BEACON_BLOCK.get().defaultBlockState(), 3);

        String action = existing != null ? "moved" : "placed";
        broadcastToTeam(team, "\u00a7a[FOB] \u00a7f" + name + " \u00a7a" + action + " by \u00a7f" + player.getName().getString());
        syncAll();
        return null;
    }

    public String canPlaceFOB(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Team team = PWP.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
        if (!team.isPlayable()) return "§cYou must be on a team";

        Squad squad = PWP.getSquadManager().getPlayerSquad(uuid);
        if (squad == null || !squad.isLeader(uuid)) return "§cOnly squad leaders can place FOBs";

        int maxFOBs = getMaxFOBsForCurrentMap();
        long teamCount = fobs.stream().filter(f -> f.teamOrdinal == team.ordinal()).count();
        if (teamCount >= maxFOBs) return "§cTeam FOB limit reached (" + maxFOBs + ")";

        int fobCost = PWP.getConfig().getFOBCost();
        if (fobCost > 0) {
            boolean hasExisting = fobs.stream().anyMatch(f -> f.ownerUUID.equals(uuid.toString()) && f.teamOrdinal == team.ordinal());
            int requiredBC = hasExisting ? fobCost / 2 : fobCost;
            int bc = BattlefieldRuntime.getInstance().getBC(uuid);
            if (bc < requiredBC) return "§cNot enough BC! FOB costs " + requiredBC + " BC";
        }

        BlockPos pos = player.blockPosition();
        return validatePosition(player.serverLevel(), pos, team.ordinal(), uuid.toString());
    }

    public synchronized String placeFOB(ServerPlayer player, String name) {
        UUID uuid = player.getUUID();
        Team team = PWP.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
        if (!team.isPlayable()) return "§cYou must be on a team to place FOBs";

        Squad squad = PWP.getSquadManager().getPlayerSquad(uuid);
        if (squad == null || !squad.isLeader(uuid)) return "§cOnly squad leaders can place FOBs";

        int maxFOBs = getMaxFOBsForCurrentMap();
        long teamCount = fobs.stream().filter(f -> f.teamOrdinal == team.ordinal()).count();
        if (teamCount >= maxFOBs) return "§cTeam FOB limit reached (" + maxFOBs + ")";
        // If squad leader already has a FOB — allow move (existing != null checked later)
        SavedFOB existing = fobs.stream()
            .filter(f -> f.ownerUUID.equals(uuid.toString()) && f.teamOrdinal == team.ordinal())
            .findFirst().orElse(null);

        BlockPos pos = player.blockPosition();
        String validation = validatePosition(player.serverLevel(), pos, team.ordinal(), uuid.toString());
        if (validation != null) return validation;

        // Check cooldown
        Long lastTime = lastFOBTime.get(uuid);
        if (lastTime != null && System.currentTimeMillis() - lastTime < FOB_COOLDOWN_MS) {
            int remain = (int) ((FOB_COOLDOWN_MS - (System.currentTimeMillis() - lastTime)) / 1000);
            return "§cWait " + remain + "s before placing another FOB";
        }

        // Check economy cost (L5: atomic deduct, no TOCTOU)
        int fobCost = PWP.getConfig().getFOBCost();
        int requiredBC = existing != null ? fobCost / 2 : fobCost;
        BattlefieldRuntime rt = BattlefieldRuntime.getInstance();
        if (requiredBC > 0 && !rt.deductBC(uuid, requiredBC)) {
            return "§cNot enough BC! FOB costs " + requiredBC + " BC";
        }

        if (existing != null) {
            removeFOBBlock(existing);
            fobs.remove(existing);
            broadcastToTeam(team, "§e[FOB] §f" + existing.name + " §eremoved by §f" + player.getName().getString());
        }

        if (requiredBC > 0) {
            rt.syncAll(player);
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
        registerAmmoProviderForFOB(fob);
        save();

        // Place beacon block as visual
        player.serverLevel().setBlock(pos, PWP.RESPAWN_BEACON_BLOCK.get().defaultBlockState(), 3);

        lastFOBTime.put(uuid, System.currentTimeMillis());

        String action = existing != null ? "moved" : "placed";
        broadcastToTeam(team, "§a[FOB] §f" + name + " §a" + action + " by §f" + player.getName().getString());
        syncAll();
        return null;
    }

    public String validatePosition(ServerLevel level, BlockPos pos, int teamOrdinal, String skipOwnerUUID) {
        PWPConfig config = PWP.getConfig();
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
        MapPoolManager mapPool = PWP.getMapPoolManager();
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

            // Check distances from capture points
            int minCapture = config.getMinDistanceFromCapturePoint();
            for (MapConfig.CapturePointEntry cp : map.getCapturePoints()) {
                double dist = distanceTo(pos, cp.x, cp.z);
                if (dist < minCapture) {
                    return "§cToo close to a capture point! (" + (int)dist + " blocks, min " + minCapture + ")";
                }
            }
        }

        return null;
    }

    private int getMaxFOBsForCurrentMap() {
        GameManager game = PWP.getGameManager();
        if (game != null) {
            MapConfig map = game.getCurrentMap();
            if (map != null && map.getMaxFOBs() > 0) {
                return map.getMaxFOBs();
            }
        }
        return PWP.getConfig().getMaxFOBsPerTeam();
    }

    public synchronized boolean damageFOB(int fobId, float damage) {
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

    public synchronized boolean removeFOB(int fobId) {
        SavedFOB fob = getFOBById(fobId);
        if (fob == null) return false;
        fobs.removeIf(f -> f.fobId == fobId);
        unregisterAmmoProviderForFOB(fobId);
        removeFOBBlock(fob);
        save();
        syncAll();
        return true;
    }

    public synchronized boolean removeFOBByName(ServerPlayer player, String name) {
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
        ServerLevel level = PWP.getTeamManager().getServer()
            .getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(fob.dimension)));
        if (level != null) {
            BlockPos pos = new BlockPos(fob.x, fob.y, fob.z);
            if (level.getBlockState(pos).getBlock() == PWP.RESPAWN_BEACON_BLOCK.get()) {
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

    public synchronized void clearAll() {
        for (int id : List.copyOf(fobAmmoProviders.keySet())) {
            unregisterAmmoProviderForFOB(id);
        }
        fobs.clear();
        save();
        syncAll();
        PWP.LOGGER.info("All FOBs cleared");
    }

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

    public void tickFOBs() {
        // Periodic save and health regen could go here
    }

    public void syncAll() {
        List<FOBData> fobDataList = fobs.stream()
            .map(f -> new FOBData(f.fobId, f.name, f.x + 0.5, f.y + 1.0, f.z + 0.5,
                f.dimension, f.teamOrdinal, f.health))
            .collect(Collectors.toList());

        FOBSyncPacket packet = new FOBSyncPacket(fobDataList);
        for (ServerPlayer player : PWP.getTeamManager().getServer().getPlayerList().getPlayers()) {
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

    public synchronized void onBeaconBroken(BlockPos pos, com.pigeostudios.pwp.blockentity.RespawnBeaconBlockEntity beacon) {
        String ownerUUID = beacon.getOwnerUUID() != null ? beacon.getOwnerUUID().toString() : "";
        String name = beacon.getName();
        SavedFOB removed = null;
        for (SavedFOB f : fobs) {
            if (f.x == pos.getX() && f.y == pos.getY() && f.z == pos.getZ()) {
                removed = f;
                break;
            }
        }
        if (removed != null) {
            fobs.remove(removed);
            unregisterAmmoProviderForFOB(removed.fobId);
            save();
            syncAll();
        }
    }

    private void broadcastToTeam(Team team, String message) {
        if (team == Team.SPECTATOR) return;
        net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(message);
        for (ServerPlayer player : PWP.getTeamManager().getPlayersByTeam(team)) {
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

    private synchronized void load() {
        Path path = PWP.getTeamManager().getServer()
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
            PWP.LOGGER.info("Loaded {} FOBs", fobs.size());
        } catch (IOException e) {
            PWP.LOGGER.warn("Failed to load FOBs: {}", e.getMessage());
        }
    }

    private synchronized void save() {
        Path path = PWP.getTeamManager().getServer()
            .getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        Path tmp = path.resolveSibling(FILE_NAME + ".tmp");
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
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(arr, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to save FOBs: {}", e.getMessage());
        }
    }
}
