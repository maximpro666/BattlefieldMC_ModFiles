package com.yourmod.teamsystem.integration;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.BattlefieldRuntime;
import com.yourmod.teamsystem.core.ContributionManager;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReviveMeIntegration {
    private static ReviveMeIntegration INSTANCE;
    private final Map<UUID, Boolean> previousFallenState = new ConcurrentHashMap<>();
    private boolean modChecked = false;
    private boolean modPresent = false;

    public static ReviveMeIntegration getInstance() {
        if (INSTANCE == null) INSTANCE = new ReviveMeIntegration();
        return INSTANCE;
    }

    public void tick(MinecraftServer server) {
        if (server == null) return;
        if (!isModPresent()) {
            previousFallenState.clear();
            return;
        }

        TeamManager tm = TeamSystem.getTeamManager();
        ContributionManager contrib = TeamSystem.getContributionManager();
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        if (tm == null || contrib == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            boolean isFallen = isPlayerFallen(player);
            Boolean wasFallen = previousFallenState.getOrDefault(uuid, false);

            if (wasFallen && !isFallen) {
                String name = player.getName().getString();
                contrib.addRevive(uuid, name);
                runtime.addActivity(uuid, BattlefieldRuntime.SCORE_REVIVE);
                runtime.syncAll(player);
            }

            previousFallenState.put(uuid, isFallen);
        }

        for (UUID uuid : previousFallenState.keySet()) {
            if (server.getPlayerList().getPlayer(uuid) == null) {
                previousFallenState.remove(uuid);
            }
        }
    }

    private static boolean isPlayerFallen(ServerPlayer player) {
        try {
            Class<?> capClass = Class.forName("invoker54.reviveme.common.capability.FallenCapability");
            Object cap = capClass.getMethod("get", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            return (boolean) capClass.getMethod("isFallen").invoke(cap);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isModPresent() {
        try {
            Class.forName("invoker54.reviveme.ReviveMe");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isPlayerDowned(ServerPlayer player) {
        if (!isModPresent()) return false;
        return isPlayerFallen(player);
    }
}
