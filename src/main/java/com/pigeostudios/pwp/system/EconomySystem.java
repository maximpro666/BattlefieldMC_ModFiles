package com.pigeostudios.pwp.system;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import com.pigeostudios.pwp.state.EconomyState;
import com.pigeostudios.pwp.state.FrontlineState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class EconomySystem {
    private static final int INCOME_INTERVAL = 80;
    private static final int FRONTLINE_RADIUS = 30;
    private static final int HOLD_RATE = 1;
    private static final int DEFENSE_RATE = 2;

    private static final int VC_INTERVAL = 1200;

    private int incomeTickCounter = 0;
    private int vehicleCrewTickCounter = 0;
    private int vcTickCounter = 0;

    public void tick(ServerLevel level, EconomyState eState, FrontlineState fState,
                     GameManager gm, TeamManager tm) {
        tickFrontlineIncome(level, eState, fState, tm);
        vehicleCrewTickCounter++;
        if (vehicleCrewTickCounter >= INCOME_INTERVAL) {
            vehicleCrewTickCounter = 0;
            tickVehicleCrewIncome(level, eState, tm);
        }
        tickVehicleCredits(eState, gm);
    }

    private void tickFrontlineIncome(ServerLevel level, EconomyState eState,
                                     FrontlineState fState, TeamManager tm) {
        incomeTickCounter++;
        if (incomeTickCounter < INCOME_INTERVAL) return;
        incomeTickCounter = 0;

        if (tm == null) return;

        var cpManager = PWP.getCapturePointManager();
        if (cpManager == null) return;
        var zones = cpManager.getZoneData();
        if (zones == null) return;
        var allZones = zones.getAllZones();
        if (allZones == null || allZones.isEmpty()) return;

        for (ServerPlayer player : level.players()) {
            UUID uuid = player.getUUID();
            Team playerTeam = tm.getOrCreatePlayerData(uuid).getTeam();
            if (!playerTeam.isPlayable()) continue;
            if (!fState.isActive(uuid)) continue;

            for (var zone : allZones) {
                double dist = player.blockPosition().distManhattan(zone.getCenter());
                if (dist > FRONTLINE_RADIUS) continue;

                if (zone.getOwnerTeam() == playerTeam && zone.isCaptured()) {
                    eState.addBC(uuid, HOLD_RATE);
                }
                if (zone.getOwnerTeam() == playerTeam
                    && zone.getCapturingTeam() == Team.SPECTATOR
                    && zone.getProgress() < 1.0f) {
                    eState.addBC(uuid, DEFENSE_RATE);
                }
            }
        }
    }

    private void tickVehicleCrewIncome(ServerLevel level, EconomyState eState, TeamManager tm) {
        if (tm == null) return;
        var cpManager = PWP.getCapturePointManager();
        if (cpManager == null) return;
        var zones = cpManager.getZoneData();
        if (zones == null) return;
        var allZones = zones.getAllZones();
        if (allZones == null || allZones.isEmpty()) return;

        var vehicleManager = PWP.getVehicleManager();
        if (vehicleManager == null) return;

        for (UUID vehicleId : vehicleManager.getSpawnedVehicleIds()) {
            Entity vehicle = level.getEntity(vehicleId);
            if (vehicle == null || !vehicle.isAlive()) continue;
            UUID ownerId = vehicleManager.getOwnerForVehicle(vehicleId);
            if (ownerId == null) continue;
            Team crewTeam = tm.getOrCreatePlayerData(ownerId).getTeam();
            if (!crewTeam.isPlayable()) continue;

            for (var zone : allZones) {
                double dist = vehicle.blockPosition().distManhattan(zone.getCenter());
                if (dist > FRONTLINE_RADIUS) continue;
                if (zone.getOwnerTeam() != crewTeam) continue;

                for (Entity passenger : vehicle.getPassengers()) {
                    if (passenger instanceof ServerPlayer crewMember) {
                        UUID crewUUID = crewMember.getUUID();
                        eState.addBC(crewUUID, HOLD_RATE);
                    }
                }
                break;
            }
        }
    }

    private void tickVehicleCredits(EconomyState eState, GameManager gm) {
        vcTickCounter++;
        if (vcTickCounter < VC_INTERVAL) return;
        vcTickCounter = 0;

        var cpManager = PWP.getCapturePointManager();
        if (cpManager == null) return;
        var zones = cpManager.getZoneData();
        if (zones == null) return;
        var allZones = zones.getAllZones();
        if (allZones == null) return;

        for (var zone : allZones) {
            if (zone.getOwnerTeam() == Team.NATO) {
                eState.addVC(Team.NATO, zone.getVcRate());
            } else if (zone.getOwnerTeam() == Team.RUSSIA) {
                eState.addVC(Team.RUSSIA, zone.getVcRate());
            }
        }
    }

    public void reset() {
        incomeTickCounter = 0;
        vehicleCrewTickCounter = 0;
        vcTickCounter = 0;
    }
}
