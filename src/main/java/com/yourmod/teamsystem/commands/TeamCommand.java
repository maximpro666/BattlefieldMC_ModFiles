package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yourmod.teamsystem.core.PlayerCombatData;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public class TeamCommand {

    private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(Team.values()).map(Team::getName),
            builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, TeamManager manager) {
        dispatcher.register(
            Commands.literal("team")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("teamname", StringArgumentType.word())
                    .suggests(TEAM_SUGGESTIONS)
                    .executes(context -> joinTeam(context, manager))
                )
                .executes(context -> showCurrentTeam(context, manager))
        );

        // Literal subcommands for convenience: /team nato, /team russia, /team spectator
        dispatcher.register(
            Commands.literal("team")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("nato")
                    .executes(context -> joinTeamLiteral(context, manager, Team.NATO))
                )
        );

        dispatcher.register(
            Commands.literal("team")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("russia")
                    .executes(context -> joinTeamLiteral(context, manager, Team.RUSSIA))
                )
        );

        dispatcher.register(
            Commands.literal("team")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("spectator")
                    .executes(context -> joinTeamLiteral(context, manager, Team.SPECTATOR))
                )
        );

        dispatcher.register(
            Commands.literal("teamstats")
                .requires(source -> source.hasPermission(0))
                .executes(context -> showStats(context, manager))
        );

        dispatcher.register(
            Commands.literal("teambalance")
                .requires(source -> source.hasPermission(0))
                .executes(context -> showBalance(context, manager))
        );

        dispatcher.register(
            Commands.literal("resetstats")
                .requires(source -> source.hasPermission(2))
                .executes(context -> resetStats(context, manager))
        );
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context, TeamManager manager) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        String teamName = StringArgumentType.getString(context, "teamname");
        Team team = Team.fromString(teamName);

        if (team == null) {
            player.sendSystemMessage(Component.literal("Invalid team! Available teams: NATO, RUSSIA, SPECTATOR")
                .withStyle(ChatFormatting.RED));
            return 0;
        }

        Team currentTeam = manager.getOrCreatePlayerData(player.getUUID()).getTeam();

        if (currentTeam == team) {
            player.sendSystemMessage(Component.literal("You are already in team ")
                .append(team.getColoredName()));
            return 0;
        }

        manager.setPlayerTeam(player, team);

        player.sendSystemMessage(Component.literal("You joined team ")
            .append(team.getColoredName())
            .withStyle(ChatFormatting.GREEN));

        Component announcement = Component.literal(player.getName().getString())
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" joined team "))
            .append(team.getColoredName());

        manager.getServer().getPlayerList().broadcastSystemMessage(announcement, false);

        return 1;
    }

    /**
     * Helper method to join team via literal subcommand (/team nato, /team russia, /team spectator).
     * Executes the same logic as joinTeam but with pre-resolved Team enum.
     */
    private static int joinTeamLiteral(CommandContext<CommandSourceStack> context, TeamManager manager, Team team) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        Team currentTeam = manager.getOrCreatePlayerData(player.getUUID()).getTeam();

        if (currentTeam == team) {
            player.sendSystemMessage(Component.literal("You are already in team ")
                .append(team.getColoredName()));
            return 0;
        }

        manager.setPlayerTeam(player, team);
        // Broadcast handled by setPlayerTeam, which calls info log and broadcastSystemMessage
        return 1;
    }

    private static int showCurrentTeam(CommandContext<CommandSourceStack> context, TeamManager manager) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        PlayerCombatData data = manager.getOrCreatePlayerData(player.getUUID());

        player.sendSystemMessage(Component.literal("Your team: ")
            .append(data.getTeam().getColoredName())
            .withStyle(ChatFormatting.GOLD));

        player.sendSystemMessage(Component.literal(String.format("Stats: %d kills, %d deaths, %.2f K/D",
            data.getKills(), data.getDeaths(), data.getKDRatio()))
            .withStyle(ChatFormatting.GRAY));

        return 1;
    }

    private static int showStats(CommandContext<CommandSourceStack> context, TeamManager manager) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        PlayerCombatData data = manager.getOrCreatePlayerData(player.getUUID());

        player.sendSystemMessage(Component.literal("=== Your Combat Stats ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        player.sendSystemMessage(Component.literal("Team: ")
            .withStyle(ChatFormatting.GRAY)
            .append(data.getTeam().getColoredName()));

        player.sendSystemMessage(Component.literal(String.format("Kills: %d", data.getKills()))
            .withStyle(ChatFormatting.GREEN));

        player.sendSystemMessage(Component.literal(String.format("Deaths: %d", data.getDeaths()))
            .withStyle(ChatFormatting.RED));

        player.sendSystemMessage(Component.literal(String.format("K/D Ratio: %.2f", data.getKDRatio()))
            .withStyle(ChatFormatting.AQUA));

        return 1;
    }

    private static int showBalance(CommandContext<CommandSourceStack> context, TeamManager manager) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        int natoCount = countPlayersInTeam(manager, Team.NATO);
        int russiaCount = countPlayersInTeam(manager, Team.RUSSIA);
        int spectatorCount = countPlayersInTeam(manager, Team.SPECTATOR);

        player.sendSystemMessage(Component.literal("=== Team Balance ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        player.sendSystemMessage(Team.NATO.getColoredName()
            .append(Component.literal(": " + natoCount + " players")));

        player.sendSystemMessage(Team.RUSSIA.getColoredName()
            .append(Component.literal(": " + russiaCount + " players")));

        player.sendSystemMessage(Team.SPECTATOR.getColoredName()
            .append(Component.literal(": " + spectatorCount + " players")));

        int balance = Math.abs(natoCount - russiaCount);
        if (balance > 0) {
            player.sendSystemMessage(Component.literal(String.format("Imbalance: %d players", balance))
                .withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.literal("Teams are balanced!")
                .withStyle(ChatFormatting.GREEN));
        }

        return 1;
    }

    private static int resetStats(CommandContext<CommandSourceStack> context, TeamManager manager) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        manager.getOrCreatePlayerData(player.getUUID()).resetStats();
        player.sendSystemMessage(Component.literal("Your stats have been reset!")
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int countPlayersInTeam(TeamManager manager, Team team) {
        return (int) manager.getServer().getPlayerList().getPlayers().stream()
            .filter(p -> manager.getOrCreatePlayerData(p.getUUID()).getTeam() == team)
            .count();
    }
}
