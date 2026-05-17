package com.yourmod.teamsystem.core;

import com.google.gson.*;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VehicleManager {
    private static final String CONFIG_DIR = "config/teamsystem";
    private static final String VEHICLES_FILE = CONFIG_DIR + "/vehicles.json";

    private Map<String, VehicleData> vehicles = new HashMap<>();

    public VehicleManager() {
        loadVehicles();
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
            for (VehicleData vehicle : vehicles.values()) {
                arr.add(vehicle.toJson());
            }

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
        if (vehicles.remove(vehicleId) != null) {
            saveVehicles();
        }
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

    public boolean buyVehicle(ServerPlayer player, String vehicleId, TeamManager teamManager) {
        VehicleData vehicle = getVehicle(vehicleId);
        if (vehicle == null) return false;

        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();

        if (vehicle.getTeam() != Team.SPECTATOR && vehicle.getTeam() != playerTeam) {
            return false;
        }

        if (playerRank < vehicle.getMinRankOrdinal()) {
            return false;
        }

        int teamTickets = teamManager.getTickets(playerTeam);
        if (teamTickets < vehicle.getTicketCost()) {
            return false;
        }

        teamManager.setTickets(playerTeam, teamTickets - vehicle.getTicketCost());

        HitResult hit = player.pick(500, 0, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos spawnPos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().above();
        }

        return true;
    }

    public Map<String, VehicleData> getVehicles() {
        return vehicles;
    }
}
