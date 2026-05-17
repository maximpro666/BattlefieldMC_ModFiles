package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.RankManager;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RankCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rank")
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
