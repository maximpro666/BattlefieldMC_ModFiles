package com.yourmod.teamsystem.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class CapturePointData {
    public enum CaptureState {
        NEUTRAL, CAPTURING, CONTESTED, CAPTURED
    }

    private final int id;
    private final String name;
    private final BlockPos pos;
    private final double captureRadius;
    private final double captureSpeed;
    private CaptureState state;
    private Team ownerTeam;
    private Team capturingTeam;
    private double progress;

    public CapturePointData(int id, String name, BlockPos pos, double captureRadius, double captureSpeed) {
        this.id = id;
        this.name = name;
        this.pos = pos;
        this.captureRadius = captureRadius > 0 ? captureRadius : 5.0;
        this.captureSpeed = captureSpeed > 0 ? captureSpeed : 1.0;
        this.state = CaptureState.NEUTRAL;
        this.ownerTeam = Team.SPECTATOR;
        this.capturingTeam = Team.SPECTATOR;
        this.progress = 0.0;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public BlockPos getPos() { return pos; }
    public double getCaptureRadius() { return captureRadius; }
    public double getCaptureSpeed() { return captureSpeed; }
    public CaptureState getState() { return state; }
    public Team getOwnerTeam() { return ownerTeam; }
    public Team getCapturingTeam() { return capturingTeam; }
    public double getProgress() { return progress; }

    public void setState(CaptureState state) { this.state = state; }
    public void setOwnerTeam(Team team) { this.ownerTeam = team; }
    public void setCapturingTeam(Team team) { this.capturingTeam = team; }
    public void setProgress(double progress) { this.progress = Math.max(0, Math.min(100, progress)); }

    public void addProgress(double amount) {
        this.progress = Math.max(0, Math.min(100, this.progress + amount));
    }

    public boolean isCaptured() { return state == CaptureState.CAPTURED; }
    public boolean isFullyCaptured() { return state == CaptureState.CAPTURED && progress >= 100.0; }

    public void reset() {
        this.state = CaptureState.NEUTRAL;
        this.ownerTeam = Team.SPECTATOR;
        this.capturingTeam = Team.SPECTATOR;
        this.progress = 0.0;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putString("Name", name);
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putDouble("CaptureRadius", captureRadius);
        tag.putDouble("CaptureSpeed", captureSpeed);
        tag.putInt("State", state.ordinal());
        tag.putInt("OwnerTeam", ownerTeam.ordinal());
        tag.putInt("CapturingTeam", capturingTeam.ordinal());
        tag.putDouble("Progress", progress);
        return tag;
    }

    public static CapturePointData fromNBT(CompoundTag tag) {
        BlockPos pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        CapturePointData data = new CapturePointData(
            tag.getInt("Id"),
            tag.getString("Name"),
            pos,
            tag.getDouble("CaptureRadius"),
            tag.getDouble("CaptureSpeed")
        );
        data.state = CaptureState.values()[tag.getInt("State")];
        data.ownerTeam = Team.fromOrdinal(tag.getInt("OwnerTeam"));
        data.capturingTeam = Team.fromOrdinal(tag.getInt("CapturingTeam"));
        data.progress = tag.getDouble("Progress");
        return data;
    }
}
