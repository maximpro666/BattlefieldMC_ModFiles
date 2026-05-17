package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.MarkerData;
import com.yourmod.teamsystem.core.MarkerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class MarkerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("marker")
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> addMarker(ctx.getSource(), StringArgumentType.getString(ctx, "name"), null))
                        .then(Commands.argument("label", StringArgumentType.greedyString())
                            .executes(ctx -> addMarker(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                                StringArgumentType.getString(ctx, "label")))
                        )
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            MarkerManager mm = TeamSystem.getMarkerManager();
                            if (mm.removeMarker(name)) {
                                ctx.getSource().sendSuccess(() ->
                                    Component.literal("Marker removed: " + name).withStyle(ChatFormatting.GREEN), true);
                            } else {
                                ctx.getSource().sendFailure(
                                    Component.literal("Marker not found: " + name));
                            }
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(ctx -> listMarkers(ctx.getSource()))
                )
                .then(Commands.literal("clear")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        TeamSystem.getMarkerManager().clearMarkers();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("All markers cleared").withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    })
                )
        );
    }

    private static int addMarker(CommandSourceStack source, String name, String label) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Players only"));
            return 0;
        }

        UUID uuid = player.getUUID();
        int teamOrdinal = TeamSystem.getTeamManager().getOrCreatePlayerData(uuid).getTeam().ordinal();
        String displayLabel = label != null ? label : name;

        MarkerManager mm = TeamSystem.getMarkerManager();
        mm.addMarker(name, displayLabel, player.level().dimension().location(),
            player.getX(), player.getY(), player.getZ(),
            teamOrdinal, MarkerData.MarkerType.POINT, uuid);

        source.sendSuccess(() ->
            Component.literal("Marker '" + displayLabel + "' added at your position").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int listMarkers(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        MarkerManager mm = TeamSystem.getMarkerManager();
        List<MarkerData> visible = mm.getMarkersForPlayer(player);

        if (visible.isEmpty()) {
            source.sendSuccess(() ->
                Component.literal("No markers visible").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(() ->
            Component.literal("--- Markers ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (MarkerData m : visible) {
            source.sendSuccess(() ->
                Component.literal(" - " + m.getName() + " (" + m.getLabel() + ") @ "
                    + String.format("%.0f, %.0f, %.0f", m.getX(), m.getY(), m.getZ())
                    + " [" + m.getType().name() + "]")
                    .withStyle(ChatFormatting.WHITE), false);
        }
        return 1;
    }
}
