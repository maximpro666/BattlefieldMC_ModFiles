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

public class VoicemuteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voicemute")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.hasPermission(p, PunishmentType.VOICE_MUTE);
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("duration_hours", IntegerArgumentType.integer(1, 720))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            int hours = IntegerArgumentType.getInteger(ctx, "duration_hours");
                            String reason = StringArgumentType.getString(ctx, "reason");
                            ServerPlayer punisher = ctx.getSource().getPlayer();

                            if (target == punisher) {
                                ctx.getSource().sendFailure(Component.literal("§cНельзя заvoicemute самого себя"));
                                return 0;
                            }

                            UUID punisherUuid = punisher != null ? punisher.getUUID() : UUID.randomUUID();
                            long seconds = hours * 3600L;
                            int id = PunishmentManager.issuePunishment(target.getUUID(),
                                punisherUuid, PunishmentType.VOICE_MUTE, WarnCategory.VOICE, reason, seconds);

                            ctx.getSource().sendSuccess(() -> Component.literal("§aГолосовой чат игрока " + target.getName().getString() + " заглушен на " + hours + "ч\n§7Причина: " + reason), true);
                            target.sendSystemMessage(Component.literal("§cВаш голосовой чат заблокирован на " + hours + "ч\n§7Причина: " + reason));
                            return Command.SINGLE_SUCCESS;
                        })))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("§cИспользование: /voicemute <игрок> <часы> <причина>"));
                    return 0;
                })));
    }
}
