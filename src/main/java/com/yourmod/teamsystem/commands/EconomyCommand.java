package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.EconomyManager;
import net.minecraft.ChatFormatting;
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

        EconomyManager econ = TeamSystem.getEconomyManager();
        if (econ == null) {
            source.sendFailure(Component.literal("Economy not available"));
            return 0;
        }

        int bc = econ.getBC(player.getUUID());
        int sp = econ.getSP(player.getUUID());

        source.sendSuccess(() -> Component.literal("§6=== Your Balance ==="), false);
        source.sendSuccess(() -> Component.literal("§eBattleCredits: §f" + bc + " BC"), false);
        source.sendSuccess(() -> Component.literal("§aScorePoints: §f" + sp + " SP"), false);
        return 1;
    }
}
