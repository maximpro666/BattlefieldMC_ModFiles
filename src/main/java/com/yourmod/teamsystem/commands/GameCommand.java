package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapPoolManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class GameCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("game")
                .then(Commands.literal("status")
                    .requires(source -> source.hasPermission(0))
                    .executes(context -> status(context))
                )
                .then(Commands.literal("start")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> startGame(context))
                )
                .then(Commands.literal("end")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> endGame(context))
                )
                .then(Commands.literal("setmap")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> setMap(context))
                    )
                )
                .then(Commands.literal("countdown")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 120))
                        .executes(context -> countdown(context))
                    )
                )
                .then(Commands.literal("cancel")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> cancel(context))
                )
        );
    }

    private static int startGame(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();

        if (game.isPlaying()) {
            context.getSource().sendFailure(
                Component.literal("Game is already in progress!").withStyle(ChatFormatting.RED));
            return 0;
        }

        game.startGame();
        return 1;
    }

    private static int endGame(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();

        if (!game.isPlaying()) {
            context.getSource().sendFailure(
                Component.literal("No game is currently running.").withStyle(ChatFormatting.RED));
            return 0;
        }

        game.endGame(TeamSystem.getTeamManager().getTeamWithMostTickets());
        return 1;
    }

    private static int setMap(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        String name = StringArgumentType.getString(context, "name");

        if (pool.setCurrentMap(name)) {
            context.getSource().sendSuccess(() ->
                Component.literal("Next map set to: " + name).withStyle(ChatFormatting.GREEN), true);
        } else {
            context.getSource().sendFailure(
                Component.literal("Map not found: " + name).withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();
        MapPoolManager pool = TeamSystem.getMapPoolManager();

        String phaseName;
        switch (game.getCurrentPhase()) {
            case LOBBY: phaseName = "LOBBY"; break;
            case PLAYING: phaseName = "PLAYING"; break;
            case ENDING: phaseName = "ENDING"; break;
            default: phaseName = "UNKNOWN";
        }

        context.getSource().sendSuccess(() ->
            Component.literal("=== Game Status ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        context.getSource().sendSuccess(() ->
            Component.literal("Phase: " + phaseName).withStyle(ChatFormatting.AQUA), false);

        String mapName = pool.getCurrentMap().map(m -> m.getName()).orElse("none");
        context.getSource().sendSuccess(() ->
            Component.literal("Current Map: " + mapName).withStyle(ChatFormatting.YELLOW), false);

        if (game.getWinningTeam() != null) {
            context.getSource().sendSuccess(() ->
                Component.literal("Winner: ").append(game.getWinningTeam().getColoredName()), false);
        }

        return 1;
    }

    private static int countdown(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        game.startCountdown(seconds);
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();
        game.cancelCountdown();
        return 1;
    }
}
