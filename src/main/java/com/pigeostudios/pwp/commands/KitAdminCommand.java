package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.network.KitAdminConfigSyncPacket;
import com.pigeostudios.pwp.network.KitConfigSyncPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.PacketDistributor;

import java.nio.file.Files;
import java.nio.file.Path;

public class KitAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kitadmin")
            .requires(ctx -> ctx.hasPermission(2))
            .then(Commands.literal("request")
                .executes(ctx -> requestKitConfig(ctx)))
            .then(Commands.literal("reload")
                .executes(ctx -> reloadKitConfig(ctx)))
            .then(Commands.literal("save")
                .then(Commands.argument("json", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String json = StringArgumentType.getString(ctx, "json");
                        return saveKitConfig(ctx, json);
                    })))
            .executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.literal("§6Usage: /kitadmin request | reload | save <json>"));
                }
                return Command.SINGLE_SUCCESS;
            }));
    }

    private static int requestKitConfig(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Player only"));
            return 0;
        }

        KitConfig cfg = KitConfig.get();
        if (cfg == null) {
            player.sendSystemMessage(Component.literal("§cKitConfig not loaded. Use /kitadmin reload first."));
            return 0;
        }

        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(cfg);
        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new KitAdminConfigSyncPacket(json));
        player.sendSystemMessage(Component.literal("§aKit config sent (" + json.length() + " bytes)"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadKitConfig(CommandContext<CommandSourceStack> ctx) {
        Path worldDir = ctx.getSource().getServer().getWorldPath(LevelResource.ROOT);
        KitConfig.loadOrCreate(worldDir);
        ctx.getSource().sendSuccess(() -> Component.literal("§aKit config reloaded from disk"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int saveKitConfig(CommandContext<CommandSourceStack> ctx, String json) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("Player only"));
            return 0;
        }

        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            KitConfig parsed = gson.fromJson(json, KitConfig.class);
            if (parsed == null) {
                player.sendSystemMessage(Component.literal("§cInvalid JSON"));
                return 0;
            }

            Path worldDir = ctx.getSource().getServer().getWorldPath(LevelResource.ROOT);
            Path dir = worldDir.resolve("pwp");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("kits.json"), gson.toJson(parsed));
            KitConfig.set(parsed);
            KitConfigSyncPacket syncAll = new KitConfigSyncPacket(gson.toJson(parsed));
            PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), syncAll);
            player.sendSystemMessage(Component.literal("§aKit config saved to disk and broadcast to all players"));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cError: " + e.getMessage()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
