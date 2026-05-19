package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class BorderManager {

    private final List<BorderZone> zones = new ArrayList<>();
    private double warningDist = 5.0;
    private double pushStrength = 0.3;
    private double teleportMargin = 3.0;
    private int tickCounter;
    private static final int WARNING_INTERVAL = 40;
    private static final int PUSH_INTERVAL = 2;

    public BorderManager() {}

    public void setZones(List<BorderZone> zones) {
        this.zones.clear();
        if (zones != null) this.zones.addAll(zones);
    }

    public void setWarningDist(double v) { this.warningDist = v; }
    public void setPushStrength(double v) { this.pushStrength = v; }
    public void setTeleportMargin(double v) { this.teleportMargin = v; }

    public double getWarningDist() { return warningDist; }
    public double getPushStrength() { return pushStrength; }
    public double getTeleportMargin() { return teleportMargin; }

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

        VehicleManager vm = TeamSystem.getVehicleManager();

        for (Entity entity : level.getAllEntities()) {
            if (!entity.isAlive()) continue;
            boolean isPlayer = entity instanceof ServerPlayer;
            boolean isVehicle = vm != null && vm.isVehicleEntityType(entity);
            if (!isPlayer && !isVehicle) continue;

            double x = entity.getX();
            double z = entity.getZ();

            if (isInsideAny(x, z)) {
                if (isPlayer && tickCounter % WARNING_INTERVAL == 0) {
                    ServerPlayer player = (ServerPlayer) entity;
                    double edgeDist = distanceToNearestEdge(x, z);
                    if (edgeDist >= 0 && edgeDist < warningDist) {
                        player.displayClientMessage(
                            Component.literal("§c\u26a0 You are approaching the battlefield border!"), true);
                        player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                    }
                }
                continue;
            }

            double[] safe = nearestSafePoint(x, z);
            double outDist = Math.sqrt((x - safe[0]) * (x - safe[0]) + (z - safe[1]) * (z - safe[1]));

            if (outDist > teleportMargin) {
                if (isPlayer) {
                    ServerPlayer player = (ServerPlayer) entity;
                    player.teleportTo(level, safe[0], player.getY(), safe[1],
                        player.getYRot(), player.getXRot());
                    player.displayClientMessage(
                        Component.literal("§c\u26a0 You have left the battlefield!"), false);
                } else if (isVehicle) {
                    entity.teleportTo(safe[0], entity.getY(), safe[1]);
                }
                continue;
            }

            if (tickCounter % PUSH_INTERVAL == 0) {
                Vec3 push = new Vec3(safe[0] - x, 0, safe[1] - z).normalize().scale(pushStrength);
                entity.setDeltaMovement(entity.getDeltaMovement().add(push));
                entity.hurtMarked = true;
                if (isPlayer) {
                    ServerPlayer player = (ServerPlayer) entity;
                    player.displayClientMessage(
                        Component.literal("§c\u26a0 Return to the battlefield!"), true);
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                }
            }
        }
    }

    private double distanceToNearestEdge(double x, double z) {
        double minDist = Double.MAX_VALUE;
        for (BorderZone zone : zones) {
            if (!zone.contains(x, z)) continue;
            double d = zone.distanceSq(x, z);
            minDist = Math.min(minDist, d);
        }
        return minDist == Double.MAX_VALUE ? -1 : Math.sqrt(minDist);
    }
}
