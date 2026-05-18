package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.RespawnManager;
import static com.yourmod.teamsystem.core.ChatHelper.*;
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
            success("Respawn beacon '" + name + "' placed"), false);
        return 1;
    }

    private static int listBeacons(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        RespawnManager rm = TeamSystem.getRespawnManager();
        List<RespawnManager.SavedBeacon> beacons = rm.getBeaconsForPlayer(player.getUUID());

        if (beacons.isEmpty()) {
            source.sendSuccess(() ->
                warning("You have no respawn beacons. Use /respawn set <name>"), false);
            return 1;
        }

        source.sendSuccess(() ->
            header("--- Your Respawn Beacons ---"), false);
        for (RespawnManager.SavedBeacon b : beacons) {
            source.sendSuccess(() ->
                bright(" - " + b.name + " @ " + b.x + ", " + b.y + ", " + b.z), false);
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
            success("Respawning at beacon '" + name + "'..."), false);
        rm.respawnPlayerAtBeacon(player, name);
        return 1;
    }

    private static int removeBeacon(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        RespawnManager rm = TeamSystem.getRespawnManager();
        if (rm.removeBeacon(name, player)) {
            source.sendSuccess(() ->
                success("Beacon '" + name + "' removed"), false);
        } else {
            source.sendFailure(Component.literal("Beacon '" + name + "' not found"));
        }
        return 1;
    }
}
