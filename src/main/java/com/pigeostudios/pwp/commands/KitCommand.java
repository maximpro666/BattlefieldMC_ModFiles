package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.Kit;
import com.pigeostudios.pwp.core.KitManager;
import com.pigeostudios.pwp.core.MapConfig;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.core.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class KitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
            .then(Commands.literal("create")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> createKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("delete")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> deleteKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("edit")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> editKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("list")
                .executes(ctx -> listKits(ctx.getSource(), ctx.getSource().getPlayer(), null)))
            .then(Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> kitInfo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("setteam")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("team", StringArgumentType.word())
                        .executes(ctx -> setKitTeam(ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            StringArgumentType.getString(ctx, "team"))))))
            .then(Commands.literal("setrank")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("rank", IntegerArgumentType.integer(0, 9))
                        .executes(ctx -> setKitRank(ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            IntegerArgumentType.getInteger(ctx, "rank"))))))
            .then(Commands.literal("setcooldown")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                        .executes(ctx -> setKitCooldown(ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            IntegerArgumentType.getInteger(ctx, "seconds"))))))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> claimKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"))))
            .then(Commands.literal("give")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> giveKit(ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static int createKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        TeamManager tm = PWP.getTeamManager();
        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        Kit kit = Kit.fromPlayerInventory(name, name, playerTeam, 0, player);

        tm.getKitManager().getKits().put(name, kit);
        tm.getKitManager().saveKits();

        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.created", name), true);
        return 1;
    }

    private static int deleteKit(CommandSourceStack source, String name) {
        PWP.getTeamManager().getKitManager().deleteKit(name);
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.deleted", name), true);
        return 1;
    }

    private static int editKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        TeamManager tm = PWP.getTeamManager();
        Kit kit = tm.getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }

        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        Kit updated = Kit.fromPlayerInventory(name, kit.getDisplayName(), playerTeam, kit.getMinRankOrdinal(), player);
        tm.getKitManager().getKits().put(name, updated);
        tm.getKitManager().saveKits();

        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.updated", name), true);
        return 1;
    }

    private static int listKits(CommandSourceStack source, ServerPlayer player, Team team) {
        if (player == null) return 0;

        KitManager km = PWP.getTeamManager().getKitManager();
        List<Kit> available = km.getAvailableKits(player, PWP.getTeamManager());

        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.list_header"), false);
        for (Kit kit : available) {
            source.sendSuccess(() -> Component.translatable("pwp.chat.kit.list_entry",
                kit.getName(), kit.getDisplayName(), kit.getItems().size()), false);
        }
        return 1;
    }

    private static int kitInfo(CommandSourceStack source, String name) {
        Kit kit = PWP.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.info",
            kit.getDisplayName(), kit.getTeam().getName(), kit.getMinRankOrdinal(), kit.getItems().size()), false);
        return 1;
    }

    private static int setKitTeam(CommandSourceStack source, String name, String teamStr) {
        Kit kit = PWP.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }
        Team team = Team.fromString(teamStr);
        if (team == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.invalid_team", teamStr));
            return 0;
        }
        kit.setTeam(team);
        PWP.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.team_set", name, team.getName()), true);
        return 1;
    }

    private static int setKitRank(CommandSourceStack source, String name, int rank) {
        Kit kit = PWP.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }
        kit.setMinRankOrdinal(rank);
        PWP.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.rank_set", name, rank), true);
        return 1;
    }

    private static int setKitCooldown(CommandSourceStack source, String name, int seconds) {
        Kit kit = PWP.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }
        kit.setCooldownSeconds(seconds);
        PWP.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.cooldown_set", name, seconds), true);
        return 1;
    }

    private static int claimKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        GameManager game = PWP.getGameManager();
        if (game != null && game.isPlaying()) {
            TeamManager tm = PWP.getTeamManager();
            Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (playerTeam.isPlayable()) {
                MapConfig map = game.getCurrentMap();
                if (map != null && map.hasTeamSpawns()) {
                    int[] spawn = playerTeam == Team.NATO ? map.getNatoSpawn() : map.getRussiaSpawn();
                    if (spawn != null && spawn.length >= 3) {
                        int dist = (int) Math.sqrt(player.distanceToSqr(new Vec3(spawn[0] + 0.5, spawn[1], spawn[2] + 0.5)));
                        int radius = PWP.getConfig() != null ? PWP.getConfig().getBaseRadius() : 30;
                        if (dist > radius) {
                            player.sendSystemMessage(Component.translatable("pwp.chat.kit.base_only"));
                            return 0;
                        }
                    }
                }
            }
        }

        KitManager km = PWP.getTeamManager().getKitManager();
        Component error = km.claimKit(player, name, PWP.getTeamManager());
        if (error == null) {
            source.sendSuccess(() -> Component.translatable("pwp.chat.kit.obtained", name), false);
            return 1;
        } else {
            source.sendFailure(error);
            return 0;
        }
    }

    private static int giveKit(CommandSourceStack source, String name, ServerPlayer player) {
        KitManager km = PWP.getTeamManager().getKitManager();
        Kit kit = km.getKit(name);
        if (kit == null) {
            source.sendFailure(Component.translatable("pwp.chat.kit.not_found", name));
            return 0;
        }

        kit.applyToPlayer(player);
        source.sendSuccess(() -> Component.translatable("pwp.chat.kit.given", name, player.getName().getString()), true);
        return 1;
    }
}
