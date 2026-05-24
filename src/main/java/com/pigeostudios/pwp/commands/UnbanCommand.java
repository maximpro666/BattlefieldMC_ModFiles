package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.punishment.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class UnbanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unban")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.hasPermission(p, PunishmentType.PERM_BAN);
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("uuid", StringArgumentType.word())
                .executes(ctx -> {
                    String uuidStr = StringArgumentType.getString(ctx, "uuid");
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        if (PunishmentManager.unban(uuid)) {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aБан снят для " + uuid), true);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("§cИгрок не найден в списке банов"));
                        }
                    } catch (IllegalArgumentException e) {
                        ctx.getSource().sendFailure(Component.literal("§cНекорректный UUID"));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal("§cИспользование: /unban <uuid>"));
                return 0;
            }));
    }
}
