package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.punishment.StaffManager;
import com.pigeostudios.pwp.report.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class TicketCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /ticket view <id> — open ticket screen (player + staff)
        dispatcher.register(Commands.literal("ticket")
            .then(Commands.literal("view")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int id = IntegerArgumentType.getInteger(ctx, "id");
                        if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
                            Report r = ReportManager.getReport(id);
                            if (r == null) {
                                ctx.getSource().sendFailure(Component.literal("\u00A7cTicket not found"));
                                return 0;
                            }
                            boolean isStaff = StaffManager.isStaff(p.getUUID());
                            boolean isOwner = r.getReporterUuid().equals(p.getUUID());
                            if (!isStaff && !isOwner) {
                                ctx.getSource().sendFailure(Component.literal("\u00A7cNot your ticket"));
                                return 0;
                            }
                            List<TicketMessage> msgs = ReportManager.getTicketMessages(id);
                            ctx.getSource().sendSuccess(() -> Component.literal("\u00A76=== Ticket #" + id + " ==="), false);
                            for (TicketMessage m : msgs) {
                                if (m.isSystem()) {
                                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A78\u00A7o" + m.getMessage()), false);
                                } else {
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                        "\u00A7f" + m.getSenderName() + "\u00A77: " + m.getMessage()), false);
                                }
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "\u00A77Use \u00A76/t reply " + id + " <msg> \u00A77to reply"), false);
                        }
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(Commands.literal("reply")
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            String msg = StringArgumentType.getString(ctx, "message");
                            if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
                                Report r = ReportManager.getReport(id);
                                if (r == null) {
                                    ctx.getSource().sendFailure(Component.literal("\u00A7cTicket not found"));
                                    return 0;
                                }
                                boolean isStaff = StaffManager.isStaff(p.getUUID());
                                boolean isOwner = r.getReporterUuid().equals(p.getUUID());
                                if (!isStaff && !isOwner) {
                                    ctx.getSource().sendFailure(Component.literal("\u00A7cNot your ticket"));
                                    return 0;
                                }
                                ReportManager.addTicketMessage(id, p.getUUID(), p.getName().getString(), msg);

                                // notify other party
                                ServerPlayer target = null;
                                if (isStaff && r.getReporterUuid() != null) {
                                    target = p.getServer().getPlayerList().getPlayer(r.getReporterUuid());
                                } else if (isOwner && r.getAssignedTo() != null) {
                                    target = p.getServer().getPlayerList().getPlayer(r.getAssignedTo());
                                }
                                if (target != null && target != p) {
                                    target.sendSystemMessage(Component.literal(
                                        "\u00A76[Ticket #" + id + "] " + p.getName().getString() + "\u00A77: " + msg));
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aReply sent"), false);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))))
            .then(Commands.literal("claim")
                .requires(ctx -> {
                    if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.isStaff(p.getUUID());
                    return ctx.hasPermission(2);
                })
                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int id = IntegerArgumentType.getInteger(ctx, "id");
                        if (ctx.getSource().getEntity() instanceof ServerPlayer p) {
                            ReportManager.claimReport(id, p.getUUID());
                            ReportManager.addSystemMessage(id, "\u00A7a" + p.getName().getString() + " claimed the ticket");
                            ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aTicket #" + id + " claimed"), true);
                        }
                        return Command.SINGLE_SUCCESS;
                    })))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00A76/ticket view <id>\u00A77 — view ticket\n" +
                    "\u00A76/t reply <id> <msg>\u00A77 — reply\n" +
                    "\u00A76/ticket claim <id>\u00A77 — claim (staff)"), false);
                return Command.SINGLE_SUCCESS;
            }));
    }
}
