package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.ammo.AmmoCooldownManager;
import com.yourmod.teamsystem.ammo.AmmoService;
import com.yourmod.teamsystem.integration.ReviveMeIntegration;
import com.yourmod.teamsystem.state.*;
import com.yourmod.teamsystem.system.*;
import com.yourmod.teamsystem.vehicle.VehicleDefinition;
import com.yourmod.teamsystem.vehicle.VehicleDefinitionRegistry;
import com.yourmod.teamsystem.vehicle.adapter.VehicleAdapterRegistry;
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
    public final EconomyState economy = new EconomyState();
    public final FrontlineState frontline = new FrontlineState();
    public final PressureState pressure = new PressureState();
    public final VehicleState vehicles = new VehicleState();
    public final MatchState match = new MatchState();

    // ===== System objects =====
    private final ActivitySystem activitySystem = new ActivitySystem();
    private final PressureSystem pressureSystem = new PressureSystem();
    private final EconomySystem economySystem = new EconomySystem();
    private final EscalationSystem escalationSystem = new EscalationSystem();
    private final VehicleUpkeepSystem vehicleUpkeepSystem = new VehicleUpkeepSystem();

    // ===== Экономика: BC =====
    public int getBC(UUID uuid) { return economy.getBC(uuid); }
    public int getWC(UUID uuid) { return economy.getWC(uuid); }

    public void setBC(UUID uuid, int amount) { economy.setBC(uuid, amount); }
    public void setWC(UUID uuid, int amount) { economy.setWC(uuid, amount); }

    public void addBC(UUID uuid, int amount) { economy.addBC(uuid, amount); }

    public void addWC(UUID uuid, int amount) {
        economy.addWC(uuid, amount);
        saveWCBatch(uuid);
    }

    public boolean deductBC(UUID uuid, int amount) {
        return economy.deductBC(uuid, amount);
    }

    public boolean deductWC(UUID uuid, int amount) {
        boolean ok = economy.deductWC(uuid, amount);
        if (ok) saveWCBatch(uuid);
        return ok;
    }

    private void saveWCBatch(UUID uuid) {
        TeamManager tm = TeamSystem.getTeamManager();
        if (tm == null) return;
        PlayerCombatData data = tm.getOrCreatePlayerData(uuid);
        data.setWarCredits(economy.getWC(uuid));
        tm.setDirty();
    }

    public void loadFromTeamManager() {
        TeamManager tm = TeamSystem.getTeamManager();
        if (tm == null) return;
        var dataCopy = tm.getPlayerDataCopy();
        for (Map.Entry<UUID, PlayerCombatData> entry : dataCopy.entrySet()) {
            int bc = entry.getValue().getBattleCredits();
            int wc = entry.getValue().getWarCredits();
            if (bc > 0) economy.setBC(entry.getKey(), bc);
            if (wc > 0) economy.setWC(entry.getKey(), wc);
        }
    }

    // ===== Vehicle Credits =====
    public int getVC(Team team) { return economy.getVC(team); }

    public boolean deductVC(Team team, int amount) {
        return economy.deductVC(team, amount);
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

    // ===== Dynamic Pressure =====
    public double getPressure(Team team, PressureType type) {
        return pressure.get(team, type);
    }

    public void addPressure(UUID vehicleUUID, Team team, VehicleDefinition def) {
        pressureSystem.onVehicleSpawn(pressure, vehicles, vehicleUUID, team, def);
    }

    public void removePressure(UUID vehicleUUID, Team team, VehicleDefinition def) {
        Team t = vehicles.getVehicleTeam(vehicleUUID);
        String defId = vehicles.getVehicleDefId(vehicleUUID);
        if (t == null || defId == null) return;
        pressureSystem.onVehicleDestroy(pressure, vehicles, vehicleUUID, vehicleDefRegistry);
    }

    public boolean isHighPressure(Team team, PressureType type) {
        return pressure.isHigh(team, type);
    }

    // ===== War Escalation =====
    public EscalationPhase getCurrentEscalation() { return match.getCurrentEscalation(); }
    public double getEscalationProgress() { return match.getEscalationProgress(); }

    public boolean isVehicleAllowed(VehicleDefinition def) {
        return match.isVehicleAllowed(def);
    }

    public boolean isPhaseAtLeast(EscalationPhase phase) {
        return match.isPhaseAtLeast(phase);
    }

    // ===== Active Frontline Players =====
    public int getActiveFrontlineCount() { return frontline.getActiveCount(); }

    // ===== Vehicle-to-Pressure Tracking =====
    public void trackVehicleSpawn(UUID vehicleUUID, Team team, String defId) {
        VehicleDefinition def = vehicleDefRegistry.get(defId);
        if (def != null) {
            pressureSystem.onVehicleSpawn(pressure, vehicles, vehicleUUID, team, def);
        }
    }

    public void trackVehicleDestroy(UUID vehicleUUID) {
        pressureSystem.onVehicleDestroy(pressure, vehicles, vehicleUUID, vehicleDefRegistry);
    }

    // ===== Sync to clients =====
    public void syncBC(ServerPlayer player) {
        com.yourmod.teamsystem.network.PacketHandler.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.yourmod.teamsystem.network.BCSyncPacket(economy.getBC(player.getUUID())));
    }

    public void syncWC(ServerPlayer player) {
        com.yourmod.teamsystem.network.PacketHandler.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.yourmod.teamsystem.network.WCSyncPacket(economy.getWC(player.getUUID())));
    }

    public void syncAll(ServerPlayer player) {
        syncBC(player);
        syncWC(player);
    }

    public void syncBCToAll() {
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
            syncBC(player);
        }
    }

    // ===== Reset =====
    public void resetMatch() {
        economy.resetMatch();
        frontline.reset();
        pressure.reset();
        vehicles.reset();
        match.reset();
        activitySystem.reset();
        pressureSystem.reset();
        economySystem.reset();
        vehicleUpkeepSystem.reset();
        ammoService.clear();
        ammoCooldownManager.resetAll();
    }

    public void resetForPlayer(UUID uuid) {
        economy.resetForPlayer(uuid);
        frontline.resetForPlayer(uuid);
        ammoCooldownManager.resetPlayer(uuid);
    }

    // ===== Единый тик =====
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        GameManager gm = TeamSystem.getGameManager();
        if (gm == null || !gm.isPlaying()) return;

        activitySystem.tick(frontline);
        pressureSystem.tick(pressure);

        TicketManager tm = TeamSystem.getTicketManager();
        escalationSystem.tick(match, pressure, tm, gm);
        frontline.recalcActive(gm.getServer());

        if (ReviveMeIntegration.isModPresent()) {
            ReviveMeIntegration.getInstance().tick(gm.getServer());
        }

        var cpManager = TeamSystem.getCapturePointManager();
        if (cpManager != null) {
            var level = gm.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation("teamsystem", "map")));
            if (level != null) {
                TeamManager teamManager = TeamSystem.getTeamManager();
                economySystem.tick(level, economy, frontline, gm, teamManager);
                VehicleManager vehicleManager = TeamSystem.getVehicleManager();
                if (vehicleManager != null) {
                    vehicleUpkeepSystem.tick(level, this, vehicleManager);
                }
            }
        }
    }
}
