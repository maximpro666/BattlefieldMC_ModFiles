package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.capture.CaptureZone;
import com.yourmod.teamsystem.core.CapturePointManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.MapPoolManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CapturePointCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cp")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                            .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .executes(ctx -> addZone(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "name"),
                                    BlockPosArgument.getBlockPos(ctx, "pos1"),
                                    BlockPosArgument.getBlockPos(ctx, "pos2"),
                                    30))
                                .then(Commands.argument("time", IntegerArgumentType.integer(5, 300))
                                    .executes(ctx -> addZone(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        BlockPosArgument.getBlockPos(ctx, "pos1"),
                                        BlockPosArgument.getBlockPos(ctx, "pos2"),
                                        IntegerArgumentType.getInteger(ctx, "time")))
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("addhere")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 50))
                            .executes(ctx -> addHere(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name"),
                                IntegerArgumentType.getInteger(ctx, "radius"),
                                30))
                            .then(Commands.argument("time", IntegerArgumentType.integer(5, 300))
                                .executes(ctx -> addHere(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "name"),
                                    IntegerArgumentType.getInteger(ctx, "radius"),
                                    IntegerArgumentType.getInteger(ctx, "time")))
                            )
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

    private static int addZone(CommandSourceStack source, String name, BlockPos pos1, BlockPos pos2, int captureTime) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.getCurrentMap().ifPresentOrElse(map -> {
            String dim = player.level().dimension().location().toString();
            CaptureZone zone = new CaptureZone(name, name, dim, pos1, pos2, captureTime);
            CapturePointManager cpm = TeamSystem.getCapturePointManager();
            if (cpm != null) {
                cpm.getZoneData().addZone(zone);
                cpm.setActive(true);
                cpm.syncToAll();
            }
            source.sendSuccess(() -> Component.literal("§aZone '" + name + "' added"), true);
        }, () -> {
            source.sendFailure(Component.literal("§cNo map selected"));
        });
        return 1;
    }

    private static int addHere(CommandSourceStack source, String name, int radius, int captureTime) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        BlockPos center = player.blockPosition();
        BlockPos pos1 = center.offset(-radius, -radius, -radius);
        BlockPos pos2 = center.offset(radius, radius, radius);
        return addZone(source, name, pos1, pos2, captureTime);
    }

    private static int remove(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CapturePointManager cpm = TeamSystem.getCapturePointManager();
        if (cpm == null) return 0;
        String dim = player.level().dimension().location().toString();
        cpm.getZoneData().removeZone(name, dim);
        source.sendSuccess(() -> Component.literal("§aZone '" + name + "' removed"), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CapturePointManager cpm = TeamSystem.getCapturePointManager();
        if (cpm == null) return 0;
        String dim = player.level().dimension().location().toString();
        var zones = cpm.getZoneData().getZones(dim);
        if (zones.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eNo zones in this dimension"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§6=== Capture Zones ==="), false);
        for (CaptureZone z : zones) {
            String owner = z.getOwnerTeam().isPlayable() ? z.getOwnerTeam().getName() : "none";
            source.sendSuccess(() -> Component.literal(
                String.format(" §b%s §7%s-%s §8(time=%ds progress=%.0f%% owner=%s)",
                    z.getName(), z.getMin(), z.getMax(), z.getCaptureSeconds(), z.getProgress() * 100, owner)
            ), false);
        }
        return 1;
    }

    private static int clear(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        CapturePointManager cpm = TeamSystem.getCapturePointManager();
        if (cpm == null) return 0;
        cpm.clearPoints();
        source.sendSuccess(() -> Component.literal("§aAll zones cleared"), true);
        return 1;
    }
}
