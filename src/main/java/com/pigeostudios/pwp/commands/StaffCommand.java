package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.punishment.StaffManager;
import com.pigeostudios.pwp.punishment.StaffRole;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;

public class StaffCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("staff")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) {
                    StaffRole role = StaffManager.getRole(p.getUUID());
                    return role == StaffRole.OWNER;
                }
                return ctx.hasPermission(4);
            })
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("role", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (StaffRole r : StaffRole.values()) {
                                builder.suggest(r.name());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String roleStr = StringArgumentType.getString(ctx, "role");
                            ServerPlayer punisher = ctx.getSource().getPlayer();

                            StaffRole role;
                            try {
                                role = StaffRole.valueOf(roleStr.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                ctx.getSource().sendFailure(Component.literal("§cНеизвестная роль. Доступно: MOD, ADMIN, OWNER"));
                                return 0;
                            }

                            if (punisher == null) return 0;
                            if (role.ordinal() >= StaffManager.getRole(punisher.getUUID()).ordinal()) {
                                ctx.getSource().sendFailure(Component.literal("§cНельзя назначить роль выше своей"));
                                return 0;
                            }

                            if (StaffManager.setRole(target.getUUID(), role, punisher.getUUID())) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§aРоль " + role.getDisplayName() + " назначена " + target.getName().getString()), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("§cУ игрока уже эта роль"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        ServerPlayer punisher = ctx.getSource().getPlayer();

                        StaffRole targetRole = StaffManager.getRole(target.getUUID());
                        if (targetRole == null) {
                            ctx.getSource().sendFailure(Component.literal("§cИгрок не является staff"));
                            return 0;
                        }

                        if (punisher != null && targetRole.ordinal() >= StaffManager.getRole(punisher.getUUID()).ordinal()) {
                            ctx.getSource().sendFailure(Component.literal("§cНельзя снять роль выше своей"));
                            return 0;
                        }

                        if (StaffManager.removeRole(target.getUUID())) {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aРоль снята с " + target.getName().getString()), true);
                        }
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    Map<UUID, StaffRole> allStaff = StaffManager.getAllStaff();
                    if (allStaff.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§7Нет staff-игроков"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> Component.literal("§6=== Staff List ==="), false);
                        for (Map.Entry<UUID, StaffRole> e : allStaff.entrySet()) {
                            String name = ctx.getSource().getServer().getPlayerList().getPlayer(e.getKey()) != null
                                ? ctx.getSource().getServer().getPlayerList().getPlayer(e.getKey()).getName().getString()
                                : e.getKey().toString().substring(0, 8) + "...";
                            ctx.getSource().sendSuccess(() -> Component.literal(" §7- " + name + " §8[" + e.getValue().getDisplayName() + "§8]"), false);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal("§cИспользование: /staff add/remove/list"));
                return 0;
            }));
    }
}
