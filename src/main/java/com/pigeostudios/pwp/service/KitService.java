package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.core.PlayerCombatData;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.data.KitConfigServerHelper;
import com.pigeostudios.pwp.role.LoadoutDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KitService {
    private final EconomyService economy;

    public KitService(EconomyService economy) {
        this.economy = economy;
    }

    // H1 fix: deduct BC after applyKit (items resolved first)
    public Component applyKit(ServerPlayer player, String classId, String kitId) {
        TeamManager teamManager = PWP.getTeamManager();
        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return Component.translatable("pwp.chat.kit_config.not_loaded");

        KitConfig.ClassConfig clazz = cfg.classes.get(classId);
        if (clazz == null) return Component.translatable("pwp.chat.kit_config.class_not_found", classId);

        KitConfig.KitDef kit = clazz.kits.get(kitId);
        if (kit == null) return Component.translatable("pwp.chat.kit_config.kit_not_found", kitId);

        if (!player.isAlive()) {
            return Component.translatable("pwp.chat.kit_config.dead_respawn");
        }

        // Check requirements (rank, team) but DEFER BC deduct
        if (kit.requirements != null) {
            if (data.getRankOrdinal() < kit.requirements.rank)
                return Component.translatable("pwp.chat.kit_config.rank_too_low", kit.requirements.rank);
            if (kit.requirements.team != null && !kit.requirements.team.equalsIgnoreCase(data.getTeam().name()))
                return Component.translatable("pwp.chat.kit_config.wrong_team");
        }

        // H1: check BC balance but don't deduct yet
        int bcCost = kit.requirements != null ? kit.requirements.bc_cost : 0;
        if (bcCost > 0 && economy.getBC(player.getUUID()) < bcCost) {
            return Component.translatable("pwp.chat.kit_config.insufficient_bc", bcCost);
        }

        // Let the helper apply the kit (items resolution happens here)
        Component result = KitConfigServerHelper.applyKit(player, classId, kitId);

        // H1: deduct AFTER items are resolved and applied
        if (result == null && bcCost > 0) {
            if (!economy.deductBC(player.getUUID(), bcCost)) {
                return Component.translatable("pwp.chat.kit_config.insufficient_bc", bcCost);
            }
            economy.syncBC(player);
        }

        return result;
    }

    // L6 fix: deduct BC after role apply (not before)
    public Component selectLoadout(ServerPlayer player, String loadoutId, TeamManager teamManager) {
        var runtime = BattlefieldRuntime.getInstance();

        // Delegate to existing RoleSystem for role resolution
        var roleSystem = PWP.getRoleSystem();
        if (roleSystem == null) return Component.translatable("pwp.chat.loadout.not_found", loadoutId);

        LoadoutDefinition loadout = roleSystem.getLoadout(loadoutId);
        if (loadout == null) return Component.translatable("pwp.chat.loadout.not_found", loadoutId);

        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());
        Team playerTeam = data.getTeam();
        if (!playerTeam.isPlayable()) return Component.translatable("pwp.chat.loadout.not_on_team");

        int playerRank = data.getRankOrdinal();
        var req = loadout.getRequirements();
        int requiredRank = req != null ? req.rank : 1;
        if (playerRank < requiredRank)
            return Component.translatable("pwp.chat.loadout.requires_rank", requiredRank);

        if (req != null && req.team != null) {
            Team requiredTeam = Team.fromString(req.team);
            if (requiredTeam != null && requiredTeam.isPlayable() && playerTeam != requiredTeam)
                return Component.translatable("pwp.chat.loadout.not_for_team");
        }

        // Check BC balance but don't deduct yet
        int bcCost = req != null ? req.bc_cost : 0;
        if (bcCost > 0 && economy.getBC(player.getUUID()) < bcCost) {
            return Component.translatable("pwp.chat.loadout.insufficient_bc", bcCost);
        }

        // Apply role first
        data.setSelectedLoadout(loadoutId);
        data.setSelectedRole(loadout.getRoleId());
        teamManager.setDirty();

        // L6: deduct AFTER role apply
        if (bcCost > 0) {
            if (!economy.deductBC(player.getUUID(), bcCost)) {
                return Component.translatable("pwp.chat.loadout.insufficient_bc", bcCost);
            }
            economy.syncBC(player);
        }

        return null;
    }
}
