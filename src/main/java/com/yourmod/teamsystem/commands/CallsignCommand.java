package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.events.PlayerEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CallsignCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("callsign")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer player = source.getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name").trim();

                        if (name.isEmpty()) {
                            player.sendSystemMessage(Component.literal("Позывной не может быть пустым")
                                .withStyle(ChatFormatting.RED));
                            return 0;
                        }

                        if (name.length() > 32) {
                            player.sendSystemMessage(Component.literal("Позывной слишком длинный (максимум 32 символа)")
                                .withStyle(ChatFormatting.RED));
                            return 0;
                        }

                        TeamSystem.getTeamManager().setPlayerDisplayName(player, name);
                        PlayerEventHandler handler = TeamSystem.getPlayerEventHandler();
                        if (handler != null) {
                            handler.setDogTagName(player, name);
                        }

                        player.sendSystemMessage(Component.literal("Позывной установлен: " + name)
                            .withStyle(ChatFormatting.GREEN));
                        TeamSystem.LOGGER.info("Player {} set callsign to {}", player.getName().getString(), name);
                        return 1;
                    })
                )
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer player = source.getPlayerOrException();
                    String current = TeamSystem.getTeamManager()
                        .getOrCreatePlayerData(player.getUUID()).getDisplayName();
                    if (current.isEmpty()) {
                        player.sendSystemMessage(Component.literal("У вас ещё нет позывного. Используйте /callsign <имя>")
                            .withStyle(ChatFormatting.YELLOW));
                    } else {
                        player.sendSystemMessage(Component.literal("Ваш позывной: " + current)
                            .withStyle(ChatFormatting.YELLOW));
                    }
                    return 1;
                })
        );
    }
}
