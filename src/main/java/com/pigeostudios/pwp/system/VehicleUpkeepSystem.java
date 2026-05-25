package com.pigeostudios.pwp.system;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.VehicleManager;
import com.pigeostudios.pwp.service.EconomyService;

import com.pigeostudios.pwp.state.VehicleState;
import com.pigeostudios.pwp.vehicle.VehicleDefinition;
import com.pigeostudios.pwp.vehicle.VehicleDefinitionRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;

public class VehicleUpkeepSystem {
    private static final int GLOBAL_INTERVAL = 20;

    private int globalTicker = 0;
    private final Map<UUID, Integer> vehicleTickers = new HashMap<>();
    private final Map<UUID, Integer> missedPayments = new HashMap<>();
    // H7: track debt for offline players — deducted on reconnect
    private final Map<UUID, Integer> upkeepDebt = new HashMap<>();

    public void tick(ServerLevel level, BattlefieldRuntime runtime, VehicleManager vehicleManager) {
        if (vehicleManager == null) return;
        GameManager gm = PWP.getGameManager();
        if (gm == null || gm.getServer() == null) return;

        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        VehicleDefinitionRegistry registry = runtime.getVehicleDefRegistry();
        VehicleState vState = runtime.vehicles;

        globalTicker++;
        if (globalTicker < GLOBAL_INTERVAL) return;
        globalTicker = 0;

        List<UUID> toDespawn = new ArrayList<>();

        for (UUID vehicleId : vehicleManager.getSpawnedVehicleIds()) {
            UUID ownerId = vehicleManager.getOwnerForVehicle(vehicleId);
            if (ownerId == null) continue;

            if (level.getEntity(vehicleId) == null) {
                vehicleManager.unregisterSpawnedVehicle(vehicleId);
                vehicleTickers.remove(vehicleId);
                continue;
            }

            String defId = vState.getVehicleDefId(vehicleId);
            if (defId == null) continue;

            VehicleDefinition def = registry.get(defId);
            if (def == null || !def.getUpkeep().isEnabled()) continue;

            int intervalTicks = def.getUpkeep().getIntervalSeconds() * 20;
            if (intervalTicks <= 0) continue;

            int elapsed = vehicleTickers.merge(vehicleId, GLOBAL_INTERVAL, Integer::sum);
            if (elapsed < intervalTicks) continue;
            vehicleTickers.put(vehicleId, 0);

            int bcCost = def.getUpkeep().getBcCost();
            ServerPlayer owner = gm.getServer().getPlayerList().getPlayer(ownerId);

            boolean deducted = false;
            if (eco != null) {
                deducted = eco.deductBC(ownerId, bcCost);
            } else {
                deducted = runtime.deductBC(ownerId, bcCost);
            }
            if (deducted) {
                missedPayments.remove(vehicleId);
                if (owner != null) {
                    owner.sendSystemMessage(Component.translatable("pwp.chat.upkeep.charged", bcCost, def.getDisplayName()));
                }
            } else {
                upkeepDebt.merge(ownerId, bcCost, (a, b) -> Math.min(a + b, 100000));
                int misses = missedPayments.merge(vehicleId, 1, Integer::sum);
                if (owner != null) {
                    owner.sendSystemMessage(Component.translatable("pwp.chat.upkeep.insufficient", def.getDisplayName(), misses));
                }
                if (misses >= 3) {
                    toDespawn.add(vehicleId);
                    if (owner != null) {
                        owner.sendSystemMessage(Component.translatable("pwp.chat.upkeep.despawned", def.getDisplayName()));
                    }
                }
            }
        }

        for (UUID id : toDespawn) {
            UUID ownerId = vehicleManager.getOwnerForVehicle(id);
            Entity vehicle = level.getEntity(id);
            if (vehicle != null) vehicle.discard();
            vehicleManager.unregisterSpawnedVehicle(id);
            runtime.trackVehicleDestroy(id);
            missedPayments.remove(id);
            vehicleTickers.remove(id);
            // Anti-abuse #3: collect upkeep debt on despawn
            if (ownerId != null && eco != null) {
                Integer debt = upkeepDebt.get(ownerId);
                if (debt != null && debt > 0) {
                    ServerPlayer owner = gm.getServer().getPlayerList().getPlayer(ownerId);
                    if (owner != null && eco.deductBC(ownerId, debt)) {
                        upkeepDebt.remove(ownerId);
                    }
                    // if offline or can't pay, debt stays for reconnect collection
                }
            }
        }
    }

    // H7: collect debt on reconnect
    public int getDebt(UUID playerUuid) {
        return upkeepDebt.getOrDefault(playerUuid, 0);
    }

    public void clearDebt(UUID playerUuid) {
        upkeepDebt.remove(playerUuid);
    }

    public void reset() {
        globalTicker = 0;
        vehicleTickers.clear();
        missedPayments.clear();
        upkeepDebt.clear();
    }
}
