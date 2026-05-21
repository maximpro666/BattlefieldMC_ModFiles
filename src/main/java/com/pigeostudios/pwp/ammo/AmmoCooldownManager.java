package com.pigeostudios.pwp.ammo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AmmoCooldownManager {
    public static final long POINT_COOLDOWN_MS = 60_000;
    public static final long BASE_COOLDOWN_MS = 120_000;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public boolean canRequest(UUID playerUuid, String ammoType, boolean isBase) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null) return true;
        Long lastTime = playerCooldowns.get(ammoType);
        if (lastTime == null) return true;
        long cooldown = isBase ? BASE_COOLDOWN_MS : POINT_COOLDOWN_MS;
        return System.currentTimeMillis() - lastTime >= cooldown;
    }

    public long getRemainingCooldown(UUID playerUuid, String ammoType, boolean isBase) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerUuid);
        if (playerCooldowns == null) return 0;
        Long lastTime = playerCooldowns.get(ammoType);
        if (lastTime == null) return 0;
        long cooldown = isBase ? BASE_COOLDOWN_MS : POINT_COOLDOWN_MS;
        long remaining = cooldown - (System.currentTimeMillis() - lastTime);
        return Math.max(0, remaining);
    }

    public void setCooldown(UUID playerUuid, String ammoType) {
        cooldowns.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
            .put(ammoType, System.currentTimeMillis());
    }

    public void resetPlayer(UUID playerUuid) {
        cooldowns.remove(playerUuid);
    }

    public void resetAll() {
        cooldowns.clear();
    }
}
