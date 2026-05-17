package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class RankCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rank")
            .executes(ctx -> viewOwnRank(ctx.getSource()))
            .then(Commands.literal("list")
                .executes(ctx -> listRanks(ctx.getSource())))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> viewRank(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
            .then(Commands.literal("set")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("rank", StringArgumentType.word())
                        .executes(ctx -> setRank(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "rank")))))));
    }

    private static int viewOwnRank(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        TeamManager tm = TeamSystem.getTeamManager();
        int rankOrdinal = tm.getPlayerRank(player.getUUID());
        Rank rank = Rank.fromOrdinal(rankOrdinal);
        int kills = tm.getOrCreatePlayerData(player.getUUID()).getKills();

        source.sendSuccess(() -> Component.literal(
            String.format("§6Your rank: §b%s §7(%d kills)", rank.getDisplayName(), kills)), false);

        Rank next = rankOrdinal < Rank.values().length - 1 ? Rank.values()[rankOrdinal + 1] : null;
        if (next != null) {
            source.sendSuccess(() -> Component.literal(
                String.format("§7Next: §b%s §7at §e%d §7kills", next.getDisplayName(), next.getKillRequirement())), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7You are at the highest rank!").withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
        return 1;
    }

    private static int listRanks(CommandSourceStack source) {
        ServerPlayer player = source.getEntity() instanceof ServerPlayer ? (ServerPlayer) source.getEntity() : null;
        TeamManager tm = TeamSystem.getTeamManager();
        int playerRank = player != null ? tm.getPlayerRank(player.getUUID()) : -1;

        source.sendSuccess(() -> Component.literal("§6=== Ranks ==="), false);
        StringBuilder sb = new StringBuilder("§6=== Ranks ===\n");
        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            Rank r = ranks[i];
            String arrow = i == playerRank ? "§b▶ " : "  ";
            String rankup = i < ranks.length - 1
                ? " §7→ §b" + ranks[i + 1].getDisplayName() + " §7(" + ranks[i + 1].getKillRequirement() + " kills)"
                : " §7[MAX]";
            sb.append(String.format("%s%s §7- §f%s%s\n", arrow, r.getDisplayName(), r.getPrefix(), rankup));
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int viewRank(CommandSourceStack source, ServerPlayer player) {
        TeamManager tm = TeamSystem.getTeamManager();
        int rankOrdinal = tm.getPlayerRank(player.getUUID());
        Rank rank = Rank.fromOrdinal(rankOrdinal);
        int kills = tm.getOrCreatePlayerData(player.getUUID()).getKills();

        source.sendSuccess(() -> Component.literal(
            String.format("§6%s is §b%s §6(%d kills)",
                player.getName().getString(), rank.getDisplayName(), kills)), false);
        return 1;
    }

    private static int setRank(CommandSourceStack source, ServerPlayer player, String rankName) {
        try {
            int rankOrdinal = Integer.parseInt(rankName);
            Rank rank = Rank.fromOrdinal(rankOrdinal);

            TeamManager tm = TeamSystem.getTeamManager();
            tm.setPlayerRank(player, rankOrdinal);

            source.sendSuccess(() -> Component.literal(
                String.format("§6Set %s to rank §b%s",
                    player.getName().getString(), rank.getDisplayName())), true);
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§cInvalid rank. Use ordinal 0-9"));
            return 0;
        }
    }
}
