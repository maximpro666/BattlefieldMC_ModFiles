package com.pigeostudios.pwp.blockentity;

import com.pigeostudios.pwp.PWP;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RespawnBeaconBlockEntity extends BlockEntity {

    public RespawnBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(PWP.RESPAWN_BEACON_BLOCK_ENTITY.get(), pos, state);
    }
}
