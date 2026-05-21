package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.PWP;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KitSelectCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
            .then(Commands.literal("select")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> selectKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("claim")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> claimKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))));
    }

    private static int selectKit(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("pwp.chat.error.player_only"));
            return 0;
        }
        var kit = PWP.getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.selected", kit.getDisplayName(), name), false);
        return 1;
    }

    private static int claimKit(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("pwp.chat.error.player_only"));
            return 0;
        }
        Component result = PWP.getKitManager().claimKit(player, name, PWP.getTeamManager());
        if (result != null) {
            source.sendFailure(result);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.obtained", name), false);
        return 1;
    }
}
