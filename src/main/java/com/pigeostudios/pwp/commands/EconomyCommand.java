package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
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
            source.sendFailure(Component.translatable("pwp.chat.error.player_only"));
            return 0;
        }

        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        int bc = runtime.getBC(player.getUUID());
        int wc = runtime.getWC(player.getUUID());

        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.header"), false);
        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.balance_bc", bc), false);
        source.sendSuccess(() -> Component.translatable("pwp.chat.economy.balance_wc", wc), false);
        return 1;
    }
}
