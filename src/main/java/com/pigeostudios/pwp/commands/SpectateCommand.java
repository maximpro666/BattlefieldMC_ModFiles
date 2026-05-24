package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.punishment.StaffManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SpectateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spectate")
            .requires(ctx -> {
                if (ctx.getEntity() instanceof ServerPlayer p) return StaffManager.isStaff(p.getUUID());
                return ctx.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    ServerPlayer mod = ctx.getSource().getPlayer();
                    if (mod == null) return 0;

                    mod.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(),
                        target.getYRot(), target.getXRot());
                    mod.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                    mod.sendSystemMessage(Component.literal("§7Вы наблюдаете за " + target.getName().getString()));
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal("§cИспользование: /spectate <игрок>"));
                return 0;
            }));
    }
}
