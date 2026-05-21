package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.BattlefieldRuntime;
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
    }

    private static int showBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        int bc = runtime.getBC(player.getUUID());
        int wc = runtime.getWC(player.getUUID());

        source.sendSuccess(() -> Component.literal("§6=== Your Balance ==="), false);
        source.sendSuccess(() -> Component.literal("§eBattle Coins: §f" + bc + " BC §7(матчевые)"), false);
        source.sendSuccess(() -> Component.literal("§bWar Credits: §f" + wc + " WC §7(постоянные)"), false);
        return 1;
    }
}
