package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.vehicle.VehicleDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Collection;

public class VehicleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vehicle")
            .then(Commands.literal("spawn")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("vehicleId", StringArgumentType.word())
                    .then(Commands.literal("at")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> spawnVehicleAt(ctx.getSource(),
                                StringArgumentType.getString(ctx, "vehicleId"),
                                EntityArgument.getPlayer(ctx, "target"))))
                    )
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> spawnVehicle(ctx.getSource(),
                            StringArgumentType.getString(ctx, "vehicleId"),
                            IntegerArgumentType.getInteger(ctx, "count"))))))
            .then(Commands.literal("setspawn")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("nato")
                    .executes(ctx -> setVehicleSpawn(ctx.getSource(), Team.NATO)))
                .then(Commands.literal("russia")
                    .executes(ctx -> setVehicleSpawn(ctx.getSource(), Team.RUSSIA))))
            .then(Commands.literal("list")
                .executes(ctx -> listVehicles(ctx.getSource())))
            .then(Commands.literal("info")
                .then(Commands.argument("vehicleId", StringArgumentType.word())
                    .executes(ctx -> vehicleInfo(ctx.getSource(), StringArgumentType.getString(ctx, "vehicleId")))))
            .then(Commands.literal("buy")
                .then(Commands.argument("vehicleId", StringArgumentType.word())
                    .executes(ctx -> buyVehicle(ctx.getSource(), StringArgumentType.getString(ctx, "vehicleId")))))
            .then(Commands.literal("savekit")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.literal("nato")
                        .executes(ctx -> saveVehicleKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), Team.NATO)))
                    .then(Commands.literal("russia")
                        .executes(ctx -> saveVehicleKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), Team.RUSSIA)))
                    .executes(ctx -> saveVehicleKit(ctx.getSource(), StringArgumentType.getString(ctx, "name"), null))))
            .then(Commands.literal("hideplaque")
                .executes(ctx -> toggleHidePlaque(ctx.getSource())))
        );
    }

    private static int spawnVehicle(CommandSourceStack source, String vehicleId, int count) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        VehicleManager vm = PWP.getVehicleManager();
        VehicleDefinition def = vm.getDefinition(vehicleId);

        if (def == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.not_found", vehicleId));
            return 0;
        }

        EntityType<?> type = def.resolveEntityType();
        if (type == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.invalid_entity", vehicleId));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        CompoundTag nbt = def.resolveNbt();
        if (nbt != null) {
            nbt.remove("UUID"); nbt.remove("uuid"); nbt.remove("Uuid");
        }

        for (int i = 0; i < count; i++) {
            Entity ent = type.create(level);
            if (ent == null) continue;
            if (nbt != null) ent.load(nbt);
            ent.setPos(player.getX() + i * 2, player.getY(), player.getZ() + i * 2);
            level.addFreshEntity(ent);
            vm.registerSpawnedVehicle(ent, player.getUUID());
            if (i == 0) player.startRiding(ent, true);
        }

        source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.spawned", count, def.getDisplayName()), true);
        return 1;
    }

    private static int spawnVehicleAt(CommandSourceStack source, String vehicleId, ServerPlayer target) {
        VehicleManager vm = PWP.getVehicleManager();
        VehicleDefinition def = vm.getDefinition(vehicleId);

        if (def == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.not_found", vehicleId));
            return 0;
        }

        EntityType<?> type = def.resolveEntityType();
        if (type == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.invalid_entity", vehicleId));
            return 0;
        }

        ServerLevel level = (ServerLevel) target.level();
        CompoundTag nbt = def.resolveNbt();
        if (nbt != null) {
            nbt.remove("UUID"); nbt.remove("uuid"); nbt.remove("Uuid");
        }

        Entity ent = type.create(level);
        if (ent == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.create_error"));
            return 0;
        }
        if (nbt != null) ent.load(nbt);
        ent.setPos(target.getX(), target.getY(), target.getZ());
        level.addFreshEntity(ent);
        vm.registerSpawnedVehicle(ent, target.getUUID());
        target.startRiding(ent, true);

        source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.created_for", def.getDisplayName(), target.getName().getString()), true);
        return 1;
    }

    private static int saveVehicleKit(CommandSourceStack source, String name, Team team) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.not_in_vehicle"));
            return 0;
        }

        if (team == null) {
            team = PWP.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
        }

        String entityTypeId = EntityType.getKey(vehicle.getType()).toString();
        CompoundTag nbt = new CompoundTag();
        vehicle.save(nbt);

        try {
            java.nio.file.Path defDir = java.nio.file.Paths.get("config/pwp/vehicle_definitions");
            java.nio.file.Files.createDirectories(defDir);
            com.google.gson.JsonObject out = new com.google.gson.JsonObject();
            out.addProperty("id", name);
            out.addProperty("displayName", name);
            out.addProperty("entityType", entityTypeId);
            out.addProperty("nbt", nbt.toString());

            com.google.gson.JsonObject costs = new com.google.gson.JsonObject();
            costs.addProperty("deployBC", 1000);
            costs.addProperty("deployVC", 200);
            out.add("costs", costs);

            out.addProperty("cooldownSeconds", 60);
            out.addProperty("ticketCost", 0);

            java.nio.file.Path outFile = defDir.resolve(name + ".json");
            java.nio.file.Files.writeString(outFile,
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));

            BattlefieldRuntime.getInstance().getVehicleDefRegistry().loadAll();

            source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.kit_saved", name, entityTypeId), true);
        } catch (Exception e) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.kit_save_failed", e.getMessage()));
        }
        return 1;
    }

    private static int setVehicleSpawn(CommandSourceStack source, Team team) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        GameManager game = PWP.getGameManager();
        if (game == null || !game.isPlaying()) {
            source.sendFailure(Component.translatable("pwp.chat.game.not_active"));
            return 0;
        }
        MapConfig map = game.getCurrentMap();
        if (map == null) {
            source.sendFailure(Component.translatable("pwp.chat.game.no_map"));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        if (team == Team.NATO) {
            map.setNatoVehicleSpawn(new int[]{pos.getX(), pos.getY(), pos.getZ()});
        } else {
            map.setRussiaVehicleSpawn(new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
        PWP.getMapPoolManager().saveConfig();
        player.sendSystemMessage(Component.translatable("pwp.chat.vehicle.spawn_set",
            team.getColoredName().getString(), pos.getX(), pos.getY(), pos.getZ()));
        return 1;
    }

    private static int listVehicles(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        VehicleDefinitionRegistryHelper helper = new VehicleDefinitionRegistryHelper();

        source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.list_header"), false);
        for (VehicleDefinition def : helper.getAll()) {
            int bc = def.getCosts().getDeployBC();
            int vc = def.getCosts().getDeployVC();
            source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.list_entry",
                def.getId(), def.getDisplayName(), bc, vc), false);
        }
        return 1;
    }

    private static int vehicleInfo(CommandSourceStack source, String vehicleId) {
        VehicleManager vm = PWP.getVehicleManager();
        VehicleDefinition def = vm.getDefinition(vehicleId);

        if (def == null) {
            source.sendFailure(Component.translatable("pwp.chat.vehicle.not_found", vehicleId));
            return 0;
        }

        int bc = def.getCosts().getDeployBC();
        int vc = def.getCosts().getDeployVC();
        source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.info",
            def.getDisplayName(), bc, vc, def.getCategory()), false);
        return 1;
    }

    private static int buyVehicle(CommandSourceStack source, String vehicleId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        VehicleManager vm = PWP.getVehicleManager();
        TeamManager tm = PWP.getTeamManager();

        Component error = PWP.getServiceRegistry().getVehicle().buyVehicle(player, vehicleId, tm);
        if (error == null) {
            VehicleDefinition def = vm.getDefinition(vehicleId);
            source.sendSuccess(() -> Component.translatable("pwp.chat.vehicle.bought",
                def != null ? def.getDisplayName() : vehicleId), false);
            return 1;
        } else {
            source.sendFailure(error);
            return 0;
        }
    }

    private static int toggleHidePlaque(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        VehicleManager vm = PWP.getVehicleManager();
        if (vm == null) return 0;
        boolean nowHidden = vm.toggleHidePlaque(player.getUUID());
        player.sendSystemMessage(Component.translatable(
            nowHidden ? "pwp.chat.vehicle.plaque_hidden" : "pwp.chat.vehicle.plaque_visible"));
        return 1;
    }

    // ===== Helper: wraps VehicleDefinitionRegistry for command use =====

    private static class VehicleDefinitionRegistryHelper {
        private final com.pigeostudios.pwp.vehicle.VehicleDefinitionRegistry reg =
            BattlefieldRuntime.getInstance().getVehicleDefRegistry();

        java.util.Collection<VehicleDefinition> getAll() {
            return reg.getAll();
        }
    }
}
