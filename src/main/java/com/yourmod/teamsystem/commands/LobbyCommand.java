package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LobbyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("lobby")
                .requires(source -> source.hasPermission(0))
                .executes(context -> teleportToLobby(context))
                .then(Commands.literal("setspawn")
                    .requires(source -> source.hasPermission(0))
                    .executes(context -> setLobbyRespawn(context))
                )
        );
    }

    private static int teleportToLobby(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();

        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        game.teleportPlayerToLobby(player);
        game.setLobbyRespawn(player);
        player.sendSystemMessage(Component.literal("Teleported to lobby! Respawn point set.")
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }

    private static int setLobbyRespawn(CommandContext<CommandSourceStack> context) {
        GameManager game = TeamSystem.getGameManager();

        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        game.setLobbyRespawnAtPlayer(player);
        player.sendSystemMessage(Component.literal("Respawn point set to your current location!")
            .withStyle(ChatFormatting.GREEN));

        return 1;
    }
}
