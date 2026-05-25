package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SquadmateRespawnCooldownManager {

    private static final Map<UUID, Long> playerLastDamage = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> fobLastDamage   = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 400L;
    private static final int FOB_RADIUS = 15;

    public static void onPlayerAttacked(ServerPlayer victim) {
        long gameTime = victim.serverLevel().getGameTime();
        playerLastDamage.put(victim.getUUID(), gameTime);
        checkFOBsNearby(victim, gameTime);
    }

    public static boolean canSpawnOnPlayer(ServerLevel level, UUID playerUUID) {
        Long last = playerLastDamage.get(playerUUID);
        if (last == null) return true;
        return level.getGameTime() - last > COOLDOWN_TICKS;
    }

    public static boolean canSpawnOnFOB(ServerLevel level, int fobId) {
        Long last = fobLastDamage.get(fobId);
        if (last == null) return true;
        return level.getGameTime() - last > COOLDOWN_TICKS;
    }

    public static int getSquadmateCooldownTicks(ServerLevel level, UUID playerUUID) {
        Long last = playerLastDamage.get(playerUUID);
        if (last == null) return 0;
        long elapsed = level.getGameTime() - last;
        return (int) Math.max(0L, COOLDOWN_TICKS - elapsed);
    }

    public static int getFobCooldownTicks(ServerLevel level, int fobId) {
        Long last = fobLastDamage.get(fobId);
        if (last == null) return 0;
        long elapsed = level.getGameTime() - last;
        return (int) Math.max(0L, COOLDOWN_TICKS - elapsed);
    }

    public static void reset() {
        playerLastDamage.clear();
        fobLastDamage.clear();
    }

    private static void checkFOBsNearby(ServerPlayer victim, long gameTime) {
        FOBManager fobManager = PWP.getFOBManager();
        if (fobManager == null) return;

        TeamManager teamManager = PWP.getTeamManager();
        if (teamManager == null) return;

        int victimTeam = teamManager.getOrCreatePlayerData(victim.getUUID())
                                    .getTeam()
                                    .ordinal();

        List<FOBManager.SavedFOB> fobs = fobManager.getFOBs();
        if (fobs == null) return;

        for (FOBManager.SavedFOB fob : fobs) {
            if (fob.teamOrdinal == victimTeam && isWithinRadius(victim, fob, FOB_RADIUS)) {
                fobLastDamage.put(fob.fobId, gameTime);
            }
        }
    }

    private static boolean isWithinRadius(ServerPlayer player, FOBManager.SavedFOB fob, int radius) {
        double dx = player.getX() - fob.x;
        double dy = player.getY() - fob.y;
        double dz = player.getZ() - fob.z;
        return (dx * dx + dy * dy + dz * dz) < (double) (radius * radius);
    }
}
