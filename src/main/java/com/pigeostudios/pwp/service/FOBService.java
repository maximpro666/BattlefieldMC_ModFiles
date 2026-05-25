package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class FOBService {
    private final EconomyService economy;
    private final ConfigService config;

    private final Map<Team, Long> lastFOBTime = new HashMap<>();
    private static final long FOB_COOLDOWN_MS = 90_000;

    public FOBService(EconomyService economy, ConfigService config) {
        this.economy = economy;
        this.config = config;
    }

    public synchronized String placeFOB(ServerPlayer player, String name) {
        UUID uuid = player.getUUID();
        Team team = PWP.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
        if (!team.isPlayable()) return "\u00a7cYou must be on a team to place FOBs";

        var aa = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getAntiAbuse() : null;
        if (aa != null && !aa.checkFOBPlace(player)) return "\u00a7cFOB cooldown active, please wait";

        Squad squad = PWP.getSquadManager().getPlayerSquad(uuid);
        if (squad == null || !squad.isLeader(uuid)) return "\u00a7cOnly squad leaders can place FOBs";

        var fobManager = PWP.getFOBManager();
        if (fobManager == null) return "\u00a7cFOB system unavailable";

        int maxFOBs = getMaxFOBs();
        long teamCount = fobManager.getFOBsForTeam(team).size();
        if (teamCount >= maxFOBs) return "\u00a7cTeam FOB limit reached (" + maxFOBs + ")";

        // M6: team-wide cooldown
        Long lastTime = lastFOBTime.get(team);
        if (lastTime != null && System.currentTimeMillis() - lastTime < FOB_COOLDOWN_MS) {
            int remain = (int) ((FOB_COOLDOWN_MS - (System.currentTimeMillis() - lastTime)) / 1000);
            return "\u00a7cTeam must wait " + remain + "s before placing another FOB";
        }

        // M3/L5: validate + deduct atomically
        String validation = fobManager.validatePosition(
            player.serverLevel(), player.blockPosition(), team.ordinal(), uuid.toString());
        if (validation != null) return validation;

        int fobCost = getFOBCost();
        if (!economy.deductBC(uuid, fobCost)) {
            return "\u00a7cNot enough BC! FOB costs " + fobCost + " BC";
        }

        // Place without duplicate checks
        String result = fobManager.placeFOBWithoutChecks(player, name, team, uuid.toString());

        lastFOBTime.put(team, System.currentTimeMillis());
        economy.syncAll(player);
        return result;
    }

    private int getFOBCost() {
        GameManager gm = PWP.getGameManager();
        if (gm == null) return config.getEconomy().fob.costNormal;
        double progress = 1.0 - (gm.getMatchTimeRemaining() / 1800.0);
        var dm = config.getEconomy().difficultyMultipliers;
        double multiplier = progress >= 0.75 ? dm.hardVehiclePrice : progress >= 0.50 ? dm.normalVehiclePrice : dm.easyVehiclePrice;
        return (int) Math.round(config.getEconomy().fob.costNormal * multiplier);
    }

    private int getMaxFOBs() {
        return config.getEconomy().fob.maxPerTeam;
    }

    // For backward compat — call this when FOBManager.placeFOB would have been called
    public static boolean isAvailable() {
        return PWP.getServiceRegistry() != null && PWP.getServiceRegistry().getEconomy() != null;
    }
}
