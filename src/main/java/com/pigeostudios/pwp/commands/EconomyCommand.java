package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import com.pigeostudios.pwp.service.EconomyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EconomyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
            .executes(ctx -> showBalance(ctx.getSource()))
        );
        dispatcher.register(Commands.literal("bal")
            .executes(ctx -> showBalance(ctx.getSource()))
        );
        dispatcher.register(Commands.literal("vc")
            .executes(ctx -> showVCBalance(ctx.getSource()))
        );
        dispatcher.register(Commands.literal("vcredits")
            .executes(ctx -> showVCBalance(ctx.getSource()))
        );
    }

    private static int showBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendSuccess(() -> Component.literal("§eBalance command is player-only. Use §a/givecoins <player> <amount>"), false);
            return 1;
        }

        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        int bc = runtime.getBC(player.getUUID());
        int wc = runtime.getWC(player.getUUID());
        int vc = getTeamVC(player);

        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.header"), false);
        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.balance_bc", bc), false);
        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.balance_wc", wc), false);
        if (vc >= 0) {
            source.sendSuccess(() -> Component.translatable("pwp.chat.economy.balance_vc", vc), false);
        }
        return 1;
    }

    private static int showVCBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendSuccess(() -> Component.literal("§cOnly players can use this command"), false);
            return 1;
        }

        int vc = getTeamVC(player);
        if (vc < 0) {
            source.sendSuccess(() -> Component.literal("§cYou are not on a playable team"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.header_vc", vc), false);
        return 1;
    }

    private static int getTeamVC(ServerPlayer player) {
        TeamManager tm = PWP.getTeamManager();
        if (tm == null) return -1;
        Team team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        if (!team.isPlayable()) return -1;

        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        if (eco != null) {
            return eco.getVC(team);
        }
        return BattlefieldRuntime.getInstance().getVC(team);
    }
}
