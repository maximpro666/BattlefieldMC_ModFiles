package com.pigeostudios.pwp.core;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MarkerData {
    public enum MarkerType {
        POINT,
        ATTACK,
        DEFEND,
        OBSERVE
    }

    private final String name;
    private final String label;
    private final ResourceLocation dimension;
    private final double x;
    private final double y;
    private final double z;
    private final int teamOrdinal;
    private final MarkerType type;
    private final UUID creatorUUID;
    private long expiryTime;
    private boolean isPing;

    public MarkerData(String name, String label, ResourceLocation dimension, double x, double y, double z,
                      int teamOrdinal, MarkerType type, UUID creatorUUID) {
        this.name = name;
        this.label = label != null ? label : name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.teamOrdinal = teamOrdinal;
        this.type = type;
        this.creatorUUID = creatorUUID;
        this.expiryTime = -1;
        this.isPing = false;
    }

    public String getName() { return name; }
    public String getLabel() { return label; }
    public ResourceLocation getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getTeamOrdinal() { return teamOrdinal; }
    public MarkerType getType() { return type; }
    public UUID getCreatorUUID() { return creatorUUID; }
    public long getExpiryTime() { return expiryTime; }
    public boolean isPing() { return isPing; }

    public void setExpiryTime(long expiryTime) { this.expiryTime = expiryTime; }
    public void setPing(boolean ping) { this.isPing = ping; }

    public boolean isExpired(long currentTime) {
        return expiryTime > 0 && currentTime >= expiryTime;
    }

    public BlockPos getBlockPos() {
        return BlockPos.containing(x, y, z);
    }
}
