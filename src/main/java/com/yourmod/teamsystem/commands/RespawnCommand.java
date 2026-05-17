package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.RespawnManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class RespawnCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("respawn")
                .then(Commands.literal("set")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setBeacon(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                .then(Commands.literal("list")
                    .executes(ctx -> listBeacons(ctx.getSource()))
                )
                .then(Commands.literal("select")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> selectBeacon(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> removeBeacon(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
        );
    }

    private static int setBeacon(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        RespawnManager rm = TeamSystem.getRespawnManager();
        String error = rm.placeBeacon(player, name);
        if (error != null) {
            source.sendFailure(Component.literal(error));
            return 0;
        }

        source.sendSuccess(() ->
            Component.literal("Respawn beacon '" + name + "' placed").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int listBeacons(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        RespawnManager rm = TeamSystem.getRespawnManager();
        List<RespawnManager.SavedBeacon> beacons = rm.getBeaconsForPlayer(player.getUUID());

        if (beacons.isEmpty()) {
            source.sendSuccess(() ->
                Component.literal("You have no respawn beacons. Use /respawn set <name>").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(() ->
            Component.literal("--- Your Respawn Beacons ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (RespawnManager.SavedBeacon b : beacons) {
            source.sendSuccess(() ->
                Component.literal(" - " + b.name + " @ " + b.x + ", " + b.y + ", " + b.z)
                    .withStyle(ChatFormatting.WHITE), false);
        }
        return 1;
    }

    private static int selectBeacon(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        RespawnManager rm = TeamSystem.getRespawnManager();
        RespawnManager.SavedBeacon beacon = rm.getBeaconByName(player.getUUID(), name);
        if (beacon == null) {
            source.sendFailure(Component.literal("Beacon '" + name + "' not found"));
            return 0;
        }

        source.sendSuccess(() ->
            Component.literal("Respawning at beacon '" + name + "'...").withStyle(ChatFormatting.GREEN), false);
        rm.respawnPlayerAtBeacon(player, name);
        return 1;
    }

    private static int removeBeacon(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        RespawnManager rm = TeamSystem.getRespawnManager();
        if (rm.removeBeacon(name, player)) {
            source.sendSuccess(() ->
                Component.literal("Beacon '" + name + "' removed").withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendFailure(Component.literal("Beacon '" + name + "' not found"));
        }
        return 1;
    }
}
