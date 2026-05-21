package com.pigeostudios.pwp.system;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.VehicleManager;
import com.pigeostudios.pwp.state.EconomyState;
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

    public void tick(ServerLevel level, BattlefieldRuntime runtime, VehicleManager vehicleManager) {
        GameManager gm = PWP.getGameManager();
        if (gm == null || gm.getServer() == null) return;

        VehicleDefinitionRegistry registry = runtime.getVehicleDefRegistry();
        VehicleState vState = runtime.vehicles;
        EconomyState economy = runtime.economy;

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

            if (owner != null && economy.getBC(ownerId) >= bcCost) {
                economy.deductBC(ownerId, bcCost);
                missedPayments.remove(vehicleId);
                owner.sendSystemMessage(Component.translatable("pwp.chat.upkeep.charged", bcCost, def.getDisplayName()));
            } else {
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
            Entity vehicle = level.getEntity(id);
            if (vehicle != null) vehicle.discard();
            vehicleManager.unregisterSpawnedVehicle(id);
            runtime.trackVehicleDestroy(id);
            missedPayments.remove(id);
            vehicleTickers.remove(id);
        }
    }

    public void reset() {
        globalTicker = 0;
        vehicleTickers.clear();
        missedPayments.clear();
    }
}
