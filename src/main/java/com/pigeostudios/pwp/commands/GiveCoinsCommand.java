package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.BattlefieldRuntime;
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
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> giveCoins(ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"),
                        IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    private static int giveCoins(CommandSourceStack source, ServerPlayer target, int amount) {
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        runtime.setBC(target.getUUID(), amount);
        runtime.addWC(target.getUUID(), amount);
        var tm = PWP.getTeamManager();
        if (tm != null) {
            var pcd = tm.getOrCreatePlayerData(target.getUUID());
            pcd.setBattleCredits(amount);
            pcd.setWarCredits(runtime.getWC(target.getUUID()));
            tm.setDirty();
        }
        runtime.syncAll(target);
        source.sendSuccess(() -> Component.literal("§aSet §e" + amount + " BC §7and §e" + amount + " WC §afor §f" + target.getName().getString()), true);
        target.sendSystemMessage(Component.literal("§aYour balance set to §e" + amount + " BC §7and §e" + amount + " WC"));
        return 1;
    }
}
