package com.yourmod.teamsystem.role;

import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.data.KitConfig.ClassConfig;

import java.util.*;

public class RoleDefinition {
    private final String id;
    private final String displayName;
    private final String icon;
    private final List<LoadoutDefinition> loadouts;

    public RoleDefinition(String id, ClassConfig classConfig) {
        this.id = id;
        this.displayName = classConfig.display_name != null ? classConfig.display_name : id;
        this.icon = classConfig.icon != null ? classConfig.icon : "";
        this.loadouts = new ArrayList<>();
        if (classConfig.kits != null) {
            for (Map.Entry<String, KitConfig.KitDef> entry : classConfig.kits.entrySet()) {
                loadouts.add(new LoadoutDefinition(entry.getKey(), id, entry.getValue()));
            }
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public List<LoadoutDefinition> getLoadouts() { return loadouts; }

    public LoadoutDefinition getLoadout(String loadoutId) {
        for (LoadoutDefinition ld : loadouts) {
            if (ld.getId().equals(loadoutId)) return ld;
        }
        return null;
    }
}
