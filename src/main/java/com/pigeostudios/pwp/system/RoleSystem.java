package com.pigeostudios.pwp.system;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.PlayerCombatData;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.role.LoadoutDefinition;
import com.pigeostudios.pwp.role.RoleDefinition;
import com.pigeostudios.pwp.role.RoleRegistry;
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

    public Component selectLoadout(ServerPlayer player, String loadoutId, TeamManager teamManager) {
        return PWP.getServiceRegistry().getKit().selectLoadout(player, loadoutId, teamManager);
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
