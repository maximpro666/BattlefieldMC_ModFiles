package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.yourmod.teamsystem.network.OpenAdminPanelPacket;
import com.yourmod.teamsystem.network.PacketHandler;
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
