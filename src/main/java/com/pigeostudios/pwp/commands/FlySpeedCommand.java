package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FlySpeedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("flyspeed")
            .requires(ctx -> ctx.hasPermission(2))
            .then(Commands.argument("speed", FloatArgumentType.floatArg(0.0f, 1.0f))
                .executes(ctx -> {
                    float speed = FloatArgumentType.getFloat(ctx, "speed");
                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                        player.getAbilities().setFlyingSpeed(speed);
                        player.onUpdateAbilities();
                        player.sendSystemMessage(Component.literal("Скорость полёта изменена на " + speed));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    float current = player.getAbilities().getFlyingSpeed();
                    player.sendSystemMessage(Component.literal("Текущая скорость полёта: " + current));
                }
                return Command.SINGLE_SUCCESS;
            }));
    }
}
