package com.yourmod.teamsystem.vehicle.adapter;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class VehicleAdapterRegistry {
    private final List<IVehicleAdapter> adapters = new ArrayList<>();
    private static final VehicleAdapterRegistry INSTANCE = new VehicleAdapterRegistry();

    private VehicleAdapterRegistry() {}

    public static VehicleAdapterRegistry getInstance() { return INSTANCE; }

    public void register(IVehicleAdapter adapter) {
        adapters.add(adapter);
        TeamSystem.LOGGER.info("Registered vehicle adapter: {}", adapter.getClass().getSimpleName());
    }

    public IVehicleAdapter findAdapter(Entity entity) {
        if (entity == null) return null;
        for (IVehicleAdapter adapter : adapters) {
            if (adapter.isApplicable(entity)) return adapter;
        }
        return null;
    }

    public boolean hasAdapter(Entity entity) {
        return findAdapter(entity) != null;
    }

    public int getAdapterCount() {
        return adapters.size();
    }
}
