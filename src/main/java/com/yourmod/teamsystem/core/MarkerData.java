package com.yourmod.teamsystem.core;

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
    private final ResourceLocation dimension;
    private final double x;
    private final double y;
    private final double z;
    private final int teamOrdinal;
    private final MarkerType type;
    private final UUID creatorUUID;

    public MarkerData(String name, ResourceLocation dimension, double x, double y, double z,
                      int teamOrdinal, MarkerType type, UUID creatorUUID) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.teamOrdinal = teamOrdinal;
        this.type = type;
        this.creatorUUID = creatorUUID;
    }

    public String getName() { return name; }
    public ResourceLocation getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public int getTeamOrdinal() { return teamOrdinal; }
    public MarkerType getType() { return type; }
    public UUID getCreatorUUID() { return creatorUUID; }

    public BlockPos getBlockPos() {
        return BlockPos.containing(x, y, z);
    }
}
