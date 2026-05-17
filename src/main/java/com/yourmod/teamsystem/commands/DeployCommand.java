package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.RespawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DeployCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deploy")
            .executes(ctx -> deploy(ctx.getSource()))
            .then(Commands.argument("beaconName", StringArgumentType.word())
                .executes(ctx -> deployAt(ctx.getSource(), StringArgumentType.getString(ctx, "beaconName")))));
    }

    private static int deploy(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        RespawnManager rm = TeamSystem.getRespawnManager();
        List<BlockPos> points = rm.getRespawnPointsForPlayer(player);
        if (points.isEmpty()) {
            source.sendFailure(Component.literal("§cNo respawn points available. Place a beacon or FOB first."));
            return 0;
        }
        BlockPos pos = points.get(0);
        player.teleportTo(player.serverLevel(), pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
        player.fallDistance = 0;
        source.sendSuccess(() -> Component.literal("§aDeployed!"), false);
        return 1;
    }

    private static int deployAt(CommandSourceStack source, String beaconName) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        TeamSystem.getRespawnManager().respawnPlayerAtBeacon(player, beaconName);
        return 1;
    }
}
