package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.yourmod.teamsystem.proxy.ProxyMessenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StartMatchCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("startmatch")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                ProxyMessenger.send("start_match");
                ctx.getSource().sendSuccess(() -> Component.literal("§a[start_match] sent to proxy"), true);
                return 1;
            })
        );
    }
}
