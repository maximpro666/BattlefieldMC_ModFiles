package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.punishment.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class UnmuteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unmute")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.hasPermission(p, PunishmentType.MUTE);
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    if (PunishmentManager.unmute(target.getUUID())) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§aМут снят с " + target.getName().getString()), true);
                        target.sendSystemMessage(Component.literal("§aВаш мут снят."));
                    } else {
                        ctx.getSource().sendFailure(Component.literal("§cИгрок не заглушен"));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal("§cИспользование: /unmute <игрок>"));
                return 0;
            }));
    }
}
