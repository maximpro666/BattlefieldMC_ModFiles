package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.network.OpenAdminPanelPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class AdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("admin")
            .requires(ctx -> ctx.hasPermission(2))
            .executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    PacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new OpenAdminPanelPacket());
                }
                return Command.SINGLE_SUCCESS;
            }));
    }
}
