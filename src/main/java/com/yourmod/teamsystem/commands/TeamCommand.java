package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.PlayerCombatData;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.core.TicketManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
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

    private static TeamManager getManager() {
        return TeamSystem.getTeamManager();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("team")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("teamname", StringArgumentType.word())
                    .suggests(TEAM_SUGGESTIONS)
                    .executes(context -> joinTeam(context))
                )
                .then(Commands.literal("modify")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.literal("setprefix")
                            .then(Commands.argument("prefix", StringArgumentType.greedyString())
                                .executes(context -> modifySetPrefix(context))
                            )
                        )
                        .then(Commands.literal("setsuffix")
                            .then(Commands.argument("suffix", StringArgumentType.greedyString())
                                .executes(context -> modifySetSuffix(context))
                            )
                        )
                        .then(Commands.literal("setdisplayname")
                            .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> modifySetDisplayName(context))
                            )
                        )
                        .then(Commands.literal("reset")
                            .executes(context -> modifyReset(context))
                        )
                    )
                )
                .executes(context -> showCurrentTeam(context))
        );

        dispatcher.register(
            Commands.literal("teamstats")
                .requires(source -> source.hasPermission(0))
                .executes(context -> showStats(context))
        );

        dispatcher.register(
            Commands.literal("teambalance")
                .requires(source -> source.hasPermission(0))
                .executes(context -> showBalance(context))
        );

        dispatcher.register(
            Commands.literal("resetstats")
                .requires(source -> source.hasPermission(2))
                .executes(context -> resetStats(context))
        );

        dispatcher.register(
            Commands.literal("teamtickets")
                .requires(source -> source.hasPermission(0))
                .executes(context -> showTickets(context))
        );

        dispatcher.register(
            Commands.literal("settickets")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("team", StringArgumentType.word())
                    .suggests(TEAM_SUGGESTIONS)
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> setTickets(context))
                    )
                )
        );

        dispatcher.register(
            Commands.literal("setprefix")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("prefix", StringArgumentType.greedyString())
                    .executes(context -> setPrefix(context))
                )
        );

        dispatcher.register(
            Commands.literal("setsuffix")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("suffix", StringArgumentType.greedyString())
                    .executes(context -> setSuffix(context))
                )
        );

        dispatcher.register(
            Commands.literal("setdisplayname")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> setDisplayName(context))
                )
        );

        dispatcher.register(
            Commands.literal("clearname")
                .requires(source -> source.hasPermission(0))
                .executes(context -> clearName(context))
        );
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
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

    private static int showCurrentTeam(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
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

    private static int showStats(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
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

    private static int showBalance(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
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

    private static int resetStats(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        manager.getOrCreatePlayerData(player.getUUID()).resetStats();
        player.sendSystemMessage(Component.literal("Your stats have been reset!")
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int showTickets(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        TicketManager tm = TeamSystem.getTicketManager();
        if (tm == null) return 0;

        player.sendSystemMessage(Component.literal("=== Team Tickets ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        player.sendSystemMessage(Team.NATO.getColoredName()
            .append(Component.literal(String.format(": %d tickets", tm.getTickets(Team.NATO)))
                .withStyle(ChatFormatting.WHITE)));

        player.sendSystemMessage(Team.RUSSIA.getColoredName()
            .append(Component.literal(String.format(": %d tickets", tm.getTickets(Team.RUSSIA)))
                .withStyle(ChatFormatting.WHITE)));

        return 1;
    }

    private static int setTickets(CommandContext<CommandSourceStack> context) {
        String teamName = StringArgumentType.getString(context, "team");
        Team team = Team.fromString(teamName);
        if (team == null || !team.isPlayable()) {
            context.getSource().sendFailure(Component.literal("Invalid team! Use NATO or RUSSIA.")
                .withStyle(ChatFormatting.RED));
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(context, "amount");
        TicketManager tm = TeamSystem.getTicketManager();
        if (tm == null) return 0;
        tm.setTickets(team, amount);

        context.getSource().sendSuccess(() ->
            Component.literal(String.format("Set %s tickets to %d", team.getName(), amount))
                .withStyle(ChatFormatting.GREEN), true);

        return 1;
    }

    private static int setPrefix(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        String prefix = StringArgumentType.getString(context, "prefix");
        manager.setPlayerPrefix(player, prefix);

        player.sendSystemMessage(Component.literal(String.format("Prefix set to: %s", prefix))
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int setSuffix(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        String suffix = StringArgumentType.getString(context, "suffix");
        manager.setPlayerSuffix(player, suffix);

        player.sendSystemMessage(Component.literal(String.format("Suffix set to: %s", suffix))
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int setDisplayName(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        String name = StringArgumentType.getString(context, "name");
        manager.setPlayerDisplayName(player, name);

        player.sendSystemMessage(Component.literal(String.format("Display name set to: %s", name))
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int clearName(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        manager.setPlayerPrefix(player, "");
        manager.setPlayerSuffix(player, "");
        manager.setPlayerDisplayName(player, "");

        player.sendSystemMessage(Component.literal("Name customization cleared!")
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int modifySetPrefix(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            String prefix = StringArgumentType.getString(context, "prefix");
            manager.setPlayerPrefix(target, prefix);
            context.getSource().sendSuccess(() ->
                Component.literal("Set prefix of " + target.getName().getString() + " to: " + prefix)
                    .withStyle(ChatFormatting.GREEN), true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found"));
        }
        return 1;
    }

    private static int modifySetSuffix(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            String suffix = StringArgumentType.getString(context, "suffix");
            manager.setPlayerSuffix(target, suffix);
            context.getSource().sendSuccess(() ->
                Component.literal("Set suffix of " + target.getName().getString() + " to: " + suffix)
                    .withStyle(ChatFormatting.GREEN), true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found"));
        }
        return 1;
    }

    private static int modifySetDisplayName(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            String name = StringArgumentType.getString(context, "name");
            manager.setPlayerDisplayName(target, name);
            context.getSource().sendSuccess(() ->
                Component.literal("Set display name of " + target.getName().getString() + " to: " + name)
                    .withStyle(ChatFormatting.GREEN), true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found"));
        }
        return 1;
    }

    private static int modifyReset(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            manager.setPlayerPrefix(target, "");
            manager.setPlayerSuffix(target, "");
            manager.setPlayerDisplayName(target, "");
            context.getSource().sendSuccess(() ->
                Component.literal("Reset name customization for " + target.getName().getString())
                    .withStyle(ChatFormatting.GREEN), true);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found"));
        }
        return 1;
    }

    private static int countPlayersInTeam(TeamManager manager, Team team) {
        return (int) manager.getServer().getPlayerList().getPlayers().stream()
            .filter(p -> manager.getOrCreatePlayerData(p.getUUID()).getTeam() == team)
            .count();
    }
}
