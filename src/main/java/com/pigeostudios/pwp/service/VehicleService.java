package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.vehicle.VehicleDefinition;
import com.pigeostudios.pwp.vehicle.VehicleDefinitionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class VehicleService {
    private final EconomyService economy;

    public VehicleService(EconomyService economy) {
        this.economy = economy;
    }

    private static final int MAX_VEHICLES_PER_PLAYER = 2;

    private double getDynamicDiscount() {
        GameManager gm = PWP.getGameManager();
        if (gm == null) return 0.0;
        int maxTime = 1800;
        int elapsed = gm.getMatchTimeRemaining() > 0 ? maxTime - gm.getMatchTimeRemaining() : 0;
        double progress = Math.min(1.0, (double) elapsed / maxTime);
        var dp = PWP.getServiceRegistry().getConfig().getEconomy().dynamicPricing;
        if (progress > 0.75) return dp.phase4Discount;
        if (progress > 0.50) return dp.phase3Discount;
        if (progress > 0.25) return dp.phase2Discount;
        return 0.0;
    }

    private double getOvertimeDiscount() {
        GameManager gm = PWP.getGameManager();
        if (gm == null || !gm.isOvertime()) return 0.0;
        return PWP.getServiceRegistry().getConfig().getEconomy().dynamicPricing.overtimeDiscount;
    }

    private double getTotalDiscount() {
        return Math.max(getDynamicDiscount(), getOvertimeDiscount());
    }

    private double getDifficultyMultiplier() {
        GameManager gm = PWP.getGameManager();
        if (gm == null) return 1.0;
        int matchTime = gm.getMatchTimeRemaining();
        if (matchTime <= 0) return 1.0;
        var dm = PWP.getServiceRegistry().getConfig().getEconomy().difficultyMultipliers;
        if (matchTime >= 160000) return dm.hardVehiclePrice;
        if (matchTime >= 90000) return dm.normalVehiclePrice;
        return dm.easyVehiclePrice;
    }

    public Component buyVehicle(ServerPlayer player, String vehicleId, TeamManager teamManager) {
        var runtime = BattlefieldRuntime.getInstance();
        VehicleDefinition def = runtime.getVehicleDefRegistry().get(vehicleId);
        if (def == null) return Component.translatable("pwp.chat.vehicle.not_found", vehicleId);

        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());
        Team playerTeam = data.getTeam();
        if (!playerTeam.isPlayable()) return Component.translatable("pwp.chat.vehicle.not_on_team");

        // M2: per-player vehicle limit
        var vmCheck = PWP.getVehicleManager();
        if (vmCheck != null && vmCheck.countPlayerVehicles(player.getUUID()) >= MAX_VEHICLES_PER_PLAYER) {
            return Component.translatable("pwp.chat.vehicle.per_player_limit", MAX_VEHICLES_PER_PLAYER);
        }

        // T2: deduct vehicle ticket cost from team
        var ticketSvc = PWP.getServiceRegistry().getTickets();
        int ticketCost = def.getTicketCost();
        if (ticketCost > 0 && ticketSvc != null && ticketSvc.getTickets(playerTeam) < ticketCost) {
            return Component.translatable("pwp.chat.vehicle.insufficient_tickets", ticketCost);
        }

        // D9: rank check — compare player's vehicle access level to vehicle's required level
        int playerRank = data.getRankOrdinal();
        int playerAccess = PWP.getServiceRegistry().getConfig().getVehicleAccessLevel(playerRank);
        if (def.getRequiredAccessLevel() > playerAccess) {
            int requiredRank = 0;
            var ranks = PWP.getServiceRegistry().getConfig().getRanks();
            for (int i = 0; i < ranks.size(); i++) {
                if (ranks.get(i).vehicleAccessLevel >= def.getRequiredAccessLevel()) {
                    requiredRank = i;
                    break;
                }
            }
            String rankName = requiredRank < ranks.size() ? ranks.get(requiredRank).displayName : "?";
            return Component.translatable("pwp.chat.vehicle.insufficient_rank", def.getRequiredAccessLevel(), rankName);
        }

        // Cooldown check
        VehicleManager vm = PWP.getVehicleManager();
        if (vm.isOnCooldown(playerTeam, vehicleId)) {
            return Component.translatable("pwp.chat.vehicle.cooldown", 0);
        }

        // Population limit check
        int activeCount = vm.countActiveVehicles(playerTeam);
        int popLimit = def.getPopulationLimit(runtime.getActiveFrontlineCount());
        if (activeCount >= popLimit) {
            return Component.translatable("pwp.chat.vehicle.limit_reached", popLimit);
        }

        // Check costs first (without deducting) — apply dynamic discount + difficulty multiplier
        int baseBcCost = def.getCosts().getDeployBC();
        int baseVcCost = def.getCosts().getDeployVC();
        double difficultyMul = getDifficultyMultiplier();
        double discount = getTotalDiscount();
        int bcCost = baseBcCost;
        bcCost = (int) Math.round(bcCost * difficultyMul);
        if (discount > 0) bcCost = Math.max(1, (int) Math.round(bcCost * (1.0 - discount)));
        int vcCost = baseVcCost;
        vcCost = (int) Math.round(vcCost * difficultyMul);
        if (discount > 0) vcCost = Math.max(1, (int) Math.round(vcCost * (1.0 - discount)));
        if (bcCost > 0 && economy.getBC(player.getUUID()) < bcCost) {
            return Component.translatable("pwp.chat.vehicle.insufficient_bc", bcCost);
        }
        if (vcCost > 0 && economy.getVC(playerTeam) < vcCost) {
            return Component.translatable("pwp.chat.vehicle.insufficient_vc", vcCost);
        }

        // Resolve entity type and position before deducting
        GameManager game = PWP.getGameManager();
        MapConfig map = game != null ? game.getCurrentMap() : null;
        ServerLevel level = (ServerLevel) player.level();
        int[] spawnPos = new int[]{player.getBlockX(), player.getBlockY(), player.getBlockZ()};
        if (map != null) {
            int[] vs = playerTeam == Team.NATO ? map.getNatoVehicleSpawn() : map.getRussiaVehicleSpawn();
            if (vs != null && vs.length >= 3) spawnPos = vs;
        }
        // L7: validate spawn position — search upward if inside a solid block
        for (int dy = 0; dy <= 5; dy++) {
            int checkY = spawnPos[1] + dy;
            if (checkY < level.getMinBuildHeight() || checkY > level.getMaxBuildHeight()) break;
            var blockState = level.getBlockState(new net.minecraft.core.BlockPos(spawnPos[0], checkY, spawnPos[2]));
            if (blockState.isAir() || blockState.canBeReplaced()) {
                spawnPos[1] = checkY;
                break;
            }
        }

        String entityTypeStr = def.getEntityType();
        if (entityTypeStr == null || entityTypeStr.isEmpty())
            return Component.translatable("pwp.chat.vehicle.no_entity_type");

        EntityType<?> type = def.resolveEntityType();
        if (type == null)
            return Component.translatable("pwp.chat.vehicle.invalid_entity", entityTypeStr);

        CompoundTag nbt = def.resolveNbt();
        if (nbt != null) {
            nbt.remove("UUID"); nbt.remove("uuid"); nbt.remove("Uuid");
        }

        // H2 fix: create entity before deducting
        int offsetIdx = activeCount;
        double angle = offsetIdx * 1.2;
        double dist = (offsetIdx / 3 + 1) * 3.0;
        double ox = Math.cos(angle) * dist;
        double oz = Math.sin(angle) * dist;

        Entity ent = type.create(level);
        if (ent == null) return Component.translatable("pwp.chat.vehicle.create_error");

        // Entity created — now deduct
        if (bcCost > 0) economy.deductBC(player.getUUID(), bcCost);
        if (vcCost > 0) economy.deductVC(playerTeam, vcCost);
        if (ticketCost > 0 && ticketSvc != null) ticketSvc.deductVehicleSpawnCost(playerTeam, ticketCost);

        // Finish setup
        if (nbt != null) ent.load(nbt);
        ent.setPos(spawnPos[0] + 0.5 + ox, spawnPos[1], spawnPos[2] + 0.5 + oz);
        level.addFreshEntity(ent);
        player.startRiding(ent, true);

        vm.registerSpawnedVehicle(ent, player.getUUID());
        vm.setCooldown(playerTeam, vehicleId, def.getCooldownSeconds());
        runtime.trackVehicleSpawn(ent.getUUID(), playerTeam, vehicleId);

        PWP.LOGGER.info("Player {} bought vehicle {} for team {}",
            player.getName().getString(), vehicleId, playerTeam);
        return null;
    }
}
