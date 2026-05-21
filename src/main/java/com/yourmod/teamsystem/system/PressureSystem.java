package com.yourmod.teamsystem.system;

import com.yourmod.teamsystem.state.PressureState;
import com.yourmod.teamsystem.state.VehicleState;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.vehicle.VehicleDefinition;
import com.yourmod.teamsystem.vehicle.VehicleDefinitionRegistry;

import java.util.UUID;

public class PressureSystem {
    private static final double PRESSURE_DECAY = 0.95;
    private static final int PRESSURE_DECAY_INTERVAL = 200;

    private int pressureTickCounter = 0;

    public void tick(PressureState state) {
        pressureTickCounter++;
        if (pressureTickCounter < PRESSURE_DECAY_INTERVAL) return;
        pressureTickCounter = 0;
        state.decay(PRESSURE_DECAY);
    }

    public void onVehicleSpawn(PressureState state, VehicleState vState,
                               UUID vehicleUUID, Team team, VehicleDefinition def) {
        vState.trackSpawn(vehicleUUID, team, def.getId());
        state.add(team, def.getPressure().getGround(), def.getPressure().getAir(), def.getPressure().getSiege());
    }

    public void onVehicleDestroy(PressureState state, VehicleState vState,
                                 UUID vehicleUUID, VehicleDefinitionRegistry registry) {
        Team team = vState.getVehicleTeam(vehicleUUID);
        String defId = vState.getVehicleDefId(vehicleUUID);
        if (team == null || defId == null) return;

        VehicleDefinition def = registry.get(defId);
        if (def != null) {
            state.remove(team, def.getPressure().getGround(), def.getPressure().getAir(), def.getPressure().getSiege());
        }
        vState.trackDestroy(vehicleUUID);
    }

    public void reset() {
        pressureTickCounter = 0;
    }
}
