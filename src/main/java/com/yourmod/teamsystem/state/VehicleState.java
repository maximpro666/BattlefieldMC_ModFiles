package com.yourmod.teamsystem.state;

import com.yourmod.teamsystem.core.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VehicleState {
    private final Map<UUID, Team> vehicleToTeam = new HashMap<>();
    private final Map<UUID, String> vehicleToDefId = new HashMap<>();

    public void trackSpawn(UUID vehicleUUID, Team team, String defId) {
        vehicleToTeam.put(vehicleUUID, team);
        vehicleToDefId.put(vehicleUUID, defId);
    }

    public void trackDestroy(UUID vehicleUUID) {
        vehicleToTeam.remove(vehicleUUID);
        vehicleToDefId.remove(vehicleUUID);
    }

    public Team getVehicleTeam(UUID vehicleUUID) {
        return vehicleToTeam.get(vehicleUUID);
    }

    public String getVehicleDefId(UUID vehicleUUID) {
        return vehicleToDefId.get(vehicleUUID);
    }

    public boolean isTracked(UUID vehicleUUID) {
        return vehicleToTeam.containsKey(vehicleUUID);
    }

    public int getCountForTeam(Team team) {
        int count = 0;
        for (Team t : vehicleToTeam.values()) {
            if (t == team) count++;
        }
        return count;
    }

    public void reset() {
        vehicleToTeam.clear();
        vehicleToDefId.clear();
    }
}
