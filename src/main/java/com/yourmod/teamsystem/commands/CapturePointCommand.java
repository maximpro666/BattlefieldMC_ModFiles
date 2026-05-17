package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourmod.teamsystem.TeamSystem;
// Removed unused import
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.MapPoolManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CapturePointCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cp")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "name"), 5.0, 1.0))
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 50.0))
                            .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                                DoubleArgumentType.getDouble(ctx, "radius"), 1.0))
                            .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.1, 10.0))
                                .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                                    DoubleArgumentType.getDouble(ctx, "radius"),
                                    DoubleArgumentType.getDouble(ctx, "speed"))))
                        )
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                .then(Commands.literal("list")
                    .executes(ctx -> list(ctx.getSource()))
                )
                .then(Commands.literal("clear")
                    .executes(ctx -> clear(ctx.getSource()))
                )
        );
    }

    private static int add(CommandSourceStack source, String name, double radius, double speed) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.getCurrentMap().ifPresentOrElse(map -> {
            map.addCapturePoint(name, x, y, z, radius, speed);
            pool.saveConfig();
            source.sendSuccess(() -> Component.literal("§aCapture point '" + name + "' added at "
                + x + " " + y + " " + z + " (r=" + radius + ", s=" + speed + ")"), true);

            CapturePointManager cpm = TeamSystem.getCapturePointManager();
            if (cpm != null) {
                cpm.addPointFromConfig(map, name);
            }
        }, () -> {
            source.sendFailure(Component.literal("§cNo map selected. Select a map first with /map select"));
        });
        return 1;
    }

    private static int remove(CommandSourceStack source, String name) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.getCurrentMap().ifPresentOrElse(map -> {
            map.removeCapturePoint(name);
            pool.saveConfig();
            source.sendSuccess(() -> Component.literal("§aCapture point '" + name + "' removed"), true);
        }, () -> {
            source.sendFailure(Component.literal("§cNo map selected"));
        });
        return 1;
    }

    private static int list(CommandSourceStack source) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.getCurrentMap().ifPresentOrElse(map -> {
            var cps = map.getCapturePoints();
            if (cps.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§eNo capture points on current map"), false);
                return;
            }
            source.sendSuccess(() -> Component.literal("§6=== Capture Points ==="), false);
            for (var cp : cps) {
                source.sendSuccess(() -> Component.literal(
                    String.format(" §b%s §7@ %d,%d,%d §8(r=%.1f s=%.1f)", cp.name, cp.x, cp.y, cp.z, cp.radius, cp.captureSpeed)
                ), false);
            }
        }, () -> {
            source.sendFailure(Component.literal("§cNo map selected"));
        });
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.getCurrentMap().ifPresentOrElse(map -> {
            map.clearCapturePoints();
            pool.saveConfig();
            CapturePointManager cpm = TeamSystem.getCapturePointManager();
            if (cpm != null) cpm.clearPoints();
            source.sendSuccess(() -> Component.literal("§aAll capture points cleared"), true);
        }, () -> {
            source.sendFailure(Component.literal("§cNo map selected"));
        });
        return 1;
    }
}
