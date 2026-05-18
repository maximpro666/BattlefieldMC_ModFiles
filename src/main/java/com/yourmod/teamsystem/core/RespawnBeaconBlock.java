package com.yourmod.teamsystem.core;

import static com.yourmod.teamsystem.core.TeamSystemColors.*;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.blockentity.RespawnBeaconBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RespawnBeaconBlock extends BaseEntityBlock {

    public RespawnBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RespawnBeaconBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RespawnBeaconBlockEntity beacon) {
            if (beacon.getOwnerUUID() != null && beacon.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "This is your respawn beacon. Use /respawn select " + beacon.getName() + " to set it as active."
                ).withStyle(CHAT_SUCCESS));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "This beacon belongs to " + (beacon.getOwnerName() != null ? beacon.getOwnerName() : "someone")
                ).withStyle(CHAT_WARNING));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RespawnBeaconBlockEntity beacon) {
            TeamSystem.getRespawnManager().onBeaconBroken(pos, beacon);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
