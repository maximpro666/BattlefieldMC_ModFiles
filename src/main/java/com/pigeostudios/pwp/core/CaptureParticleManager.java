package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.capture.ZoneDataManager;
import com.pigeostudios.pwp.capture.CaptureZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class CaptureParticleManager {
    private static final int INTERVAL_TICKS = 50;
    private static final double CHECK_RADIUS = 64.0;
    private static final Random RANDOM = new Random();
    private int tickCounter = 0;

    public void tick(ServerLevel level, ZoneDataManager data) {
        if (!data.isActive()) return;
        String dimId = level.dimension().location().toString();
        List<CaptureZone> zones = data.getZones(dimId);
        if (zones.isEmpty()) return;

        tickCounter++;
        if (tickCounter < INTERVAL_TICKS) return;
        tickCounter = 0;

        for (CaptureZone zone : zones) {
            BlockPos center = zone.getCenter();

            // Check if any players are nearby before spawning particles
            boolean hasNearbyPlayers = false;
            for (ServerPlayer player : level.players()) {
                if (player.position().distanceTo(new Vec3(center.getX(), center.getY(), center.getZ())) < CHECK_RADIUS) {
                    hasNearbyPlayers = true;
                    break;
                }
            }
            if (!hasNearbyPlayers) continue;

            // Calculate radius from half-diagonal of the zone
            BlockPos min = zone.getMin();
            BlockPos max = zone.getMax();
            double radius = Math.sqrt(
                Math.pow(max.getX() - min.getX(), 2) +
                Math.pow(max.getZ() - min.getZ(), 2)
            ) / 2.0;

            int particleCount = 8;
            double angleStep = 2.0 * Math.PI / particleCount;
            double baseAngle = (level.getGameTime() % 200) * 0.01;

            for (int i = 0; i < particleCount; i++) {
                double angle = baseAngle + i * angleStep;
                double px = center.getX() + 0.5 + radius * Math.cos(angle);
                double pz = center.getZ() + 0.5 + radius * Math.sin(angle);
                double py = center.getY() + 1.0 + RANDOM.nextDouble() * 2.0;

                level.sendParticles(
                    ParticleTypes.END_ROD,
                    px, py, pz, 1, 0, 0, 0, 0
                );
            }
        }
    }
}
