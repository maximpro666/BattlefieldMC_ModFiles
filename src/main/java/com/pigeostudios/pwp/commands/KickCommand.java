package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.punishment.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class KickCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kick")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.hasPermission(p, PunishmentType.KICK);
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        String reason = StringArgumentType.getString(ctx, "reason");
                        ServerPlayer punisher = ctx.getSource().getPlayer();

                        if (target == punisher) {
                            ctx.getSource().sendFailure(Component.literal("§cНельзя кикнуть самого себя"));
                            return 0;
                        }

                        UUID punisherUuid = punisher != null ? punisher.getUUID() : UUID.randomUUID();
                        PunishmentManager.issuePunishment(target.getUUID(),
                            punisherUuid, PunishmentType.KICK, null, reason, 0);

                        if (target.connection != null) target.connection.disconnect(Component.literal("§cВы были кикнуты\n§7Причина: " + reason));
                        ctx.getSource().sendSuccess(() -> Component.literal("§aИгрок " + target.getName().getString() + " кикнут\n§7Причина: " + reason), true);
                        return Command.SINGLE_SUCCESS;
                    })))
            .executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal("§cИспользование: /kick <игрок> <причина>"));
                return 0;
            }));
    }
}
