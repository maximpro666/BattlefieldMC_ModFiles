package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.EconomyManager;
import com.yourmod.teamsystem.core.PlayerCombatData;
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
        EconomyManager econ = TeamSystem.getEconomyManager();
        if (econ == null) {
            source.sendFailure(Component.literal("Economy not available"));
            return 0;
        }
        econ.setBC(target.getUUID(), amount);
        econ.setSP(target.getUUID(), amount);
        var tm = TeamSystem.getTeamManager();
        if (tm != null) {
            PlayerCombatData pcd = tm.getOrCreatePlayerData(target.getUUID());
            pcd.setBattleCredits(amount);
            tm.setDirty();
        }
        econ.syncAll(target);
        source.sendSuccess(() -> Component.literal("§aSet §e" + amount + " BC §7and §e" + amount + " SP §afor §f" + target.getName().getString()), true);
        target.sendSystemMessage(Component.literal("§aYour balance set to §e" + amount + " BC §7and §e" + amount + " SP"));
        return 1;
    }
}
