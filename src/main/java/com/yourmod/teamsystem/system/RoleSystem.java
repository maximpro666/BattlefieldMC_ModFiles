package com.yourmod.teamsystem.system;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.PlayerCombatData;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.role.LoadoutDefinition;
import com.yourmod.teamsystem.role.RoleDefinition;
import com.yourmod.teamsystem.role.RoleRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class RoleSystem {
    private final RoleRegistry registry = new RoleRegistry();

    public void reload(KitConfig config) {
        registry.loadFromKitConfig(config);
    }

    public RoleRegistry getRegistry() { return registry; }

    public RoleDefinition getRole(String roleId) {
        return registry.getRole(roleId);
    }

    public LoadoutDefinition getLoadout(String loadoutId) {
        return registry.getLoadout(loadoutId);
    }

    public Collection<RoleDefinition> getAllRoles() {
        return registry.getAllRoles();
    }

    public String selectLoadout(ServerPlayer player, String loadoutId, TeamManager teamManager) {
        LoadoutDefinition loadout = registry.getLoadout(loadoutId);
        if (loadout == null) return "§cЛоадут не найден: " + loadoutId;

        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());
        Team playerTeam = data.getTeam();
        if (!playerTeam.isPlayable()) return "§cВы не в команде";

        int playerRank = data.getRankOrdinal();
        KitConfig.KitRequirements req = loadout.getRequirements();
        int requiredRank = req != null ? req.rank : 1;
        if (playerRank < requiredRank) return "§cТребуется ранг " + requiredRank;

        if (req != null && req.team != null) {
            Team requiredTeam = Team.fromString(req.team);
            if (requiredTeam != null && requiredTeam.isPlayable() && playerTeam != requiredTeam) {
                return "§cЛоадут недоступен для вашей команды";
            }
        }

        if (req != null && req.bc_cost > 0) {
            var runtime = com.yourmod.teamsystem.core.BattlefieldRuntime.getInstance();
            if (!runtime.deductBC(player.getUUID(), req.bc_cost)) {
                return "§cНе хватает BC (нужно " + req.bc_cost + ")";
            }
        }

        data.setSelectedLoadout(loadoutId);
        data.setSelectedRole(loadout.getRoleId());
        teamManager.setDirty();

        return null;
    }

    public List<LoadoutDefinition> getAvailableLoadouts(ServerPlayer player, TeamManager teamManager) {
        List<LoadoutDefinition> result = new ArrayList<>();
        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());
        Team playerTeam = data.getTeam();
        int playerRank = data.getRankOrdinal();

        for (LoadoutDefinition ld : registry.getAllLoadouts()) {
            KitConfig.KitRequirements req = ld.getRequirements();
            if (req == null) { result.add(ld); continue; }

            if (req.team != null) {
                Team requiredTeam = Team.fromString(req.team);
                if (requiredTeam != null && requiredTeam.isPlayable() && playerTeam != requiredTeam) continue;
            }

            if (playerRank < req.rank) continue;

            result.add(ld);
        }
        return result;
    }
}
