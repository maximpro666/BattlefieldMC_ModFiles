package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.PlayerCombatData;
import com.yourmod.teamsystem.events.PlayerEventHandler;
import static com.yourmod.teamsystem.core.ChatHelper.*;
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
                            player.sendSystemMessage(error("Позывной не может быть пустым"));
                            return 0;
                        }

                        if (name.length() > 32) {
                            player.sendSystemMessage(error("Позывной слишком длинный (максимум 32 символа)"));
                            return 0;
                        }

                        PlayerCombatData pcd = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID());
                        pcd.setCallsign(name);
                        TeamSystem.getTeamManager().setPlayerDisplayName(player, name);
                        PlayerEventHandler handler = TeamSystem.getPlayerEventHandler();
                        if (handler != null) {
                            handler.setDogTagName(player, name);
                        }

                        player.sendSystemMessage(success("Позывной установлен: " + name));
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
                        player.sendSystemMessage(warning("У вас ещё нет позывного. Используйте /callsign <имя>"));
                    } else {
                        player.sendSystemMessage(warning("Ваш позывной: " + current));
                    }
                    return 1;
                })
        );
    }
}
