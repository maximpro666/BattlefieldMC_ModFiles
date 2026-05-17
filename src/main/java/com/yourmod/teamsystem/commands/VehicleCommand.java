package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class VehicleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vehicle")
            .then(Commands.literal("spawn")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("vehicleId", StringArgumentType.word())
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 10))
                        .executes(ctx -> spawnVehicle(ctx.getSource(),
                            StringArgumentType.getString(ctx, "vehicleId"),
                            IntegerArgumentType.getInteger(ctx, "count"))))))
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
                    .executes(ctx -> saveVehicleKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
        );
    }

    private static int saveVehicleKit(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            source.sendFailure(Component.literal("§cYou are not in a vehicle!"));
            return 0;
        }

        VehicleManager vm = TeamSystem.getVehicleManager();
        TeamManager tm = TeamSystem.getTeamManager();

        String entityTypeId = EntityType.getKey(vehicle.getType()).toString();
        CompoundTag nbt = new CompoundTag();
        vehicle.save(nbt);

        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();

        VehicleData data = new VehicleData(name, name, playerTeam, 0, 0);
        data.setEntityData(entityTypeId, nbt);
        vm.addVehicle(data);

        source.sendSuccess(() -> Component.literal(
            "§aVehicle kit saved: " + name + " (" + entityTypeId + ")"), true);
        return 1;
    }

    private static int spawnVehicle(CommandSourceStack source, String vehicleId, int count) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        VehicleManager vm = TeamSystem.getVehicleManager();
        VehicleData vehicle = vm.getVehicle(vehicleId);

        if (vehicle == null) {
            source.sendFailure(Component.literal("§cVehicle not found: " + vehicleId));
            return 0;
        }

        EntityType<?> type = vehicle.resolveEntityType();
        if (type == null) {
            source.sendFailure(Component.literal("§cInvalid entity type for: " + vehicleId));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        CompoundTag nbt = vehicle.resolveNbt();
        if (nbt != null) {
            nbt.remove("UUID");
            nbt.remove("uuid");
            nbt.remove("Uuid");
        }

        for (int i = 0; i < count; i++) {
            Entity ent = type.create(level);
            if (ent == null) continue;
            if (nbt != null) ent.load(nbt);
            ent.setPos(player.getX() + i * 2, player.getY(), player.getZ() + i * 2);
            level.addFreshEntity(ent);
            if (i == 0) player.startRiding(ent, true);
        }

        source.sendSuccess(() -> Component.literal(
            String.format("§aSpawned %d x %s", count, vehicle.getDisplayName())), true);
        return 1;
    }

    private static int listVehicles(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        VehicleManager vm = TeamSystem.getVehicleManager();
        TeamManager tm = TeamSystem.getTeamManager();

        source.sendSuccess(() -> Component.literal("§6Available Vehicles:"), false);
        for (VehicleData vehicle : vm.getAvailableVehicles(player, tm)) {
            source.sendSuccess(() -> Component.literal(
                String.format("§b%s§6: §a%s§6 (cost: §c%d§6 tickets)",
                    vehicle.getVehicleId(), vehicle.getDisplayName(), vehicle.getTicketCost())), false);
        }
        return 1;
    }

    private static int vehicleInfo(CommandSourceStack source, String vehicleId) {
        VehicleManager vm = TeamSystem.getVehicleManager();
        VehicleData vehicle = vm.getVehicle(vehicleId);

        if (vehicle == null) {
            source.sendFailure(Component.literal("§cVehicle not found"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
            String.format("§6Vehicle: §b%s§6 | Team: §a%s§6 | Ticket Cost: §c%d§6 | Min Rank: §a%d",
                vehicle.getDisplayName(), vehicle.getTeam().getName(),
                vehicle.getTicketCost(), vehicle.getMinRankOrdinal())), false);
        return 1;
    }

    private static int buyVehicle(CommandSourceStack source, String vehicleId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        VehicleManager vm = TeamSystem.getVehicleManager();
        TeamManager tm = TeamSystem.getTeamManager();

        if (vm.buyVehicle(player, vehicleId, tm)) {
            VehicleData vehicle = vm.getVehicle(vehicleId);
            source.sendSuccess(() -> Component.literal(
                String.format("§aVehicle purchased: §b%s", vehicle.getDisplayName())), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cCannot purchase vehicle: check rank/team/tickets"));
            return 0;
        }
    }
}
