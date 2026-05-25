package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.MapConfig;
import com.pigeostudios.pwp.core.MapPoolManager;
import com.pigeostudios.pwp.core.Team;

import static com.pigeostudios.pwp.core.ChatHelper.*;
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
                        .suggests((ctx, builder) -> {
                            MapPoolManager pool = PWP.getMapPoolManager();
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                pool.getMaps().stream().map(MapConfig::getName),
                                builder);
                        })
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
        GameManager game = PWP.getGameManager();

        if (game.isPlaying()) {
            context.getSource().sendFailure(
                error("Game is already in progress!"));
            return 0;
        }

        game.startGame();
        return 1;
    }

    private static int endGame(CommandContext<CommandSourceStack> context) {
        GameManager game = PWP.getGameManager();
        if (game == null) { return 0; }

        if (!game.isPlaying()) {
            context.getSource().sendFailure(
                error("No game is currently running."));
            return 0;
        }

        var reg = PWP.getServiceRegistry();
        var ts = reg != null ? reg.getTickets() : null;
        Team winner = Team.NATO;
        if (ts != null) {
            int nt = ts.getTickets(Team.NATO);
            int rt = ts.getTickets(Team.RUSSIA);
            winner = nt >= rt ? Team.NATO : Team.RUSSIA;
        }
        game.endGame(winner);
        return 1;
    }

    private static int setMap(CommandContext<CommandSourceStack> context) {
        MapPoolManager pool = PWP.getMapPoolManager();
        String name = StringArgumentType.getString(context, "name");

        if (pool.selectMap(name)) {
            context.getSource().sendSuccess(() ->
                success("Next map set to: " + name), true);
        } else {
            context.getSource().sendFailure(
                error("Map not found: " + name));
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        GameManager game = PWP.getGameManager();
        MapPoolManager pool = PWP.getMapPoolManager();

        String phaseName;
        switch (game.getCurrentPhase()) {
            case LOBBY: phaseName = "LOBBY"; break;
            case PLAYING: phaseName = "PLAYING"; break;
            case ENDING: phaseName = "ENDING"; break;
            default: phaseName = "UNKNOWN";
        }

        context.getSource().sendSuccess(() ->
            header("=== Game Status ==="), false);
        context.getSource().sendSuccess(() ->
            info("Phase: " + phaseName), false);

        String mapName = pool.getCurrentMap().map(m -> m.getName()).orElse("none");
        context.getSource().sendSuccess(() ->
            warning("Current Map: " + mapName), false);

        if (game.getWinningTeam() != null) {
            context.getSource().sendSuccess(() ->
                Component.literal("Winner: ").append(game.getWinningTeam().getColoredName()), false);
        }

        return 1;
    }

    private static int countdown(CommandContext<CommandSourceStack> context) {
        GameManager game = PWP.getGameManager();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        game.startCountdown(seconds);
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> context) {
        GameManager game = PWP.getGameManager();
        game.cancelCountdown();
        return 1;
    }
}
