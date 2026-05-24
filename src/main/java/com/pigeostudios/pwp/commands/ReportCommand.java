package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.punishment.StaffManager;
import com.pigeostudios.pwp.punishment.PunishmentType;
import com.pigeostudios.pwp.report.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ReportCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("report")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (ReportType t : ReportType.values()) {
                            builder.suggest(t.name());
                        }
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("description", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String typeStr = StringArgumentType.getString(ctx, "type");
                            String description = StringArgumentType.getString(ctx, "description");
                            ServerPlayer reporter = ctx.getSource().getPlayer();

                            if (reporter == null) return 0;
                            if (reporter == target) {
                                ctx.getSource().sendFailure(Component.literal("§cНельзя отправить репорт на самого себя"));
                                return 0;
                            }

                            ReportType type;
                            try {
                                type = ReportType.valueOf(typeStr.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(Component.literal("§cНеизвестный тип нарушения"));
                                return 0;
                            }

                            int id = ReportManager.createReport(reporter.getUUID(), target.getUUID(),
                                target.getName().getString(), type, description);

                            ReportManager.addSystemMessage(id, "§eTicket #" + id + " created by §f" + reporter.getName().getString()
                                + "§e for §f" + target.getName().getString() + " §e(" + type.getDisplayName() + ")");

                            ReportManager.notifyStaff(ctx.getSource().getServer(), id,
                                reporter.getName().getString(), target.getName().getString(), type);

                            ctx.getSource().sendSuccess(() -> Component.literal("§aРепорт #" + id + " отправлен. Спасибо!"), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal("§cИспол: /report <игрок> <тип> <описание>"));
                    return 0;
                })));

        dispatcher.register(Commands.literal("reports")
            .executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
                    com.pigeostudios.pwp.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                        new com.pigeostudios.pwp.network.OpenReportScreenPacket());
                }
                return Command.SINGLE_SUCCESS;
            }));

        dispatcher.register(Commands.literal("report")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.isStaff(p.getUUID());
                return ctx.hasPermission(2);
            })
            .then(Commands.literal("view")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int id = IntegerArgumentType.getInteger(ctx, "id");
                        Report r = ReportManager.getReport(id);
                        if (r == null) {
                            ctx.getSource().sendFailure(Component.literal("§cРепорт не найден"));
                            return 0;
                        }
                        String assigned = r.getAssignedTo() != null
                            ? ctx.getSource().getServer().getPlayerList().getPlayer(r.getAssignedTo()) != null
                                ? ctx.getSource().getServer().getPlayerList().getPlayer(r.getAssignedTo()).getName().getString()
                                : r.getAssignedTo().toString().substring(0, 8) + "..."
                            : "—";
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6=== Report #" + r.getId() + " ===\n" +
                            "§7 Reporter: §f" + r.getReporterUuid() + "\n" +
                            "§7 Target: §f" + r.getTargetName() + " (" + r.getTargetUuid() + ")\n" +
                            "§7 Type: §f" + r.getType().getDisplayName() + "\n" +
                            "§7 Description: §f" + (r.getDescription().isEmpty() ? "—" : r.getDescription()) + "\n" +
                            "§7 Status: §f" + r.getStatus().getDisplayName() + "\n" +
                            "§7 Assigned to: §f" + assigned), false);
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(Commands.literal("claim")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int id = IntegerArgumentType.getInteger(ctx, "id");
                        ServerPlayer mod = ctx.getSource().getPlayer();
                        if (mod == null) return 0;
                        ReportManager.claimReport(id, mod.getUUID());
                        ctx.getSource().sendSuccess(() -> Component.literal("§aРепорт #" + id + " назначен на вас"), true);
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(Commands.literal("dismiss")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            String reason = StringArgumentType.getString(ctx, "reason");
                            ServerPlayer mod = ctx.getSource().getPlayer();
                            if (mod == null) return 0;
                            ReportManager.dismissReport(id, mod.getUUID(), reason);
                            ctx.getSource().sendSuccess(() -> Component.literal("§aРепорт #" + id + " отклонён"), true);
                            return Command.SINGLE_SUCCESS;
                        }))))
            .then(Commands.literal("punish")
                .requires(ctx -> {
                    if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.hasPermission(p, PunishmentType.KICK);
                    return ctx.hasPermission(2);
                })
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .then(Commands.argument("punishment_id", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int reportId = IntegerArgumentType.getInteger(ctx, "id");
                            int punishmentId = IntegerArgumentType.getInteger(ctx, "punishment_id");
                            ServerPlayer mod = ctx.getSource().getPlayer();
                            if (mod == null) return 0;
                            ReportManager.linkPunishment(reportId, punishmentId, mod.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("§aНаказание #" + punishmentId + " привязано к репорту #" + reportId), true);
                            return Command.SINGLE_SUCCESS;
                        })))));
    }
}
