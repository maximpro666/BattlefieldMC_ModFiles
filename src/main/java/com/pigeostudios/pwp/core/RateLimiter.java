package com.pigeostudios.pwp.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    private RateLimiter() {}

    private static final int MAX_PACKETS_PER_SECOND = 40;
    private static final int MAX_BURST = 60;
    private static final long INTERVAL_NANOS = 1_000_000_000L;

    private static final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    private static final class Bucket {
        long lastRefillNanos = System.nanoTime();
        double tokens = MAX_BURST;
    }

    public static boolean checkAndThrottle(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Bucket bucket = buckets.computeIfAbsent(uuid, k -> new Bucket());

        long now = System.nanoTime();
        long elapsed = now - bucket.lastRefillNanos;
        bucket.lastRefillNanos = now;

        double refill = (double) elapsed / INTERVAL_NANOS * MAX_PACKETS_PER_SECOND;
        bucket.tokens = Math.min(MAX_BURST, bucket.tokens + refill);

        if (bucket.tokens < 1.0) {
            player.connection.disconnect(Component.literal("Disconnected: too many requests"));
            return false;
        }

        bucket.tokens -= 1.0;
        return true;
    }

    public static void removePlayer(UUID uuid) {
        buckets.remove(uuid);
    }

    public static void clearAll() {
        buckets.clear();
    }
}
