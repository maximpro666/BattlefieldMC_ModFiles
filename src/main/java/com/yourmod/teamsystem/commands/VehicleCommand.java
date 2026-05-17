package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

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
            .then(Commands.literal("create")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("team", StringArgumentType.word())
                        .then(Commands.argument("minRank", IntegerArgumentType.integer(0))
                            .then(Commands.argument("ticketCost", IntegerArgumentType.integer(1))
                                .then(Commands.argument("cooldown", IntegerArgumentType.integer(0))
                                    .then(Commands.argument("maxActive", IntegerArgumentType.integer(1))
                                        .executes(ctx -> createVehicle(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "id"),
                                            StringArgumentType.getString(ctx, "team"),
                                            IntegerArgumentType.getInteger(ctx, "minRank"),
                                            IntegerArgumentType.getInteger(ctx, "ticketCost"),
                                            IntegerArgumentType.getInteger(ctx, "cooldown"),
                                            IntegerArgumentType.getInteger(ctx, "maxActive"))))))))))
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

    private static int saveVehicleKit(CommandSourceStack source, String name, Team team) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            source.sendFailure(Component.literal("§cYou are not in a vehicle!"));
            return 0;
        }

        VehicleManager vm = TeamSystem.getVehicleManager();
        TeamManager tm = TeamSystem.getTeamManager();

        if (team == null) {
            team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
        }

        String entityTypeId = EntityType.getKey(vehicle.getType()).toString();
        CompoundTag nbt = new CompoundTag();
        vehicle.save(nbt);

        VehicleData data = new VehicleData(name, name, team, 0, 0);
        data.setEntityData(entityTypeId, nbt);
        vm.addVehicle(data);

        String teamName = team != null ? team.getColoredName().getString() : "§7Any";
        source.sendSuccess(() -> Component.literal(
            "§aVehicle kit saved: " + name + " (" + entityTypeId + ") for " + teamName), true);
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
            vm.registerSpawnedVehicle(ent, player.getUUID());
            if (i == 0) player.startRiding(ent, true);
        }

        source.sendSuccess(() -> Component.literal(
            String.format("§aSpawned %d x %s", count, vehicle.getDisplayName())), true);
        return 1;
    }

    private static int spawnVehicleAt(CommandSourceStack source, String vehicleId, ServerPlayer target) {
        VehicleManager vm = TeamSystem.getVehicleManager();
        VehicleData vehicle = vm.getVehicle(vehicleId);

        if (vehicle == null) {
            source.sendFailure(Component.literal("§cТехника не найдена: " + vehicleId));
            return 0;
        }

        EntityType<?> type = vehicle.resolveEntityType();
        if (type == null) {
            source.sendFailure(Component.literal("§cНеверный тип сущности для: " + vehicleId));
            return 0;
        }

        ServerLevel level = (ServerLevel) target.level();
        CompoundTag nbt = vehicle.resolveNbt();
        if (nbt != null) {
            nbt.remove("UUID"); nbt.remove("uuid"); nbt.remove("Uuid");
        }

        Entity ent = type.create(level);
        if (ent == null) {
            source.sendFailure(Component.literal("§cОшибка создания техники"));
            return 0;
        }
        if (nbt != null) ent.load(nbt);
        ent.setPos(target.getX(), target.getY(), target.getZ());
        level.addFreshEntity(ent);
        vm.registerSpawnedVehicle(ent, target.getUUID());
        target.startRiding(ent, true);

        source.sendSuccess(() -> Component.literal(
            String.format("§aТехника %s создана для %s", vehicle.getDisplayName(), target.getName().getString())), true);
        return 1;
    }

    private static int createVehicle(CommandSourceStack source, String id, String teamName, int minRank, int ticketCost, int cooldown, int maxActive) {
        Team team = Team.fromString(teamName);
        if (team == null) {
            source.sendFailure(Component.literal("§cНеверная команда: " + teamName + " (nato/russia/spectator)"));
            return 0;
        }

        VehicleManager vm = TeamSystem.getVehicleManager();
        if (vm.getVehicle(id) != null) {
            source.sendFailure(Component.literal("§cТехника \"" + id + "\" уже существует"));
            return 0;
        }

        VehicleData data = new VehicleData(id, id, team, minRank, ticketCost);
        data.setCooldownSeconds(cooldown);
        data.setMaxActive(maxActive);
        vm.addVehicle(data);

        source.sendSuccess(() -> Component.literal(
            String.format("§aТехника создана: §b%s§a | Команда: %s§a | Кд: §c%dс§a | Лимит: §c%d",
                id, team.getColoredName().getString(), cooldown, maxActive)), true);
        return 1;
    }

    private static int setVehicleSpawn(CommandSourceStack source, Team team) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        GameManager game = TeamSystem.getGameManager();
        if (game == null || !game.isPlaying()) {
            source.sendFailure(Component.literal("§cИгра не активна"));
            return 0;
        }
        MapConfig map = game.getCurrentMap();
        if (map == null) {
            source.sendFailure(Component.literal("§cНет активной карты"));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        if (team == Team.NATO) {
            map.setNatoVehicleSpawn(new int[]{pos.getX(), pos.getY(), pos.getZ()});
        } else {
            map.setRussiaVehicleSpawn(new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
        TeamSystem.getMapPoolManager().saveConfig();
        player.sendSystemMessage(Component.literal(String.format(
            "§aСпавн техники для %s установлен на %d %d %d",
            team.getColoredName().getString(), pos.getX(), pos.getY(), pos.getZ())));
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

        String error = vm.buyVehicle(player, vehicleId, tm);
        if (error == null) {
            VehicleData vehicle = vm.getVehicle(vehicleId);
            source.sendSuccess(() -> Component.literal(
                String.format("§aКуплена техника: §b%s", vehicle.getDisplayName())), false);
            return 1;
        } else {
            source.sendFailure(Component.literal(error));
            return 0;
        }
    }

    private static int toggleHidePlaque(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        VehicleManager vm = TeamSystem.getVehicleManager();
        if (vm == null) return 0;
        boolean nowHidden = vm.toggleHidePlaque(player.getUUID());
        player.sendSystemMessage(Component.literal(
            nowHidden ? "§aПлажка вражеской техники скрыта" : "§eПлажка вражеской техники видна"));
        return 1;
    }
}
