package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pigeostudios.pwp.PWP;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DonatorCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("donator")
            .requires(ctx -> ctx.hasPermission(2))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("tier", IntegerArgumentType.integer(0, 3))
                        .executes(ctx -> setTier(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"),
                            IntegerArgumentType.getInteger(ctx, "tier"))))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> setTier(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"), 0))))
            .then(Commands.literal("check")
                .executes(ctx -> checkOwn(ctx.getSource()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(ctx -> ctx.hasPermission(2))
                    .executes(ctx -> checkOther(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int setTier(CommandSourceStack source, ServerPlayer target, int tier) {
        var tm = PWP.getTeamManager();
        if (tm == null) return 0;

        var pcd = tm.getOrCreatePlayerData(target.getUUID());
        pcd.setDonatTier(tier);
        tm.setDirty();

        String tierName = switch (tier) {
            case 1 -> "§7Donator I";
            case 2 -> "§eDonator II";
            case 3 -> "§6Donator III";
            default -> "§8None";
        };

        source.sendSuccess(() -> Component.literal("§aSet donator tier for §f" + target.getName().getString() + " §ato " + tierName), true);
        if (target != source.getEntity()) {
            target.sendSystemMessage(Component.literal("§aYour donator tier has been set to " + tierName));
        }
        return 1;
    }

    private static int checkOwn(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        return checkOther(source, player);
    }

    private static int checkOther(CommandSourceStack source, ServerPlayer target) {
        var tm = PWP.getTeamManager();
        if (tm == null) return 0;

        var pcd = tm.getOrCreatePlayerData(target.getUUID());
        int tier = pcd.getDonatTier();
        String tierName = switch (tier) {
            case 1 -> "§7Donator I";
            case 2 -> "§eDonator II";
            case 3 -> "§6Donator III";
            default -> "§8None";
        };

        source.sendSuccess(() -> Component.literal("§f" + target.getName().getString() + "§7: " + tierName), false);
        return 1;
    }
}
