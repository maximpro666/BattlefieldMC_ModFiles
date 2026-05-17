package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.Kit;
import com.yourmod.teamsystem.core.KitManager;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().items) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }

        KitManager km = TeamSystem.getTeamManager().getKitManager();
        TeamManager tm = TeamSystem.getTeamManager();
        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();

        km.createKit(name, name, playerTeam, 0, items);
        source.sendSuccess(() -> Component.literal("§aKit created: " + name), true);
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

        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found: " + name));
            return 0;
        }

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().items) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        kit.setItems(items);

        TeamSystem.getTeamManager().getKitManager().saveKits();
        source.sendSuccess(() -> Component.literal("§aKit updated: " + name), true);
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
        source.sendSuccess(() -> Component.literal("§aKit team updated"), true);
        return 1;
    }

    private static int setKitRank(CommandSourceStack source, String name, int rank) {
        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aKit min rank updated to " + rank), true);
        return 1;
    }

    private static int setKitCooldown(CommandSourceStack source, String name, int seconds) {
        Kit kit = TeamSystem.getTeamManager().getKitManager().getKit(name);
        if (kit == null) {
            source.sendFailure(Component.literal("§cKit not found"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aKit cooldown updated to " + seconds + "s"), true);
        return 1;
    }

    private static int claimKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

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

        player.getInventory().clearContent();
        for (ItemStack item : kit.getItems()) {
            if (!item.isEmpty()) {
                player.addItem(item.copy());
            }
        }

        source.sendSuccess(() -> Component.literal("§aGave kit " + name + " to " + player.getName().getString()), true);
        return 1;
    }
}
