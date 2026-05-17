package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.MapPoolManager;
import com.yourmod.teamsystem.core.MapState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class MapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("map")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                    .executes(context -> listMaps(context))
                )
                .then(Commands.literal("available")
                    .executes(context -> listAvailable(context))
                )
                .then(Commands.literal("select")
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> selectByIndex(context))
                    )
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> selectByName(context))
                    )
                )
                .then(Commands.literal("reload")
                    .executes(context -> reloadConfig(context))
                )
                .then(Commands.literal("info")
                    .executes(context -> infoMap(context))
                )
                .then(Commands.literal("states")
                    .executes(context -> listStates(context))
                )
                .then(Commands.literal("maintenance")
                    .executes(context -> runMaintenance(context))
                )
                .then(Commands.literal("vote")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> voteMap(context))
                    )
                )
        );
    }

    private static int listMaps(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String mapList = pool.getMapListFormatted();
        context.getSource().sendSuccess(() ->
            Component.literal("=== Map Pool ===\n" + mapList)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        return 1;
    }

    private static int listAvailable(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String mapList = pool.getAvailableMapListFormatted();
        context.getSource().sendSuccess(() ->
            Component.literal("=== Available Maps ===\n" + mapList)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        return 1;
    }

    private static int selectByIndex(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        int index = IntegerArgumentType.getInteger(context, "index") - 1;

        if (pool.selectMap(index)) {
            context.getSource().sendSuccess(() ->
                Component.literal("Map selected and set to IN_MATCH")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendFailure(
                Component.literal("Invalid index or map not AVAILABLE. Use /map available")
                    .withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int selectByName(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String name = StringArgumentType.getString(context, "name");

        if (pool.selectMap(name)) {
            context.getSource().sendSuccess(() ->
                Component.literal("Map selected: " + name).withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendFailure(
                Component.literal("Map not found or not AVAILABLE: " + name)
                    .withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.reloadConfig();
        context.getSource().sendSuccess(() ->
            Component.literal("Map config reloaded! " + pool.getMaps().size() + " maps loaded.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int listStates(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        context.getSource().sendSuccess(() -> Component.literal("=== Map States ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Available: " + pool.getAvailableCount()), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Dirty: " + pool.getDirtyCount()), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Maintenance needed: " + pool.isMaintenanceNeeded()), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Maintenance running: " + pool.isMaintenanceRunning()), false);
        return 1;
    }

    private static int runMaintenance(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        if (pool.isMaintenanceRunning()) {
            context.getSource().sendFailure(
                Component.literal("Maintenance already running!").withStyle(ChatFormatting.RED));
            return 0;
        }
        pool.runMaintenance();
        context.getSource().sendSuccess(() ->
            Component.literal("Maintenance complete. Available maps: " + pool.getAvailableCount())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int voteMap(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String name = StringArgumentType.getString(context, "name");

        boolean found = pool.getMapsByState(MapState.AVAILABLE).stream()
            .anyMatch(m -> m.getName().equalsIgnoreCase(name));
        if (!found) {
            context.getSource().sendFailure(
                Component.literal("Map not available for voting: " + name)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        pool.castVote(player, name);
        context.getSource().sendSuccess(() ->
            Component.literal("Voted for map: " + name).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int infoMap(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        java.util.Optional<MapConfig> optMap = pool.getCurrentMap();

        if (optMap.isPresent()) {
            MapConfig map = optMap.get();
            context.getSource().sendSuccess(() ->
                Component.literal("=== Current Map: " + map.getName() + " ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            context.getSource().sendSuccess(() ->
                Component.literal("State: " + map.getState().name()), false);
            context.getSource().sendSuccess(() ->
                Component.literal("World: " + map.getWorldFolder()), false);
            context.getSource().sendSuccess(() ->
                Component.literal("Respawn: " + (map.hasRespawn() ? "ON" : "OFF")), false);
            context.getSource().sendSuccess(() ->
                Component.literal("Capture Points: " + (map.hasCapturePoints() ? "ON" : "OFF")), false);
            context.getSource().sendSuccess(() ->
                Component.literal("Tickets: " + map.getTickets()), false);
        } else {
            context.getSource().sendFailure(
                Component.literal("No map currently selected.")
                    .withStyle(ChatFormatting.RED));
        }
        return 1;
    }
}
