package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class PingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ping")
            .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("label", StringArgumentType.greedyString())
                            .executes(ctx -> placePing(ctx.getSource(),
                                DoubleArgumentType.getDouble(ctx, "x"),
                                DoubleArgumentType.getDouble(ctx, "y"),
                                DoubleArgumentType.getDouble(ctx, "z"),
                                StringArgumentType.getString(ctx, "label")))))))
            .then(Commands.literal("clear")
                .executes(ctx -> clearPings(ctx.getSource())))
            .then(Commands.literal("list")
                .executes(ctx -> listPings(ctx.getSource()))));
    }

    private static int placePing(CommandSourceStack source, double x, double y, double z, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        TeamManager tm = PWP.getTeamManager();
        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();

        if (!playerTeam.isPlayable()) {
            source.sendFailure(Component.literal("§cSpectators cannot place pings"));
            return 0;
        }

        BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
        ServerLevel level = (ServerLevel) player.level();

        Vec3 center = Vec3.atCenterOf(pos);
        for (ServerPlayer teammate : level.getServer().getPlayerList().getPlayers()) {
            Team teamTeam = tm.getOrCreatePlayerData(teammate.getUUID()).getTeam();
            if (teamTeam == playerTeam) {
                teammate.displayClientMessage(Component.literal(
                    String.format("§a%s§6 pinged at §b%d, %d, %d§6: §a%s",
                        player.getName().getString(), (int)x, (int)y, (int)z, label)), false);
            }
        }

        source.sendSuccess(() -> Component.literal("§aPing placed at §b" + pos), false);
        return 1;
    }

    private static int clearPings(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        source.sendSuccess(() -> Component.literal("§aYour pings cleared"), false);
        return 1;
    }

    private static int listPings(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        source.sendSuccess(() -> Component.literal("§6Your team's pings:"), false);
        source.sendSuccess(() -> Component.literal("§6(No pings active)"), false);
        return 1;
    }
}
