package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
import com.pigeostudios.pwp.service.EconomyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class GiveCoinsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("givecoins")
            .requires(ctx -> ctx.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                    .executes(ctx -> giveCoins(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"),
                        IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    private static int giveCoins(CommandSourceStack source, ServerPlayer target, int amount) {
        // H5 fix: addBC instead of setBC
        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        if (eco != null) {
            eco.addBC(target.getUUID(), amount);
            eco.addWC(target.getUUID(), amount);
            eco.syncAll(target);
        } else {
            BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
            runtime.addBC(target.getUUID(), amount);
            runtime.addWC(target.getUUID(), amount);
            runtime.syncAll(target);
        }
        var tm = PWP.getTeamManager();
        if (tm != null) {
            var pcd = tm.getOrCreatePlayerData(target.getUUID());
            pcd.setBattleCredits(eco != null ? eco.getBC(target.getUUID()) : BattlefieldRuntime.getInstance().getBC(target.getUUID()));
            pcd.setWarCredits(eco != null ? eco.getWC(target.getUUID()) : BattlefieldRuntime.getInstance().getWC(target.getUUID()));
            tm.setDirty();
        }
        source.sendSuccess(() -> Component.literal("§aAdded §e" + amount + " BC §7and §e" + amount + " WC §afor §f" + target.getName().getString()), true);
        target.sendSystemMessage(Component.literal("§aReceived §e" + amount + " BC §7and §e" + amount + " WC"));
        return 1;
    }
}
