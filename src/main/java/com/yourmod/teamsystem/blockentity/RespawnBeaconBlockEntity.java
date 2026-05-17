package com.yourmod.teamsystem.blockentity;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RespawnBeaconBlockEntity extends BlockEntity {

    private UUID ownerUUID;
    private String ownerName;
    private String name;
    private int teamOrdinal;
    private long placedTime;

    public RespawnBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(TeamSystem.RESPAWN_BEACON_BLOCK_ENTITY.get(), pos, state);
        this.ownerUUID = null;
        this.ownerName = "";
        this.name = "";
        this.teamOrdinal = -1;
        this.placedTime = 0;
    }

    public void setOwner(UUID uuid, String name, int teamOrdinal, String beaconName) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        this.teamOrdinal = teamOrdinal;
        this.name = beaconName;
        this.placedTime = level != null ? level.getGameTime() : 0;
        setChanged();
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public String getName() { return name; }
    public int getTeamOrdinal() { return teamOrdinal; }
    public long getPlacedTime() { return placedTime; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
            tag.putString("OwnerName", ownerName);
            tag.putString("BeaconName", name);
            tag.putInt("TeamOrdinal", teamOrdinal);
            tag.putLong("PlacedTime", placedTime);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
            this.ownerName = tag.getString("OwnerName");
            this.name = tag.getString("BeaconName");
            this.teamOrdinal = tag.getInt("TeamOrdinal");
            this.placedTime = tag.getLong("PlacedTime");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
