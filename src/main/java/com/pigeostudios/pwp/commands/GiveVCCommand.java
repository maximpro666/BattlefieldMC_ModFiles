package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.TeamManager;
import com.pigeostudios.pwp.service.EconomyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GiveVCCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("givevc")
            .requires(ctx -> ctx.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 5000))
                    .executes(ctx -> giveVC(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"),
                        IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    private static int giveVC(CommandSourceStack source, ServerPlayer target, int amount) {
        TeamManager tm = PWP.getTeamManager();
        if (tm == null) {
            source.sendFailure(Component.literal("§cTeam manager not available"));
            return 0;
        }

        var team = tm.getOrCreatePlayerData(target.getUUID()).getTeam();
        if (!team.isPlayable()) {
            source.sendFailure(Component.literal("§c" + target.getName().getString() + " is not on a playable team"));
            return 0;
        }

        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        if (eco == null) {
            source.sendFailure(Component.literal("§cEconomy service not available"));
            return 0;
        }

        int oldVc = eco.getVC(team);
        boolean success = eco.addVC(team, amount, true);
        if (!success) {
            source.sendFailure(Component.literal("§cFailed to add VC — match cap may be reached"));
            return 0;
        }

        int newVc = eco.getVC(team);
        int actualAdded = newVc - oldVc;

        source.sendSuccess(() -> Component.literal("§aAdded §e" + actualAdded + " VC §ato §f" + target.getName().getString() + "'s team §7(" + team.name() + ")"), true);
        target.sendSystemMessage(Component.literal("§aTeam received §e" + actualAdded + " VC §7(now " + newVc + " VC)"));
        return 1;
    }
}
