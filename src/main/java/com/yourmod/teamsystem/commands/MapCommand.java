package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.MapPoolManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class MapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("map")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                    .executes(context -> listMaps(context))
                )
                .then(Commands.literal("set")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> setMapByIndex(context))
                    )
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> setMapByName(context))
                    )
                )
                .then(Commands.literal("next")
                    .executes(context -> nextMap(context))
                )
                .then(Commands.literal("reload")
                    .executes(context -> reloadConfig(context))
                )
                .then(Commands.literal("info")
                    .executes(context -> infoMap(context))
                )
                .then(Commands.literal("backup")
                    .executes(context -> backupMap(context))
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

    private static int setMapByIndex(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        int index = IntegerArgumentType.getInteger(context, "index");

        if (pool.setCurrentMap(index)) {
            Optional<MapConfig> map = pool.getCurrentMap();
            context.getSource().sendSuccess(() ->
                Component.literal("Current map set to: " + map.get().getName())
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendFailure(
                Component.literal("Invalid map index. Use /map list to see available maps.")
                    .withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int setMapByName(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String name = StringArgumentType.getString(context, "name");

        if (pool.setCurrentMap(name)) {
            context.getSource().sendSuccess(() ->
                Component.literal("Current map set to: " + name)
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendFailure(
                Component.literal("Map not found: " + name)
                    .withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int nextMap(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        MapConfig next = pool.nextMap();

        if (next != null) {
            context.getSource().sendSuccess(() ->
                Component.literal("Next map: " + next.getName())
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendFailure(
                Component.literal("No maps configured!")
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

    private static int backupMap(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();
        if (game == null) {
            context.getSource().sendFailure(
                Component.literal("Game manager not initialized.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        game.reBackupCurrentMap();
        context.getSource().sendSuccess(() ->
            Component.literal("Map backup updated.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int infoMap(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        Optional<MapConfig> optMap = pool.getCurrentMap();

        if (optMap.isPresent()) {
            MapConfig map = optMap.get();
            context.getSource().sendSuccess(() ->
                Component.literal("=== Current Map: " + map.getName() + " ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            context.getSource().sendSuccess(() ->
                Component.literal("World: " + map.getWorldFolder()), false);
            context.getSource().sendSuccess(() ->
                Component.literal("Respawn: " + (map.hasRespawn() ? "ON" : "OFF")), false);
            context.getSource().sendSuccess(() ->
                Component.literal("Capture Points: " + (map.hasCapturePoints() ? "ON" : "OFF")), false);
            context.getSource().sendSuccess(() ->
                Component.literal("Regen: " + (map.hasRegen() ? "ON" : "OFF")), false);
            context.getSource().sendSuccess(() ->
                Component.literal("World Border: " + (map.hasWorldBorder() ? "ON" : "OFF")), false);
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
