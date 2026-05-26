package com.pigeostudios.pwp.core;

import static com.pigeostudios.pwp.core.PWPColors.*;

import com.pigeostudios.pwp.blockentity.RespawnBeaconBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RespawnBeaconBlock extends Block implements EntityBlock {

    public RespawnBeaconBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RespawnBeaconBlockEntity(pos, state);
    }
}
