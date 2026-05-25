package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BorderManager {

    private final List<BorderZone> zones = new ArrayList<>();
    private double warningDist = 8.0;
    private double pushStrength = 0.3;
    private double teleportMargin = 10.0;
    private double pushZoneDist = 20.0;
    private int tickCounter;
    private static final int WARNING_INTERVAL = 40;
    private static final int PUSH_INTERVAL = 1;
    private static final int PARTICLE_INTERVAL = 5;
    private static final int PARTICLE_COLUMNS = 3;
    private static final int PARTICLE_PER_COLUMN = 4;

    public BorderManager() {}

    public void setZones(List<BorderZone> zones) {
        this.zones.clear();
        if (zones != null) this.zones.addAll(zones);
    }

    public void setWarningDist(double v) { this.warningDist = v; }
    public void setPushStrength(double v) { this.pushStrength = v; }
    public void setTeleportMargin(double v) { this.teleportMargin = v; }
    public void setPushZoneDist(double v) { this.pushZoneDist = v; }

    public double getWarningDist() { return warningDist; }
    public double getPushStrength() { return pushStrength; }
    public double getTeleportMargin() { return teleportMargin; }
    public double getPushZoneDist() { return pushZoneDist; }

    public boolean hasZones() {
        return !zones.isEmpty();
    }

    public boolean isInsideAny(double x, double z) {
        if (zones.isEmpty()) return true;
        for (BorderZone zone : zones) {
            if (zone.contains(x, z)) return true;
        }
        return false;
    }

    public double[] nearestSafePoint(double x, double z) {
        if (zones.isEmpty()) return new double[]{x, z};
        double bestDist = Double.MAX_VALUE;
        double bestX = x, bestZ = z;
        for (BorderZone zone : zones) {
            double[] p = zone.closestPoint(x, z);
            double dx = x - p[0];
            double dz = z - p[1];
            double d = dx * dx + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                bestX = p[0];
                bestZ = p[1];
            }
        }
        return new double[]{bestX, bestZ};
    }

    public void tick(ServerLevel level) {
        if (!hasZones()) return;
        tickCounter++;

        boolean doPush = tickCounter % PUSH_INTERVAL == 0;
        boolean doWarning = tickCounter % WARNING_INTERVAL == 0;
        boolean doParticles = tickCounter % PARTICLE_INTERVAL == 0;

        VehicleManager vm = PWP.getVehicleManager();

        List<Entity> targets = new ArrayList<>(level.players());
        if (vm != null) {
            for (UUID vid : vm.getSpawnedVehicleIds()) {
                Entity e = level.getEntity(vid);
                if (e != null && e.isAlive()) targets.add(e);
            }
        }

        for (Entity entity : targets) {
            if (!entity.isAlive()) continue;
            boolean isPlayer = entity instanceof ServerPlayer;
            boolean isVehicle = vm != null && vm.getSpawnedVehicleIds().contains(entity.getUUID());

            double x = entity.getX();
            double z = entity.getZ();
            double edgeDist = distanceToNearestEdge(x, z);

            if (edgeDist >= 0) {
                if (isPlayer) {
                    ServerPlayer player = (ServerPlayer) entity;
                    if (doWarning && edgeDist < warningDist) {
                        player.displayClientMessage(
                            Component.literal("§c\u26a0 You are near the battlefield border!"), true);
                        player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                    }
                    if (doParticles && edgeDist < warningDist) {
                        spawnBorderParticles(player, level, x, z);
                    }
                }

                if (doPush && edgeDist < pushZoneDist) {
                    double[] safe = nearestSafePoint(x, z);
                    double t = edgeDist / pushZoneDist;
                    double strength = pushStrength * (1.0 - t * t);
                    Vec3 dir = new Vec3(safe[0] - x, 0, safe[1] - z).normalize();

                    if (isVehicle) {
                        strength *= 3.0;
                        if (edgeDist < 8.0) {
                            double pull = (8.0 - edgeDist) * 0.04;
                            entity.setPos(
                                entity.getX() + dir.x * pull,
                                entity.getY(),
                                entity.getZ() + dir.z * pull
                            );
                        }
                    }

                    entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(strength)));
                    entity.hurtMarked = true;
                }
            } else {
                double outDist = -edgeDist;
                double[] safe = nearestSafePoint(x, z);
                Vec3 dir = new Vec3(safe[0] - x, 0, safe[1] - z).normalize();

                if (isVehicle) {
                    double pullBack = Math.min(1.0, outDist * 0.1 + 0.3);
                    entity.setPos(
                        entity.getX() + dir.x * pullBack,
                        entity.getY(),
                        entity.getZ() + dir.z * pullBack
                    );

                    if (outDist > teleportMargin || pullBack >= 1.0) {
                        entity.teleportTo(safe[0], entity.getY(), safe[1]);
                        entity.setDeltaMovement(Vec3.ZERO);
                        continue;
                    }
                }

                if (outDist > teleportMargin) {
                    if (isPlayer) {
                        ServerPlayer player = (ServerPlayer) entity;
                        player.teleportTo(level, safe[0], player.getY(), safe[1],
                            player.getYRot(), player.getXRot());
                        player.displayClientMessage(
                            Component.literal("§c\u26a0 You have left the battlefield!"), false);
                    }
                    continue;
                }

                if (doPush) {
                    double vehicleMult = isVehicle ? 3.0 : 1.0;
                    entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(pushStrength * (1.0 + outDist) * vehicleMult)));
                    entity.hurtMarked = true;
                }

                if (isPlayer) {
                    ServerPlayer player = (ServerPlayer) entity;
                    player.displayClientMessage(
                        Component.literal("§c\u26a0 Return to the battlefield!"), true);
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                }
            }
        }
    }

    private void spawnBorderParticles(ServerPlayer player, ServerLevel level, double px, double pz) {
        double[] edge = nearestSafePoint(px, pz);
        double dx = px - edge[0];
        double dz = pz - edge[1];
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.5) return;
        double nx = dx / len;
        double nz = dz / len;

        for (int col = -PARTICLE_COLUMNS / 2; col <= PARTICLE_COLUMNS / 2; col++) {
            double ox = nx * col;
            double oz = nz * col;
            for (int row = 0; row < PARTICLE_PER_COLUMN; row++) {
                double yOff = 1.0 + row * 1.2;
                level.sendParticles(player,
                    ParticleTypes.FLAME, false,
                    edge[0] + ox, yOff, edge[1] + oz,
                    1, 0, 0, 0, 0);
            }
        }
    }

    private double distanceToNearestEdge(double x, double z) {
        double minDist = Double.MAX_VALUE;
        for (BorderZone zone : zones) {
            if (!zone.contains(x, z)) continue;
            double d = zone.distanceSq(x, z);
            if (d < minDist) minDist = d;
        }
        if (minDist != Double.MAX_VALUE) return Math.sqrt(minDist);
        double[] safe = nearestSafePoint(x, z);
        double dx = x - safe[0];
        double dz = z - safe[1];
        return -Math.sqrt(dx * dx + dz * dz);
    }
}
