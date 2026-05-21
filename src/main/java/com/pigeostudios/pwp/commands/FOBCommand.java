package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.FOBManager;
import com.pigeostudios.pwp.core.FOBManager.SavedFOB;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class FOBCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fob")
            .then(Commands.literal("place")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> placeFOB(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> removeFOB(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list")
                .executes(ctx -> listFOBs(ctx.getSource()))));
    }

    private static int placeFOB(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        String result = PWP.getFOBManager().placeFOB(player, name);
        if (result != null) {
            source.sendFailure(Component.literal(result));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aFOB '" + name + "' placed!"), false);
        return 1;
    }

    private static int removeFOB(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        boolean removed = PWP.getFOBManager().removeFOBByName(player, name);
        if (!removed) {
            source.sendFailure(Component.literal("§cFOB '" + name + "' not found"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aFOB '" + name + "' removed!"), false);
        return 1;
    }

    private static int listFOBs(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        java.util.Optional<com.pigeostudios.pwp.core.Team> teamOpt =
            java.util.Optional.ofNullable(PWP.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam());
        if (teamOpt.isEmpty() || !teamOpt.get().isPlayable()) {
            source.sendFailure(Component.literal("§cYou are not on a team"));
            return 0;
        }
        List<SavedFOB> fobs = PWP.getFOBManager().getFOBsForTeam(teamOpt.get());
        if (fobs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No FOBs placed"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("§6=== Your Team's FOBs ==="), false);
        for (SavedFOB fob : fobs) {
            source.sendSuccess(() -> Component.literal(
                String.format(" §b%s §7- %d,%d,%d §7[HP: %.0f/200]",
                    fob.name, fob.x, fob.y, fob.z, fob.health)), false);
        }
        return 1;
    }
}
