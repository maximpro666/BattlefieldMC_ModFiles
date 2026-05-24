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

public class WarnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (WarnCategory cat : WarnCategory.values()) {
            String cmdName = "warn" + cat.name().toLowerCase();
            dispatcher.register(Commands.literal(cmdName)
                .requires(ctx -> {
                    if (ctx.getEntity() instanceof ServerPlayer p) {
                        return StaffManager.hasPermission(p, PunishmentType.WARN);
                    }
                    return ctx.hasPermission(2);
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String reason = StringArgumentType.getString(ctx, "reason");
                            ServerPlayer punisher = ctx.getSource().getPlayer();

                            if (punisher == null) return 0;
                            if (target == punisher) {
                                ctx.getSource().sendFailure(Component.literal("§cНельзя выдать варн самому себе"));
                                return 0;
                            }

                            int id = PunishmentManager.issuePunishment(
                                target.getUUID(), punisher.getUUID(),
                                PunishmentType.WARN, cat, reason, 0);

                            AutoEscalationManager.onWarn(target.getUUID(), cat, punisher.getUUID());

                            String catName = cat.getDisplayName();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aВыдан варн (" + catName + ") игроку " + target.getName().getString() + "\n§7Причина: " + reason), true);
                            target.sendSystemMessage(Component.literal("§cВы получили предупреждение (" + catName + ")\n§7Причина: " + reason));
                            return Command.SINGLE_SUCCESS;
                        })))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("§cИспользование: /" + cmdName + " <игрок> <причина>"));
                    return 0;
                }));
        }
    }
}
