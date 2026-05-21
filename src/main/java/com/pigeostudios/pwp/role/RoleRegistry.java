package com.pigeostudios.pwp.role;

import com.pigeostudios.pwp.data.KitConfig;

import java.util.*;

public class RoleRegistry {
    private final Map<String, RoleDefinition> roles = new HashMap<>();
    private final Map<String, LoadoutDefinition> loadouts = new HashMap<>();

    public void loadFromKitConfig(KitConfig config) {
        roles.clear();
        loadouts.clear();
        if (config == null || config.classes == null) return;

        for (Map.Entry<String, KitConfig.ClassConfig> entry : config.classes.entrySet()) {
            RoleDefinition role = new RoleDefinition(entry.getKey(), entry.getValue());
            roles.put(role.getId(), role);
            for (LoadoutDefinition ld : role.getLoadouts()) {
                loadouts.put(ld.getId(), ld);
            }
        }
    }

    public RoleDefinition getRole(String roleId) {
        return roles.get(roleId);
    }

    public LoadoutDefinition getLoadout(String loadoutId) {
        return loadouts.get(loadoutId);
    }

    public Collection<RoleDefinition> getAllRoles() {
        return roles.values();
    }

    public List<LoadoutDefinition> getLoadoutsForRole(String roleId) {
        RoleDefinition role = roles.get(roleId);
        return role != null ? role.getLoadouts() : Collections.emptyList();
    }

    public boolean hasRole(String roleId) {
        return roles.containsKey(roleId);
    }

    public boolean hasLoadout(String loadoutId) {
        return loadouts.containsKey(loadoutId);
    }

    public Collection<LoadoutDefinition> getAllLoadouts() {
        return loadouts.values();
    }

    public int getRoleCount() { return roles.size(); }
    public int getLoadoutCount() { return loadouts.size(); }
}
