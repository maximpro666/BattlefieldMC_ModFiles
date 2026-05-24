package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.punishment.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class TempbanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tempban")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.hasPermission(p, PunishmentType.TEMP_BAN);
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("duration_hours", IntegerArgumentType.integer(1, 8760))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            int hours = IntegerArgumentType.getInteger(ctx, "duration_hours");
                            String reason = StringArgumentType.getString(ctx, "reason");
                            ServerPlayer punisher = ctx.getSource().getPlayer();

                            if (target == punisher) {
                                ctx.getSource().sendFailure(Component.literal("§cНельзя забанить самого себя"));
                                return 0;
                            }

                            UUID punisherUuid = punisher != null ? punisher.getUUID() : UUID.randomUUID();
                            long seconds = hours * 3600L;
                            int id = PunishmentManager.issuePunishment(target.getUUID(),
                                punisherUuid, PunishmentType.TEMP_BAN, WarnCategory.GENERAL, reason, seconds);

                            target.connection.disconnect(Component.literal(
                                "§cВы временно забанены.\n§7Срок: " + hours + "ч\n§7Причина: " + reason));

                            ctx.getSource().sendSuccess(() -> Component.literal("§aИгрок " + target.getName().getString() + " забанен на " + hours + "ч\n§7Причина: " + reason), true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("§cИспользование: /tempban <игрок> <часы> <причина>"));
                    return 0;
                })));
    }
}
