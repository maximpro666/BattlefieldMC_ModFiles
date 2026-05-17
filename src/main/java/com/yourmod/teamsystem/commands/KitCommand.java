package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.Kit;
import com.yourmod.teamsystem.core.KitManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
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

        TeamManager tm = TeamSystem.getTeamManager();
        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        Kit kit = Kit.fromPlayerInventory(name, name, playerTeam, 0, player);

        tm.getKitManager().getKits().put(name, kit);
        tm.getKitManager().saveKits();

        source.sendSuccess(() -> Component.literal("§aKit created from your inventory: " + name), true);
        return 1;
    }

    private static int deleteKit(CommandSourceStack source, String name) {
        TeamSystem.getTeamManager().getKitManager().deleteKit(name);
        source.sendSuccess(() -> Component.literal("§aKit deleted: " + name), true);
        return 1;
    }

    private static int editKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        TeamManager tm = TeamSystem.getTeamManager();
        Kit kit = tm.getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found: " + name));
            return 0;
        }

        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        Kit updated = Kit.fromPlayerInventory(name, kit.getDisplayName(), playerTeam, kit.getMinRankOrdinal(), player);
        tm.getKitManager().getKits().put(name, updated);
        tm.getKitManager().saveKits();

        source.sendSuccess(() -> Component.literal("§aKit updated from your inventory: " + name), true);
        return 1;
    }

    private static int listKits(CommandSourceStack source, ServerPlayer player, Team team) {
        if (player == null) return 0;

        KitManager km = TeamSystem.getTeamManager().getKitManager();
        List<Kit> available = km.getAvailableKits(player, TeamSystem.getTeamManager());

        source.sendSuccess(() -> Component.literal("§6Available Kits:"), false);
        for (Kit kit : available) {
            source.sendSuccess(() -> Component.literal(
                String.format("§b%s§6: §a%s §6(%d items)",
                    kit.getName(), kit.getDisplayName(), kit.getItems().size())), false);
        }
        return 1;
    }

    private static int kitInfo(CommandSourceStack source, String name) {
        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found: " + name));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
            String.format("§6Kit: §b%s§6 | Team: §a%s§6 | Min Rank: §a%d§6 | Items: §a%d",
                kit.getDisplayName(), kit.getTeam().getName(), kit.getMinRankOrdinal(), kit.getItems().size())), false);
        return 1;
    }

    private static int setKitTeam(CommandSourceStack source, String name, String teamStr) {
        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found"));
            return 0;
        }
        Team team = Team.fromString(teamStr);
        if (team == null) {
            source.sendFailure(Component.literal("§cInvalid team: " + teamStr));
            return 0;
        }
        kit.setTeam(team);
        TeamSystem.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.literal("§aKit " + name + " team set to " + team.getName()), true);
        return 1;
    }

    private static int setKitRank(CommandSourceStack source, String name, int rank) {
        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found"));
            return 0;
        }
        kit.setMinRankOrdinal(rank);
        TeamSystem.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.literal("§aKit " + name + " min rank set to " + rank), true);
        return 1;
    }

    private static int setKitCooldown(CommandSourceStack source, String name, int seconds) {
        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found"));
            return 0;
        }
        kit.setCooldownSeconds(seconds);
        TeamSystem.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.literal("§aKit " + name + " cooldown set to " + seconds + "s"), true);
        return 1;
    }

    private static int claimKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        GameManager game = TeamSystem.getGameManager();
        if (game != null && game.isPlaying()) {
            TeamManager tm = TeamSystem.getTeamManager();
            Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (playerTeam.isPlayable()) {
                MapConfig map = game.getCurrentMap();
                if (map != null && map.hasTeamSpawns()) {
                    int[] spawn = playerTeam == Team.NATO ? map.getNatoSpawn() : map.getRussiaSpawn();
                    if (spawn != null && spawn.length >= 3) {
                        int dist = (int) Math.sqrt(player.distanceToSqr(new Vec3(spawn[0] + 0.5, spawn[1], spawn[2] + 0.5)));
                        int radius = TeamSystem.getConfig() != null ? TeamSystem.getConfig().getBaseRadius() : 30;
                        if (dist > radius) {
                            player.sendSystemMessage(Component.literal("§cВы можете сменить обвес только на своей базе!"));
                            return 0;
                        }
                    }
                }
            }
        }

        KitManager km = TeamSystem.getTeamManager().getKitManager();
        if (km.claimKit(player, name, TeamSystem.getTeamManager())) {
            source.sendSuccess(() -> Component.literal("§aKit claimed: " + name), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cCannot claim kit: check rank/team/cooldown"));
            return 0;
        }
    }

    private static int giveKit(CommandSourceStack source, String name, ServerPlayer player) {
        KitManager km = TeamSystem.getTeamManager().getKitManager();
        Kit kit = km.getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found"));
            return 0;
        }

        kit.applyToPlayer(player);
        source.sendSuccess(() -> Component.literal("§aGave kit " + name + " to " + player.getName().getString()), true);
        return 1;
    }
}
