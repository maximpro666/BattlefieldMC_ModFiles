package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.PlayerCombatData;
import com.pigeostudios.pwp.events.PlayerEventHandler;
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
                            player.sendSystemMessage(Component.translatable("pwp.chat.callsign.empty"));
                            return 0;
                        }

                        if (name.length() > 32) {
                            player.sendSystemMessage(Component.translatable("pwp.chat.callsign.too_long"));
                            return 0;
                        }

                        PlayerCombatData pcd = PWP.getTeamManager().getOrCreatePlayerData(player.getUUID());
                        pcd.setCallsign(name);
                        PWP.getTeamManager().setPlayerDisplayName(player, name);
                        PlayerEventHandler handler = PWP.getPlayerEventHandler();
                        if (handler != null) {
                            handler.setDogTagName(player, name);
                        }

                        player.sendSystemMessage(Component.translatable("pwp.chat.callsign.set", name));
                        PWP.LOGGER.info("Player {} set callsign to {}", player.getName().getString(), name);
                        return 1;
                    })
                )
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer player = source.getPlayerOrException();
                    String current = PWP.getTeamManager()
                        .getOrCreatePlayerData(player.getUUID()).getDisplayName();
                    if (current.isEmpty()) {
                        player.sendSystemMessage(Component.translatable("pwp.chat.callsign.not_set"));
                    } else {
                        player.sendSystemMessage(Component.translatable("pwp.chat.callsign.current", current));
                    }
                    return 1;
                })
        );
    }
}
