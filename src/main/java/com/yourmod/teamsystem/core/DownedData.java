package com.yourmod.teamsystem.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class DownedData {
    private final UUID playerUUID;
    private final String playerName;
    private final double x, y, z;
    private final String dimension;
    private final UUID downerUUID;
    private int bleedoutTimer;
    private double reviveProgress;
    private int reviveTicks;

    public DownedData(UUID playerUUID, String playerName, double x, double y, double z,
                      String dimension, UUID downerUUID) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.downerUUID = downerUUID;
        this.bleedoutTimer = 20 * 30;
        this.reviveProgress = 0.0;
        this.reviveTicks = 0;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public BlockPos getBlockPos() { return BlockPos.containing(x, y, z); }
    public String getDimension() { return dimension; }
    public UUID getDownerUUID() { return downerUUID; }
    public int getBleedoutTimer() { return bleedoutTimer; }
    public double getReviveProgress() { return reviveProgress; }
    public int getReviveTicks() { return reviveTicks; }

    public void tickBleedout() { bleedoutTimer--; }
    public boolean isBleedoutDead() { return bleedoutTimer <= 0; }

    public void addReviveProgress(double amount) {
        this.reviveProgress = Math.min(100, this.reviveProgress + amount);
        this.reviveTicks++;
    }

    public void resetReviveProgress() {
        this.reviveProgress = 0.0;
        this.reviveTicks = 0;
    }

    public boolean isRevived() { return reviveProgress >= 100.0; }

    public double distanceToSqr(double ox, double oy, double oz) {
        double dx = this.x - ox;
        double dy = this.y - oy;
        double dz = this.z - oz;
        return dx * dx + dy * dy + dz * dz;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PlayerUUID", playerUUID);
        tag.putString("PlayerName", playerName);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putString("Dimension", dimension);
        if (downerUUID != null) tag.putUUID("DownerUUID", downerUUID);
        tag.putInt("BleedoutTimer", bleedoutTimer);
        tag.putDouble("ReviveProgress", reviveProgress);
        tag.putInt("ReviveTicks", reviveTicks);
        return tag;
    }

    public static DownedData fromNBT(CompoundTag tag) {
        DownedData data = new DownedData(
            tag.getUUID("PlayerUUID"),
            tag.getString("PlayerName"),
            tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
            tag.getString("Dimension"),
            tag.hasUUID("DownerUUID") ? tag.getUUID("DownerUUID") : null
        );
        data.bleedoutTimer = tag.getInt("BleedoutTimer");
        data.reviveProgress = tag.getDouble("ReviveProgress");
        data.reviveTicks = tag.getInt("ReviveTicks");
        return data;
    }
}
