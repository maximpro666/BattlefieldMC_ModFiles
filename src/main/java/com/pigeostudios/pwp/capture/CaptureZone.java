package com.pigeostudios.pwp.capture;

import com.pigeostudios.pwp.core.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import java.util.*;

public class CaptureZone {
    private final String id;
    private final String name;
    private final String dimension;
    private final BlockPos min;
    private final BlockPos max;
    private final int captureSeconds;
    private final String pointType;

    private Team ownerTeam;
    private Team capturingTeam;
    private float progress;
    private final Map<UUID, Double> captureContributions;
    private int vcRate;
    private boolean contested;

    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "name";
    private static final String TAG_DIM = "dim";
    private static final String TAG_MIN_X = "minX";
    private static final String TAG_MIN_Y = "minY";
    private static final String TAG_MIN_Z = "minZ";
    private static final String TAG_MAX_X = "maxX";
    private static final String TAG_MAX_Y = "maxY";
    private static final String TAG_MAX_Z = "maxZ";
    private static final String TAG_CAPTURE_SEC = "captureSec";
    private static final String TAG_OWNER = "owner";
    private static final String TAG_CAPTURING = "capturing";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_POINT_TYPE = "pointType";
    private static final String TAG_VC_RATE = "vcRate";
    private static final String TAG_CONTESTED = "contested";

    public CaptureZone(String id, String name, String dimension, BlockPos pos1, BlockPos pos2, int captureSeconds) {
        this(id, name, dimension, pos1, pos2, captureSeconds, "small");
    }

    public CaptureZone(String id, String name, String dimension, BlockPos pos1, BlockPos pos2, int captureSeconds, String pointType) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.min = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
        this.max = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
        this.captureSeconds = Math.max(1, captureSeconds);
        this.pointType = pointType;
        this.ownerTeam = Team.SPECTATOR;
        this.capturingTeam = Team.SPECTATOR;
        this.progress = 0.0f;
        this.captureContributions = new HashMap<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDimension() { return dimension; }
    public BlockPos getMin() { return min; }
    public BlockPos getMax() { return max; }
    public int getCaptureSeconds() { return captureSeconds; }
    public boolean isMain() { return "major".equals(pointType); }
    public String getPointType() { return pointType; }
    public Team getOwnerTeam() { return ownerTeam; }
    public Team getCapturingTeam() { return capturingTeam; }
    public float getProgress() { return progress; }
    public int getVcRate() { return vcRate; }
    public void setVcRate(int rate) { this.vcRate = Math.max(0, rate); }

    public void setOwnerTeam(Team t) { this.ownerTeam = t; }
    public void setCapturingTeam(Team t) { this.capturingTeam = t; }
    public void setProgress(float p) { this.progress = Math.max(0.0f, Math.min(1.0f, p)); }
    public void addProgress(float amount) { this.progress = Math.max(0.0f, Math.min(1.0f, this.progress + amount)); }

    public boolean isCaptured() { return ownerTeam.isPlayable() && progress >= 1.0f; }
    public boolean isContested() { return contested; }
    public void setContested(boolean c) { this.contested = c; }

    public void addContribution(UUID playerUUID, double amount) {
        captureContributions.merge(playerUUID, amount, Double::sum);
    }

    public double getContribution(UUID playerUUID) {
        return captureContributions.getOrDefault(playerUUID, 0.0);
    }

    public Map<UUID, Double> getCaptureContributions() {
        return captureContributions;
    }

    public double getTotalCaptureContributions() {
        double total = 0;
        for (double v : captureContributions.values()) total += v;
        return total;
    }

    public void clearCaptureContributions() {
        captureContributions.clear();
    }

    public BlockPos getCenter() {
        return new BlockPos(
            (min.getX() + max.getX()) / 2,
            (min.getY() + max.getY()) / 2,
            (min.getZ() + max.getZ()) / 2
        );
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean contains(Entity entity) {
        return contains(entity.blockPosition());
    }

    public void reset() {
        this.ownerTeam = Team.SPECTATOR;
        this.capturingTeam = Team.SPECTATOR;
        this.progress = 0.0f;
        this.captureContributions.clear();
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ID, id);
        tag.putString(TAG_NAME, name);
        tag.putString(TAG_DIM, dimension);
        tag.putInt(TAG_MIN_X, min.getX());
        tag.putInt(TAG_MIN_Y, min.getY());
        tag.putInt(TAG_MIN_Z, min.getZ());
        tag.putInt(TAG_MAX_X, max.getX());
        tag.putInt(TAG_MAX_Y, max.getY());
        tag.putInt(TAG_MAX_Z, max.getZ());
        tag.putInt(TAG_CAPTURE_SEC, captureSeconds);
        tag.putInt(TAG_OWNER, ownerTeam.ordinal());
        tag.putInt(TAG_CAPTURING, capturingTeam.ordinal());
        tag.putFloat(TAG_PROGRESS, progress);
        tag.putString(TAG_POINT_TYPE, pointType);
        tag.putInt(TAG_VC_RATE, vcRate);
        tag.putBoolean(TAG_CONTESTED, contested);
        return tag;
    }

    public static CaptureZone fromNBT(CompoundTag tag) {
        BlockPos min = new BlockPos(tag.getInt(TAG_MIN_X), tag.getInt(TAG_MIN_Y), tag.getInt(TAG_MIN_Z));
        BlockPos max = new BlockPos(tag.getInt(TAG_MAX_X), tag.getInt(TAG_MAX_Y), tag.getInt(TAG_MAX_Z));
        String pointType = tag.contains(TAG_POINT_TYPE) ? tag.getString(TAG_POINT_TYPE) : "small";
        CaptureZone zone = new CaptureZone(
            tag.getString(TAG_ID),
            tag.getString(TAG_NAME),
            tag.getString(TAG_DIM),
            min, max,
            tag.getInt(TAG_CAPTURE_SEC),
            pointType
        );
        zone.ownerTeam = Team.fromOrdinal(tag.getInt(TAG_OWNER));
        zone.capturingTeam = Team.fromOrdinal(tag.getInt(TAG_CAPTURING));
        zone.progress = tag.getFloat(TAG_PROGRESS);
        if (tag.contains(TAG_VC_RATE)) zone.vcRate = tag.getInt(TAG_VC_RATE);
        if (tag.contains(TAG_CONTESTED)) zone.contested = tag.getBoolean(TAG_CONTESTED);
        return zone;
    }
}
