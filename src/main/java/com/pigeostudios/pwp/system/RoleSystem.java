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
        LoadoutDefinition loadout = registry.getLoadout(loadoutId);
        if (loadout == null) return Component.translatable("pwp.chat.loadout.not_found", loadoutId);

        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());
        Team playerTeam = data.getTeam();
        if (!playerTeam.isPlayable()) return Component.translatable("pwp.chat.loadout.not_on_team");

        int playerRank = data.getRankOrdinal();
        KitConfig.KitRequirements req = loadout.getRequirements();
        int requiredRank = req != null ? req.rank : 1;
        if (playerRank < requiredRank) return Component.translatable("pwp.chat.loadout.requires_rank", requiredRank);

        if (req != null && req.team != null) {
            Team requiredTeam = Team.fromString(req.team);
            if (requiredTeam != null && requiredTeam.isPlayable() && playerTeam != requiredTeam) {
                return Component.translatable("pwp.chat.loadout.not_for_team");
            }
        }

        if (req != null && req.bc_cost > 0) {
            var runtime = com.pigeostudios.pwp.core.BattlefieldRuntime.getInstance();
            if (!runtime.deductBC(player.getUUID(), req.bc_cost)) {
                return Component.translatable("pwp.chat.loadout.insufficient_bc", req.bc_cost);
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
