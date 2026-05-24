package com.pigeostudios.pwp.core;

import com.google.gson.*;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.vehicle.VehicleDefinition;
import com.pigeostudios.pwp.vehicle.VehicleDefinitionRegistry;
import com.pigeostudios.pwp.vehicle.adapter.VehicleAdapterRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VehicleManager {
    private static final String COOLDOWN_FILE = "config/pwp/vehicle_cooldowns.json";

    private final Set<UUID> spawnedVehicles = new HashSet<>();
    private final Map<UUID, UUID> vehicleToOwner = new HashMap<>();
    private final Set<UUID> playersHidingPlaques = new HashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private boolean cooldownsLoaded = false;

    public VehicleManager() {
        loadCooldowns();
    }

    // ===== Vehicle Lookup =====

    public VehicleDefinition getDefinition(String vehicleId) {
        return BattlefieldRuntime.getInstance().getVehicleDefRegistry().get(vehicleId);
    }

    @Deprecated
    public VehicleData getVehicle(String vehicleId) {
        VehicleDefinition def = getDefinition(vehicleId);
        if (def != null) {
            VehicleData data = new VehicleData(vehicleId, def.getDisplayName(),
                Team.SPECTATOR, 0, def.getTicketCost());
            data.setCooldownSeconds(def.getCooldownSeconds());
            data.setMaxActive(4);
            return data;
        }
        return null;
    }

    // ===== Spawned Vehicle Tracking =====

    public void registerSpawnedVehicle(Entity ent, UUID ownerUUID) {
        spawnedVehicles.add(ent.getUUID());
        if (ownerUUID != null) vehicleToOwner.put(ent.getUUID(), ownerUUID);
    }

    public boolean isSpawnedVehicle(UUID entityUUID) {
        return spawnedVehicles.contains(entityUUID);
    }

    public boolean isVehicleEntityType(Entity entity) {
        if (spawnedVehicles.contains(entity.getUUID())) return true;
        VehicleAdapterRegistry registry = BattlefieldRuntime.getInstance().getVehicleAdapterRegistry();
        if (registry.findAdapter(entity) != null) return true;
        String entityTypeId = EntityType.getKey(entity.getType()).toString();
        for (VehicleDefinition def : BattlefieldRuntime.getInstance().getVehicleDefRegistry().getAll()) {
            if (entityTypeId.equals(def.getEntityType())) return true;
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

    public void unregisterPlayerVehicles(UUID ownerUUID) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : vehicleToOwner.entrySet()) {
            if (ownerUUID.equals(entry.getValue())) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID entUUID : toRemove) {
            spawnedVehicles.remove(entUUID);
            vehicleToOwner.remove(entUUID);
        }
    }

    public int countActiveVehicles(Team team) {
        int count = 0;
        for (UUID uid : spawnedVehicles) {
            UUID ownerUuid = vehicleToOwner.get(uid);
            if (ownerUuid != null) {
                PlayerCombatData data = PWP.getTeamManager().getOrCreatePlayerData(ownerUuid);
                if (data.getTeam() == team) count++;
            }
        }
        return count;
    }

    public int countPlayerVehicles(UUID ownerUUID) {
        int count = 0;
        for (UUID vUid : spawnedVehicles) {
            UUID owner = vehicleToOwner.get(vUid);
            if (ownerUUID.equals(owner)) count++;
        }
        return count;
    }

    // ===== Upkeep System Access =====

    public Collection<UUID> getSpawnedVehicleIds() {
        return new ArrayList<>(spawnedVehicles);
    }

    public UUID getOwnerForVehicle(UUID vehicleId) {
        return vehicleToOwner.get(vehicleId);
    }

    // ===== Cooldowns =====

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

    // ===== Plaque Visibility =====

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

    // ===== Buy Vehicle =====

    public Component buyVehicle(ServerPlayer player, String vehicleId, TeamManager teamManager) {
        VehicleDefinition def = getDefinition(vehicleId);
        if (def == null) return Component.translatable("pwp.chat.vehicle.not_found", vehicleId);

        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        if (!playerTeam.isPlayable()) return Component.translatable("pwp.chat.vehicle.not_on_team");

        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        if (playerRank < 0) return Component.translatable("pwp.chat.vehicle.requires_rank", 0);

        if (isOnCooldown(playerTeam, vehicleId)) {
            long remaining = (cooldowns.get(playerTeam.name() + ":" + vehicleId) - System.currentTimeMillis()) / 1000;
            return Component.translatable("pwp.chat.vehicle.cooldown", remaining);
        }

        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        int bcCost = def.getCosts().getDeployBC();
        int vcCost = def.getCosts().getDeployVC();

        if (bcCost > 0 && !runtime.deductBC(player.getUUID(), bcCost)) {
            return Component.translatable("pwp.chat.vehicle.insufficient_bc", bcCost);
        }
        if (vcCost > 0 && !runtime.deductVC(playerTeam, vcCost)) {
            if (bcCost > 0) runtime.addBC(player.getUUID(), bcCost);
            return Component.translatable("pwp.chat.vehicle.insufficient_vc", vcCost);
        }

        int activeCount = countActiveVehicles(playerTeam);
        int popLimit = def.getPopulationLimit(runtime.getActiveFrontlineCount());
        if (activeCount >= popLimit) return Component.translatable("pwp.chat.vehicle.limit_reached", popLimit);

        String entityTypeStr = def.getEntityType();
        if (entityTypeStr == null || entityTypeStr.isEmpty()) return Component.translatable("pwp.chat.vehicle.no_entity_type");

        EntityType<?> type = def.resolveEntityType();
        if (type == null) return Component.translatable("pwp.chat.vehicle.invalid_entity", entityTypeStr);

        GameManager game = PWP.getGameManager();
        MapConfig map = game != null ? game.getCurrentMap() : null;
        int[] spawnPos = new int[]{player.getBlockX(), player.getBlockY(), player.getBlockZ()};
        if (map != null) {
            int[] vs = playerTeam == Team.NATO ? map.getNatoVehicleSpawn() : map.getRussiaVehicleSpawn();
            if (vs != null && vs.length >= 3) spawnPos = vs;
        }

        ServerLevel level = (ServerLevel) player.level();
        CompoundTag nbt = def.resolveNbt();
        if (nbt != null) {
            nbt = nbt.copy();
            nbt.remove("UUID"); nbt.remove("uuid"); nbt.remove("Uuid");
        }
        int offsetIdx = countActiveVehicles(playerTeam);
        double angle = offsetIdx * 1.2;
        double dist = (offsetIdx / 3 + 1) * 3.0;
        double ox = Math.cos(angle) * dist;
        double oz = Math.sin(angle) * dist;
        Entity ent = type.create(level);
        if (ent == null) return Component.translatable("pwp.chat.vehicle.create_error");
        if (nbt != null) ent.load(nbt);
        ent.setPos(spawnPos[0] + 0.5 + ox, spawnPos[1], spawnPos[2] + 0.5 + oz);
        level.addFreshEntity(ent);
        player.startRiding(ent, true);
        registerSpawnedVehicle(ent, player.getUUID());
        setCooldown(playerTeam, vehicleId, def.getCooldownSeconds());

        runtime.trackVehicleSpawn(ent.getUUID(), playerTeam, vehicleId);

        PWP.LOGGER.info("Player {} bought vehicle {} for team {}", player.getName().getString(), vehicleId, playerTeam);
        return null;
    }

    // ===== Available Vehicles =====

    public List<VehicleDefinition> getAvailableDefinitions(ServerPlayer player, TeamManager teamManager) {
        List<VehicleDefinition> available = new ArrayList<>();
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        VehicleDefinitionRegistry reg = BattlefieldRuntime.getInstance().getVehicleDefRegistry();
        for (VehicleDefinition def : reg.getAll()) {
            String faction = def.getFaction();
            if ("ALL".equals(faction) ||
                (playerTeam == Team.NATO && "NATO".equals(faction)) ||
                (playerTeam == Team.RUSSIA && "RUSSIA".equals(faction))) {
                available.add(def);
            }
        }
        if (available.isEmpty()) {
            PWP.LOGGER.warn("No vehicle definitions available! Registry has {} definitions.", reg.getAll().size());
        }
        return available;
    }

    public List<VehicleData> getAvailableVehicles(ServerPlayer player, TeamManager teamManager) {
        List<VehicleData> result = new ArrayList<>();
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        for (VehicleDefinition def : getAvailableDefinitions(player, teamManager)) {
            VehicleData data = new VehicleData(def.getId(), def.getDisplayName(),
                playerTeam, def.getRequiredAccessLevel(), def.getTicketCost());
            data.setCooldownSeconds(def.getCooldownSeconds());
            data.setDeployBC(def.getCosts().getDeployBC());
            data.setDeployVC(def.getCosts().getDeployVC());
            int totalPlayers = teamManager.getPlayersByTeam(Team.NATO).size() + teamManager.getPlayersByTeam(Team.RUSSIA).size();
            data.setMaxActive(def.getPopulationLimit(Math.max(totalPlayers, 1)));
            data.setEntityData(def.getEntityType(), def.resolveNbt());
            result.add(data);
        }
        return result;
    }

    // ===== Cooldown Persistence =====

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
            PWP.LOGGER.warn("Failed to load vehicle cooldowns: {}", e.getMessage());
        }
    }

    private void saveCooldowns() {
        try {
            Path configPath = Paths.get("config/pwp");
            Files.createDirectories(configPath);
            JsonObject obj = new JsonObject();
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
                if (e.getValue() > now) obj.addProperty(e.getKey(), e.getValue());
            }
            Files.writeString(Paths.get(COOLDOWN_FILE), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to save vehicle cooldowns: {}", e.getMessage());
        }
    }
}
