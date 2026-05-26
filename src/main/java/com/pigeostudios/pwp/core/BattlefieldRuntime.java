package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.ammo.AmmoCooldownManager;
import com.pigeostudios.pwp.ammo.AmmoService;

import com.pigeostudios.pwp.service.EconomyService;
import com.pigeostudios.pwp.state.FrontlineState;
import com.pigeostudios.pwp.state.VehicleState;
import com.pigeostudios.pwp.system.ActivitySystem;
import com.pigeostudios.pwp.system.VehicleUpkeepSystem;
import com.pigeostudios.pwp.vehicle.VehicleDefinitionRegistry;
import com.pigeostudios.pwp.vehicle.adapter.VehicleAdapterRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;

public class BattlefieldRuntime {
    private static BattlefieldRuntime INSTANCE;

    private BattlefieldRuntime() {
        INSTANCE = this;
    }

    public static BattlefieldRuntime getInstance() {
        if (INSTANCE == null) INSTANCE = new BattlefieldRuntime();
        return INSTANCE;
    }

    private EconomyService getEconomyService() {
        var reg = PWP.getServiceRegistry();
        return reg != null ? reg.getEconomy() : null;
    }

    // ===== Реестры =====
    private final VehicleDefinitionRegistry vehicleDefRegistry = new VehicleDefinitionRegistry();
    private final VehicleAdapterRegistry vehicleAdapterRegistry = VehicleAdapterRegistry.getInstance();
    private final AmmoService ammoService = new AmmoService();
    private final AmmoCooldownManager ammoCooldownManager = new AmmoCooldownManager();

    public VehicleDefinitionRegistry getVehicleDefRegistry() { return vehicleDefRegistry; }
    public VehicleAdapterRegistry getVehicleAdapterRegistry() { return vehicleAdapterRegistry; }
    public AmmoService getAmmoService() { return ammoService; }
    public AmmoCooldownManager getAmmoCooldownManager() { return ammoCooldownManager; }

    // ===== State objects =====
    public final FrontlineState frontline = new FrontlineState();
    public final VehicleState vehicles = new VehicleState();

    // ===== System objects =====
    private final ActivitySystem activitySystem = new ActivitySystem();
    private final VehicleUpkeepSystem vehicleUpkeepSystem = new VehicleUpkeepSystem();

    private int recalcTickCounter = 0;
    private static final int RECALC_INTERVAL = 20;

    public VehicleUpkeepSystem getVehicleUpkeepSystem() { return vehicleUpkeepSystem; }

    // ===== Экономика: BC (делегирует в EconomyService) =====
    public int getBC(UUID uuid) {
        EconomyService svc = getEconomyService();
        return svc != null ? svc.getBC(uuid) : 0;
    }

    public int getWC(UUID uuid) {
        EconomyService svc = getEconomyService();
        return svc != null ? svc.getWC(uuid) : 0;
    }

    public void setBC(UUID uuid, int amount) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.setBC(uuid, amount);
    }

    public void setWC(UUID uuid, int amount) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.setWC(uuid, amount);
    }

    public void addBC(UUID uuid, int amount) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.addBC(uuid, amount);
    }

    public void addWC(UUID uuid, int amount) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.addWC(uuid, amount);
    }

    public boolean deductBC(UUID uuid, int amount) {
        EconomyService svc = getEconomyService();
        return svc != null && svc.deductBC(uuid, amount);
    }

    public boolean deductWC(UUID uuid, int amount) {
        EconomyService svc = getEconomyService();
        return svc != null && svc.deductWC(uuid, amount);
    }

    // ===== Vehicle Credits =====
    public int getVC(Team team) {
        EconomyService svc = getEconomyService();
        return svc != null ? svc.getVC(team) : 0;
    }

    public boolean deductVC(Team team, int amount) {
        EconomyService svc = getEconomyService();
        return svc != null && svc.deductVC(team, amount);
    }

    // ===== Activity Score =====
    public static final int SCORE_MOVE = FrontlineState.SCORE_MOVE;
    public static final int SCORE_DAMAGE = FrontlineState.SCORE_DAMAGE;
    public static final int SCORE_HEAL = FrontlineState.SCORE_HEAL;
    public static final int SCORE_REVIVE = FrontlineState.SCORE_REVIVE;
    public static final int SCORE_CAPTURE = FrontlineState.SCORE_CAPTURE;
    public static final int SCORE_SUPPLY = FrontlineState.SCORE_SUPPLY;
    public static final int SCORE_TAKE_DAMAGE = FrontlineState.SCORE_TAKE_DAMAGE;
    public static final int ACTIVITY_THRESHOLD = FrontlineState.ACTIVITY_THRESHOLD;

    public void addActivity(UUID uuid, int score) {
        frontline.addActivity(uuid, score);
    }

    public boolean isActive(UUID uuid) {
        return frontline.isActive(uuid);
    }

    public int getActivityScore(UUID uuid) {
        return frontline.getActivityScore(uuid);
    }

    // ===== Active Frontline Players =====
    public int getActiveFrontlineCount() { return frontline.getActiveCount(); }

    // ===== Sync to clients =====
    public void syncBC(ServerPlayer player) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.syncBC(player);
    }

    public void syncWC(ServerPlayer player) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.syncWC(player);
    }

    public void syncAll(ServerPlayer player) {
        syncBC(player);
        syncWC(player);
        syncVC(player);
    }

    public void syncVC(ServerPlayer player) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.syncVC(player);
    }

    public void syncBCToAll() {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.syncBCToAll();
    }

    // ===== Reset =====
    public void resetMatch() {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.resetMatch();
        frontline.reset();
        vehicles.reset();
        activitySystem.reset();
        vehicleUpkeepSystem.reset();
        recalcTickCounter = 0;
        ammoService.clear();
        ammoCooldownManager.resetAll();
    }

    public void resetForPlayer(UUID uuid) {
        EconomyService svc = getEconomyService();
        if (svc != null) svc.resetForPlayer(uuid);
        frontline.resetForPlayer(uuid);
        ammoCooldownManager.resetPlayer(uuid);
    }

    // ===== Единый тик =====
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        GameManager gm = PWP.getGameManager();
        if (gm == null || !gm.isPlaying()) return;

        activitySystem.tick(frontline);

        recalcTickCounter++;
        if (recalcTickCounter >= RECALC_INTERVAL) {
            recalcTickCounter = 0;
            frontline.recalcActive(gm.getServer());
        }

        var cpManager = PWP.getCapturePointManager();
        if (cpManager != null) {
            var level = gm.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation("pwp", "map")));
            if (level != null) {
                TeamManager teamManager = PWP.getTeamManager();
                EconomyService svc = getEconomyService();
                if (svc != null) svc.tick(level, teamManager, gm);
                VehicleManager vehicleManager = PWP.getVehicleManager();
                if (vehicleManager != null) {
                    vehicleUpkeepSystem.tick(level, this, vehicleManager);
                }
            }
        }
    }
}
