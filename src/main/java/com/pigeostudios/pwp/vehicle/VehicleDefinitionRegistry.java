package com.pigeostudios.pwp.vehicle;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.config.ConfigManager;

import java.util.*;

public class VehicleDefinitionRegistry {
    private final Map<String, VehicleDefinition> definitions = new HashMap<>();

    public void loadAll() {
        definitions.clear();
        List<VehicleDefinition> loaded = ConfigManager.loadAll("vehicle_definitions", VehicleDefinition.class);
        for (VehicleDefinition def : loaded) {
            if (def.getId() != null) {
                definitions.put(def.getId(), def);
            } else {
                PWP.LOGGER.warn("Skipping vehicle definition without id");
            }
        }
        PWP.LOGGER.info("VehicleDefinitionRegistry: loaded {} definitions", definitions.size());
    }

    public VehicleDefinition get(String id) {
        return definitions.get(id);
    }

    public Collection<VehicleDefinition> getAll() {
        return definitions.values();
    }

    public boolean has(String id) {
        return definitions.containsKey(id);
    }

    public int size() {
        return definitions.size();
    }
}
