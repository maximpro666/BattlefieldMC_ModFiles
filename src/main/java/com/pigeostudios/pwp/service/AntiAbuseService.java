package com.pigeostudios.pwp.service;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiAbuseService {
    private static final long KIT_SWITCH_COOLDOWN_MS = 2000;
    private static final long FOB_PLACE_COOLDOWN_MS = 90_000;
    private static final long VEHICLE_BUY_COOLDOWN_MS = 3000;
    private static final long COMMAND_GENERIC_COOLDOWN_MS = 500;
    private static final long KILL_FARM_COOLDOWN_MS = 30_000;
    private static final long CLEANUP_INTERVAL_MS = 300_000;

    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();
    private long lastCleanupTime;

    public AntiAbuseService() {
        cooldowns.put("kit_switch", new ConcurrentHashMap<>());
        cooldowns.put("fob_place", new ConcurrentHashMap<>());
        cooldowns.put("vehicle_buy", new ConcurrentHashMap<>());
        cooldowns.put("command", new ConcurrentHashMap<>());
        cooldowns.put("kill_farm", new ConcurrentHashMap<>());
    }

    public boolean checkKitSwitch(ServerPlayer player) {
        return checkCooldown("kit_switch", player, KIT_SWITCH_COOLDOWN_MS);
    }

    public boolean checkFOBPlace(ServerPlayer player) {
        return checkCooldown("fob_place", player, FOB_PLACE_COOLDOWN_MS);
    }

    public boolean checkVehicleBuy(ServerPlayer player) {
        return checkCooldown("vehicle_buy", player, VEHICLE_BUY_COOLDOWN_MS);
    }

    public boolean checkCommand(ServerPlayer player) {
        return checkCooldown("command", player, COMMAND_GENERIC_COOLDOWN_MS);
    }

    public boolean checkKillFarm(ServerPlayer killer, UUID victimUuid) {
        if (killer.getUUID().equals(victimUuid)) return false;
        Map<UUID, Long> map = cooldowns.get("kill_farm");
        if (map == null) return true;
        UUID compound = UUID.nameUUIDFromBytes((killer.getUUID().toString() + "|" + victimUuid.toString()).getBytes());
        long now = System.currentTimeMillis();
        Long lastTime = map.get(compound);
        if (lastTime != null && (now - lastTime) < KILL_FARM_COOLDOWN_MS) {
            return false;
        }
        map.put(compound, now);
        return true;
    }

    private boolean checkCooldown(String key, ServerPlayer player, long cooldownMs) {
        Map<UUID, Long> map = cooldowns.get(key);
        if (map == null) return true;

        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        Long lastTime = map.get(uuid);

        if (lastTime != null && (now - lastTime) < cooldownMs) {
            return false;
        }

        map.put(uuid, now);
        periodicCleanup(now);
        return true;
    }

    private void periodicCleanup(long now) {
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) return;
        lastCleanupTime = now;
        long expiryThreshold = now - KILL_FARM_COOLDOWN_MS * 2;
        for (Map<UUID, Long> map : cooldowns.values()) {
            map.values().removeIf(t -> t < expiryThreshold);
        }
    }

    public void clearPlayer(UUID uuid) {
        for (Map<UUID, Long> map : cooldowns.values()) {
            map.remove(uuid);
        }
    }

    public void clearAll() {
        for (Map<UUID, Long> map : cooldowns.values()) {
            map.clear();
        }
    }
}
