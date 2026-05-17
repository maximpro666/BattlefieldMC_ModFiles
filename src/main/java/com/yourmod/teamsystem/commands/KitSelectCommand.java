package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
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
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        var kit = TeamSystem.getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found: " + name));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aKit '" + kit.getDisplayName() + "' selected! Use /kit claim " + name + " to equip."), false);
        return 1;
    }

    private static int claimKit(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        String result = TeamSystem.getKitManager().claimKit(player, name, TeamSystem.getTeamManager());
        if (result != null) {
            source.sendFailure(Component.literal(result));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aKit '" + name + "' equipped!"), false);
        return 1;
    }
}
