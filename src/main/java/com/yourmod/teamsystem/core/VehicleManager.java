package com.yourmod.teamsystem.core;

import com.google.gson.*;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VehicleManager {
    private static final String CONFIG_DIR = "config/teamsystem";
    private static final String VEHICLES_FILE = CONFIG_DIR + "/vehicles.json";
    private static final String COOLDOWN_FILE = CONFIG_DIR + "/vehicle_cooldowns.json";

    private Map<String, VehicleData> vehicles = new HashMap<>();
    private final Set<UUID> spawnedVehicles = new HashSet<>();
    private final Map<UUID, UUID> vehicleToOwner = new HashMap<>();
    private final Set<UUID> playersHidingPlaques = new HashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private static Class<?> superbVehicleClass = null;
    private static boolean superbChecked = false;

    public VehicleManager() {
        loadVehicles();
        loadCooldowns();
        tryDetectSuperbClass();
    }

    private static void tryDetectSuperbClass() {
        if (superbChecked) return;
        superbChecked = true;
        try {
            superbVehicleClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            TeamSystem.LOGGER.info("Superb Warfare vehicle system detected");
        } catch (ClassNotFoundException e) {
            superbVehicleClass = null;
        }
    }

    public static boolean isSuperbVehicleEntity(Entity entity) {
        if (superbVehicleClass == null) return false;
        return superbVehicleClass.isAssignableFrom(entity.getClass());
    }

    public void registerSpawnedVehicle(Entity ent, UUID ownerUUID) {
        spawnedVehicles.add(ent.getUUID());
        if (ownerUUID != null) vehicleToOwner.put(ent.getUUID(), ownerUUID);
    }

    public boolean isSpawnedVehicle(UUID entityUUID) {
        return spawnedVehicles.contains(entityUUID);
    }

    public boolean isVehicleEntityType(Entity entity) {
        String entityTypeId = EntityType.getKey(entity.getType()).toString();
        for (VehicleData v : vehicles.values()) {
            if (entityTypeId.equals(v.getEntityType())) return true;
        }
        if (isSuperbVehicleEntity(entity)) return true;
        if (entity.getTeam() != null) {
            String teamName = entity.getTeam().getName();
            if ("NATO".equalsIgnoreCase(teamName) || "RUSSIA".equalsIgnoreCase(teamName)) return true;
        }
        return false;
    }

    public UUID getVehicleOwner(UUID entityUUID) {
        return vehicleToOwner.get(entityUUID);
    }

    public void unregisterSpawnedVehicle(UUID entityUUID) {
        spawnedVehicles.remove(entityUUID);
        vehicleToOwner.remove(entityUUID);
    }

    public int countActiveVehicles(Team team) {
        int count = 0;
        for (UUID uid : spawnedVehicles) {
            UUID ownerUuid = vehicleToOwner.get(uid);
            if (ownerUuid != null) {
                PlayerCombatData data = TeamSystem.getTeamManager().getOrCreatePlayerData(ownerUuid);
                if (data.getTeam() == team) count++;
            }
        }
        return count;
    }

    public boolean isOnCooldown(Team team, String vehicleId) {
        String key = team.name() + ":" + vehicleId;
        Long expiry = cooldowns.get(key);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(key);
            saveCooldowns();
            return false;
        }
        return true;
    }

    public void setCooldown(Team team, String vehicleId, int seconds) {
        String key = team.name() + ":" + vehicleId;
        cooldowns.put(key, System.currentTimeMillis() + seconds * 1000L);
        saveCooldowns();
    }

    public boolean isHidingPlaques(UUID playerUUID) {
        return playersHidingPlaques.contains(playerUUID);
    }

    public boolean toggleHidePlaque(UUID playerUUID) {
        if (playersHidingPlaques.contains(playerUUID)) {
            playersHidingPlaques.remove(playerUUID);
            return false;
        } else {
            playersHidingPlaques.add(playerUUID);
            return true;
        }
    }

    public boolean buyVehicle(ServerPlayer player, String vehicleId, TeamManager teamManager) {
        VehicleData vehicle = getVehicle(vehicleId);
        if (vehicle == null) return false;

        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        if (!playerTeam.isPlayable()) return false;

        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        if (vehicle.getTeam() != Team.SPECTATOR && vehicle.getTeam() != playerTeam) return false;
        if (playerRank < vehicle.getMinRankOrdinal()) return false;

        if (isOnCooldown(playerTeam, vehicleId)) return false;

        int activeCount = countActiveVehicles(playerTeam);
        if (activeCount >= vehicle.getMaxActive()) return false;

        TicketManager ticketMgr = TeamSystem.getTicketManager();
        if (ticketMgr != null) {
            int teamTickets = ticketMgr.getTickets(playerTeam);
            if (teamTickets < vehicle.getTicketCost()) return false;
            ticketMgr.setTickets(playerTeam, teamTickets - vehicle.getTicketCost());
        }

        EntityType<?> type = vehicle.resolveEntityType();
        if (type == null) return false;

        GameManager game = TeamSystem.getGameManager();
        MapConfig map = game != null ? game.getCurrentMap() : null;
        int[] spawnPos = new int[]{player.getBlockX(), player.getBlockY(), player.getBlockZ()};
        if (map != null) {
            int[] vs = playerTeam == Team.NATO ? map.getNatoVehicleSpawn() : map.getRussiaVehicleSpawn();
            if (vs != null && vs.length >= 3) spawnPos = vs;
        }

        ServerLevel level = (ServerLevel) player.level();
        CompoundTag nbt = vehicle.resolveNbt();
        if (nbt != null) {
            nbt.remove("UUID"); nbt.remove("uuid"); nbt.remove("Uuid");
        }
        int offsetIdx = countActiveVehicles(playerTeam);
        double angle = offsetIdx * 1.2;
        double dist = (offsetIdx / 3 + 1) * 3.0;
        double ox = Math.cos(angle) * dist;
        double oz = Math.sin(angle) * dist;
        Entity ent = type.create(level);
        if (ent == null) return false;
        if (nbt != null) ent.load(nbt);
        ent.setPos(spawnPos[0] + 0.5 + ox, spawnPos[1], spawnPos[2] + 0.5 + oz);
        level.addFreshEntity(ent);
        registerSpawnedVehicle(ent, player.getUUID());
        setCooldown(playerTeam, vehicleId, vehicle.getCooldownSeconds());

        TeamSystem.LOGGER.info("Player {} bought vehicle {} for team {}", player.getName().getString(), vehicleId, playerTeam);
        return true;
    }

    public void loadVehicles() {
        try {
            Path configPath = Paths.get(VEHICLES_FILE);
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
                for (JsonElement elem : arr) {
                    VehicleData vehicle = VehicleData.fromJson(elem.getAsJsonObject());
                    vehicles.put(vehicle.getVehicleId(), vehicle);
                }
                TeamSystem.LOGGER.info("Loaded {} vehicles", vehicles.size());
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to load vehicles: {}", e.getMessage());
        }
    }

    public void saveVehicles() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            Files.createDirectories(configPath);
            JsonArray arr = new JsonArray();
            for (VehicleData vehicle : vehicles.values()) arr.add(vehicle.toJson());
            Files.writeString(Paths.get(VEHICLES_FILE), new GsonBuilder().setPrettyPrinting().create().toJson(arr));
            TeamSystem.LOGGER.info("Saved {} vehicles", vehicles.size());
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to save vehicles: {}", e.getMessage());
        }
    }

    public void addVehicle(VehicleData vehicle) {
        vehicles.put(vehicle.getVehicleId(), vehicle);
        saveVehicles();
    }

    public void removeVehicle(String vehicleId) {
        if (vehicles.remove(vehicleId) != null) saveVehicles();
    }

    public VehicleData getVehicle(String vehicleId) {
        return vehicles.get(vehicleId);
    }

    public List<VehicleData> getAvailableVehicles(ServerPlayer player, TeamManager teamManager) {
        List<VehicleData> available = new ArrayList<>();
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        for (VehicleData vehicle : vehicles.values()) {
            if ((vehicle.getTeam() == Team.SPECTATOR || vehicle.getTeam() == playerTeam) &&
                playerRank >= vehicle.getMinRankOrdinal()) {
                available.add(vehicle);
            }
        }
        return available;
    }

    public Map<String, VehicleData> getVehicles() {
        return vehicles;
    }

    private void loadCooldowns() {
        try {
            Path path = Paths.get(COOLDOWN_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                long now = System.currentTimeMillis();
                for (String key : obj.keySet()) {
                    long expiry = obj.get(key).getAsLong();
                    if (expiry > now) cooldowns.put(key, expiry);
                }
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to load vehicle cooldowns: {}", e.getMessage());
        }
    }

    private void saveCooldowns() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            Files.createDirectories(configPath);
            JsonObject obj = new JsonObject();
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
                if (e.getValue() > now) obj.addProperty(e.getKey(), e.getValue());
            }
            Files.writeString(Paths.get(COOLDOWN_FILE), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to save vehicle cooldowns: {}", e.getMessage());
        }
    }
}
