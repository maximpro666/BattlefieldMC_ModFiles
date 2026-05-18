package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.MapPoolManager;
import com.yourmod.teamsystem.core.MapState;
import static com.yourmod.teamsystem.core.ChatHelper.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("map")
                .then(Commands.literal("list")
                    .executes(context -> listMaps(context))
                )
                .then(Commands.literal("available")
                    .executes(context -> listAvailable(context))
                )
                .then(Commands.literal("vote")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            MapPoolManager pool = TeamSystem.getMapPoolManager();
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                pool.getMapsByState(MapState.AVAILABLE).stream().map(MapConfig::getName),
                                builder);
                        })
                        .executes(context -> voteMap(context))
                    )
                )
                .then(Commands.literal("votes")
                    .executes(context -> showVotes(context))
                )
                .then(Commands.literal("select")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> selectByIndex(context))
                    )
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            MapPoolManager pool = TeamSystem.getMapPoolManager();
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                pool.getMapsByState(MapState.AVAILABLE).stream().map(MapConfig::getName),
                                builder);
                        })
                        .executes(context -> selectByName(context))
                    )
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> reloadConfig(context))
                )
                .then(Commands.literal("info")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> infoMap(context))
                )
                .then(Commands.literal("states")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> listStates(context))
                )
                .then(Commands.literal("maintenance")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> runMaintenance(context))
                )
                .then(Commands.literal("setspawn")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("team", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            new String[]{"nato", "russia"}, builder))
                        .executes(context -> setSpawn(context))
                    )
                )
        );
    }

    private static int listMaps(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String mapList = pool.getMapListFormatted();
        context.getSource().sendSuccess(() ->
            header("=== Map Pool ===\n" + mapList), false);
        return 1;
    }

    private static int listAvailable(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String mapList = pool.getAvailableMapListFormatted();
        context.getSource().sendSuccess(() ->
            styled("=== Available Maps ===\n" + mapList, ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        return 1;
    }

    private static int selectByIndex(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        int index = IntegerArgumentType.getInteger(context, "index") - 1;

        if (pool.selectMap(index)) {
            context.getSource().sendSuccess(() ->
                success("Map selected for next match"), true);
        } else {
            context.getSource().sendFailure(
                error("Invalid index or map not AVAILABLE. Use /map available"));
        }
        return 1;
    }

    private static int selectByName(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String name = StringArgumentType.getString(context, "name");

        if (pool.selectMap(name)) {
            context.getSource().sendSuccess(() ->
                success("Map selected: " + name), true);
        } else {
            context.getSource().sendFailure(
                error("Map not found or not AVAILABLE: " + name));
        }
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        pool.reloadConfig();
        context.getSource().sendSuccess(() ->
            success("Map config reloaded! " + pool.getMaps().size() + " maps loaded."), true);
        return 1;
    }

    private static int listStates(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        context.getSource().sendSuccess(() -> header("=== Map States ==="), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Available: " + pool.getAvailableCount()), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Maintenance running: " + pool.isMaintenanceRunning()), false);
        return 1;
    }

    private static int runMaintenance(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        if (pool.isMaintenanceRunning()) {
            context.getSource().sendFailure(
                error("Maintenance already running!"));
            return 0;
        }
        pool.runMaintenance();
        context.getSource().sendSuccess(() ->
            success("Maintenance complete. Available maps: " + pool.getAvailableCount()), true);
        return 1;
    }

    private static int showVotes(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        Map<String, Integer> tally = pool.getVoteTally();
        if (tally.isEmpty()) {
            context.getSource().sendSuccess(() ->
                warning("No votes yet!"), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§6=== Vote Results ===\n");
        List<Map.Entry<String, Integer>> sorted = tally.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());
        int total = tally.values().stream().mapToInt(Integer::intValue).sum();
        for (Map.Entry<String, Integer> e : sorted) {
            sb.append("§e").append(e.getKey()).append("§7: §f").append(e.getValue())
                .append(" §7(").append(String.format("%.0f%%", e.getValue() * 100.0 / total)).append(")\n");
        }
        context.getSource().sendSuccess(() ->
            header(sb.toString()), false);
        return 1;
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        GameManager game = TeamSystem.getGameManager();
        MapConfig map = game.getCurrentMap();
        if (map == null) {
            context.getSource().sendFailure(error("No map selected!"));
            return 0;
        }

        String team = StringArgumentType.getString(context, "team").toLowerCase();
        int[] pos = new int[]{(int)player.getX(), (int)player.getY(), (int)player.getZ()};

        if (team.equals("nato")) {
            map.setNatoSpawn(pos);
            context.getSource().sendSuccess(() ->
                success("NATO spawn set to " + pos[0] + " " + pos[1] + " " + pos[2]), true);
        } else if (team.equals("russia")) {
            map.setRussiaSpawn(pos);
            context.getSource().sendSuccess(() ->
                success("Russia spawn set to " + pos[0] + " " + pos[1] + " " + pos[2]), true);
        } else {
            context.getSource().sendFailure(error("Use: /map setspawn <nato|russia>"));
            return 0;
        }

        TeamSystem.getMapPoolManager().saveConfig();
        return 1;
    }

    private static int voteMap(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        GameManager game = TeamSystem.getGameManager();
        String name = StringArgumentType.getString(context, "name");

        if (game.voteMap(player, name)) {
            context.getSource().sendSuccess(() ->
                success("Voted for map: " + name), false);
        }
        return 1;
    }

    private static int infoMap(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        java.util.Optional<MapConfig> optMap = pool.getCurrentMap();

        if (optMap.isPresent()) {
            MapConfig map = optMap.get();
            context.getSource().sendSuccess(() ->
                header("=== Current Map: " + map.getName() + " ==="), false);
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
                error("No map currently selected."));
        }
        return 1;
    }
}
