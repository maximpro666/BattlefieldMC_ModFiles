package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.punishment.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PunishmentHistoryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("punishments")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.isStaff(p.getUUID());
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    List<Punishment> history = PunishmentManager.getPunishmentHistory(target.getUUID(), 20);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm");

                    ctx.getSource().sendSuccess(() -> Component.literal("§6=== История наказаний: " + target.getName().getString() + " ==="), false);

                    if (history.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§7Нет наказаний"), false);
                    } else {
                        for (Punishment p : history) {
                            String date = sdf.format(new Date(p.getIssuedAt() * 1000));
                            String type = p.getType().name();
                            String cat = p.getCategory() != null ? " (" + p.getCategory().getDisplayName() + ")" : "";
                            String status = p.isActive() ? "§c[A]§r" : "§7[E]§r";
                            String dur = p.getType().isPermanent() ? "perm" : formatDuration(p.getDurationSeconds());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "§7#" + p.getId() + " " + status + " §f" + type + cat
                                + " §7| " + dur + " | " + date
                                + "\n§7  Причина: " + p.getReason()), false);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal("§cИспользование: /punishments <игрок>"));
                return 0;
            }));
    }

    private static String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (mins > 0) sb.append(mins).append("м");
        return sb.toString().trim();
    }
}
