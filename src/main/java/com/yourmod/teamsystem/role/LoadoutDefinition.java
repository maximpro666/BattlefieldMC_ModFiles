package com.yourmod.teamsystem.role;

import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.data.KitConfig.KitDef;

import java.util.HashMap;
import java.util.Map;

public class LoadoutDefinition {
    private static final Map<String, int[]> COST_OVERRIDES = buildCostOverrides();

    private static Map<String, int[]> buildCostOverrides() {
        Map<String, int[]> map = new HashMap<>();
        map.put("rifleman",       new int[]{  0,   0});
        map.put("assault",        new int[]{ 50, 500});
        map.put("medic",          new int[]{ 75, 500});
        map.put("support",        new int[]{100, 700});
        map.put("engineer",       new int[]{150,1200});
        map.put("scout_sniper",   new int[]{200,1400});
        map.put("heavy_at",       new int[]{400,3000});
        map.put("anti_air",       new int[]{500,3500});
        map.put("drone_operator", new int[]{450,4000});
        return map;
    }

    private final String id;
    private final String roleId;
    private final String displayName;
    private final String description;
    private final KitConfig.KitWeapons weapons;
    private final KitConfig.KitArmor armor;
    private final KitConfig.KitRequirements requirements;
    private final int deployCost;
    private final int unlockCost;

    public LoadoutDefinition(String id, String roleId, KitDef kitDef) {
        this.id = id;
        this.roleId = roleId;
        this.displayName = kitDef.display_name != null ? kitDef.display_name : id;
        this.description = kitDef.description != null ? kitDef.description : "";
        this.weapons = kitDef.weapons;
        this.armor = kitDef.armor;
        this.requirements = kitDef.requirements;
        int[] costs = COST_OVERRIDES.get(id);
        if (costs != null) {
            this.deployCost = costs[0];
            this.unlockCost = costs[1];
        } else {
            this.deployCost = kitDef.requirements.bc_cost;
            this.unlockCost = kitDef.requirements.sp_cost;
        }
    }

    public String getId() { return id; }
    public String getRoleId() { return roleId; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public KitConfig.KitWeapons getWeapons() { return weapons; }
    public KitConfig.KitArmor getArmor() { return armor; }
    public KitConfig.KitRequirements getRequirements() { return requirements; }
    public int getDeployCost() { return deployCost; }
    public int getUnlockCost() { return unlockCost; }
}
