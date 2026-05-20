package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import com.yourmod.teamsystem.network.OpenLoadoutScreenPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import static com.yourmod.teamsystem.core.ChatHelper.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

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
            player.sendSystemMessage(error("Invalid team! Available teams: NATO, RUSSIA, SPECTATOR"));
            return 0;
        }

        Team currentTeam = manager.getOrCreatePlayerData(player.getUUID()).getTeam();

        if (currentTeam == team) {
            player.sendSystemMessage(bright("You are already in team ")
                .append(team.getColoredName()));
            return 0;
        }

        TeamSystemConfig cfg = TeamSystem.getConfig();
        if (team.isPlayable() && cfg != null && cfg.isTeamBalancing()) {
            int maxDiff = cfg.getMaxTeamDifference();
            int natoCount = manager.getPlayersByTeam(Team.NATO).size();
            int russiaCount = manager.getPlayersByTeam(Team.RUSSIA).size();
            int targetCount = team == Team.NATO ? natoCount : russiaCount;
            int otherCount = team == Team.NATO ? russiaCount : natoCount;
            if (targetCount >= otherCount + maxDiff + 1) {
                player.sendSystemMessage(error("Команда переполнена! Выберите другую сторону."));
                return 0;
            }
        }

        manager.setPlayerTeam(player, team);

        player.sendSystemMessage(success("You joined team ")
            .append(team.getColoredName()));

        Component announcement = warning(player.getName().getString())
            .append(bright(" joined team "))
            .append(team.getColoredName());

        manager.getServer().getPlayerList().broadcastSystemMessage(announcement, false);

        if (team.isPlayable()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenLoadoutScreenPacket());
        }

        return 1;
    }

    private static int showCurrentTeam(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        PlayerCombatData data = manager.getOrCreatePlayerData(player.getUUID());

        player.sendSystemMessage(accent("Your team: ")
            .append(data.getTeam().getColoredName()));

        player.sendSystemMessage(neutral(String.format("Stats: %d kills, %d deaths, %.2f K/D",
            data.getKills(), data.getDeaths(), data.getKDRatio())));

        return 1;
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        PlayerCombatData data = manager.getOrCreatePlayerData(player.getUUID());

        player.sendSystemMessage(header("=== Your Combat Stats ==="));

        player.sendSystemMessage(neutral("Team: ")
            .append(data.getTeam().getColoredName()));

        player.sendSystemMessage(success(String.format("Kills: %d", data.getKills())));

        player.sendSystemMessage(error(String.format("Deaths: %d", data.getDeaths())));

        player.sendSystemMessage(info(String.format("K/D Ratio: %.2f", data.getKDRatio())));

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

        player.sendSystemMessage(header("=== Team Balance ==="));

        player.sendSystemMessage(Team.NATO.getColoredName()
            .append(Component.literal(": " + natoCount + " players")));

        player.sendSystemMessage(Team.RUSSIA.getColoredName()
            .append(Component.literal(": " + russiaCount + " players")));

        player.sendSystemMessage(Team.SPECTATOR.getColoredName()
            .append(Component.literal(": " + spectatorCount + " players")));

        int balance = Math.abs(natoCount - russiaCount);
        if (balance > 0) {
            player.sendSystemMessage(warning(String.format("Imbalance: %d players", balance)));
        } else {
            player.sendSystemMessage(success("Teams are balanced!"));
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
        player.sendSystemMessage(success("Your stats have been reset!"));

        return 1;
    }

    private static int showTickets(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        TicketManager tm = TeamSystem.getTicketManager();
        if (tm == null) return 0;

        player.sendSystemMessage(header("=== Team Tickets ==="));

        player.sendSystemMessage(Team.NATO.getColoredName()
            .append(bright(String.format(": %d tickets", tm.getTickets(Team.NATO)))));

        player.sendSystemMessage(Team.RUSSIA.getColoredName()
            .append(bright(String.format(": %d tickets", tm.getTickets(Team.RUSSIA)))));

        return 1;
    }

    private static int setTickets(CommandContext<CommandSourceStack> context) {
        String teamName = StringArgumentType.getString(context, "team");
        Team team = Team.fromString(teamName);
        if (team == null || !team.isPlayable()) {
            context.getSource().sendFailure(error("Invalid team! Use NATO or RUSSIA."));
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(context, "amount");
        TicketManager tm = TeamSystem.getTicketManager();
        if (tm == null) return 0;
        tm.setTickets(team, amount);

        context.getSource().sendSuccess(() ->
            success(String.format("Set %s tickets to %d", team.getName(), amount)), true);

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

        player.sendSystemMessage(success(String.format("Prefix set to: %s", prefix)));

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

        player.sendSystemMessage(success(String.format("Suffix set to: %s", suffix)));

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

        player.sendSystemMessage(success(String.format("Display name set to: %s", name)));

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

        player.sendSystemMessage(success("Name customization cleared!"));

        return 1;
    }

    private static int modifySetPrefix(CommandContext<CommandSourceStack> context) {
        TeamManager manager = getManager();
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            String prefix = StringArgumentType.getString(context, "prefix");
            manager.setPlayerPrefix(target, prefix);
            context.getSource().sendSuccess(() ->
                success("Set prefix of " + target.getName().getString() + " to: " + prefix), true);
        } catch (Exception e) {
            context.getSource().sendFailure(error("Player not found"));
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
                success("Set suffix of " + target.getName().getString() + " to: " + suffix), true);
        } catch (Exception e) {
            context.getSource().sendFailure(error("Player not found"));
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
                success("Set display name of " + target.getName().getString() + " to: " + name), true);
        } catch (Exception e) {
            context.getSource().sendFailure(error("Player not found"));
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
                success("Reset name customization for " + target.getName().getString()), true);
        } catch (Exception e) {
            context.getSource().sendFailure(error("Player not found"));
        }
        return 1;
    }

    private static int countPlayersInTeam(TeamManager manager, Team team) {
        return (int) manager.getServer().getPlayerList().getPlayers().stream()
            .filter(p -> manager.getOrCreatePlayerData(p.getUUID()).getTeam() == team)
            .count();
    }
}
