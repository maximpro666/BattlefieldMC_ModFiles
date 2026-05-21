package com.pigeostudios.pwp.state;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FrontlineState {
    public static final int SCORE_MOVE = 1;
    public static final int SCORE_DAMAGE = 10;
    public static final int SCORE_HEAL = 4;
    public static final int SCORE_REVIVE = 15;
    public static final int SCORE_CAPTURE = 20;
    public static final int SCORE_SUPPLY = 5;
    public static final int SCORE_TAKE_DAMAGE = 5;
    public static final int ACTIVITY_THRESHOLD = 3;

    private final Map<UUID, Integer> activityScores = new HashMap<>();
    private int activeFrontlineCount = 0;

    public void addActivity(UUID uuid, int score) {
        int current = activityScores.getOrDefault(uuid, 0);
        activityScores.put(uuid, Math.min(100, current + score));
    }

    public boolean isActive(UUID uuid) {
        return activityScores.getOrDefault(uuid, 0) >= ACTIVITY_THRESHOLD;
    }

    public int getActivityScore(UUID uuid) {
        return activityScores.getOrDefault(uuid, 0);
    }

    public void decay(int amount) {
        activityScores.replaceAll((uuid, score) -> Math.max(0, score - amount));
    }

    public int getActiveCount() {
        return activeFrontlineCount;
    }

    public void recalcActive(MinecraftServer server) {
        if (server == null) {
            activeFrontlineCount = 0;
            return;
        }
        int count = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && isActive(p.getUUID())) {
                count++;
            }
        }
        activeFrontlineCount = count;
    }

    public void reset() {
        activityScores.clear();
        activeFrontlineCount = 0;
    }

    public void resetForPlayer(UUID uuid) {
        activityScores.remove(uuid);
    }
}
