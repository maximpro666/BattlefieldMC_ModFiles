package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.network.NotificationPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class AdminNotifyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("anotify")
            .requires(ctx -> ctx.hasPermission(2))
            .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String text = StringArgumentType.getString(ctx, "text");
                    NotificationPacket pkt = new NotificationPacket(text, "admin", 5000, "");
                    for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
                    }
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Notification sent"), true);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
