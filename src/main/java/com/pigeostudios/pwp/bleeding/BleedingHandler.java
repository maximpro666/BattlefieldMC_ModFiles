package com.pigeostudios.pwp.bleeding;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.network.*;
import com.pigeostudios.pwp.service.EconomyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BleedingHandler {
    private static BleedingHandler INSTANCE;

    private final Map<UUID, BleedingState> bleedingPlayers = new ConcurrentHashMap<>();
    private final TeamManager teamManager;

    public BleedingHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
        INSTANCE = this;
    }

    public static BleedingHandler getInstance() {
        return INSTANCE;
    }

    public static boolean isBleeding(Player player) {
        return INSTANCE != null && INSTANCE.bleedingPlayers.containsKey(player.getUUID());
    }

    public static BleedingState getState(Player player) {
        return INSTANCE != null ? INSTANCE.bleedingPlayers.get(player.getUUID()) : null;
    }

    public void startBleeding(ServerPlayer victim, ServerPlayer attacker, DamageSource source) {
        PWPConfig cfg = PWP.getConfig();
        BleedingState state = new BleedingState(victim, attacker, source);
        state.bleedTimeRemaining = cfg.getBleedoutTimeSeconds() * 20;
        state.reviveThreshold = cfg.getReviveTimeSeconds() * 20;
        bleedingPlayers.put(victim.getUUID(), state);

        victim.setHealth((float) cfg.getBleedingHealth());
        victim.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);

        victim.setCustomName(net.minecraft.network.chat.Component.literal("\u00a7c\u271a \u0422\u0420\u0415\u0411\u0423\u0415\u0422 \u041f\u041e\u041c\u041e\u0429\u0418"));
        victim.setCustomNameVisible(true);

        syncToClient(victim);
    }

    private void clearBleeding(ServerPlayer player) {
        player.setCustomName(null);
        player.setCustomNameVisible(false);
        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new BleedingSyncPacket(player.getUUID(), false, 0, null, 0));
    }

    private void doRevive(ServerPlayer player) {
        BleedingState state = bleedingPlayers.remove(player.getUUID());
        if (state == null) return;

        if (state.reviverUUID != null) {
            ContributionManager contrib = PWP.getContributionManager();
            if (contrib != null) {
                String name = player.getServer().getPlayerList().getPlayer(state.reviverUUID) != null
                    ? player.getServer().getPlayerList().getPlayer(state.reviverUUID).getName().getString()
                    : "unknown";
                contrib.addRevive(state.reviverUUID, name);
            }
            EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
            if (eco != null) {
                ServerPlayer reviver = player.getServer().getPlayerList().getPlayer(state.reviverUUID);
                if (reviver != null) {
                    eco.addBCAndActivity(reviver.getUUID(), 10, BattlefieldRuntime.SCORE_HEAL);
                    eco.syncBC(reviver);
                }
            }
        }

        clearBleeding(player);

        PWPConfig cfg = PWP.getConfig();
        player.setHealth((float) cfg.getHealthAfterRevive());
        player.setForcedPose(null);
    }

    private void doBleedoutDeath(ServerPlayer player) {
        BleedingState state = bleedingPlayers.remove(player.getUUID());
        if (state == null) return;

        clearBleeding(player);

        player.getPersistentData().putUUID("pwp:bleedout_attacker",
            state.attackerUUID != null ? state.attackerUUID : player.getUUID());
        player.getPersistentData().putString("pwp:bleedout_attacker_name", state.attackerName);

        player.setForcedPose(null);
        player.die(player.damageSources().fellOutOfWorld());
    }

    public void finishPlayer(ServerPlayer victim, ServerPlayer attacker, DamageSource source) {
        BleedingState state = bleedingPlayers.remove(victim.getUUID());
        if (state == null) return;

        clearBleeding(victim);
        victim.setForcedPose(null);
        victim.die(source);
    }

    public void syncToClient(ServerPlayer player) {
        BleedingState state = bleedingPlayers.get(player.getUUID());
        if (state == null) return;

        int bleedTime = state.isBleeding() ? state.bleedTimeRemaining : 0;
        UUID reviverId = state.reviverUUID;
        int reviveProg = state.reviveProgress;

        PacketHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new BleedingSyncPacket(player.getUUID(), true, bleedTime, reviverId, reviveProg));
    }

    // === Event Handlers ===

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        GameManager game = PWP.getGameManager();
        if (game != null && !game.isPlaying()) return;

        PWPConfig cfg = PWP.getConfig();
        if (cfg == null || !cfg.isBleedingEnabled()) return;

        DamageSource source = event.getSource();
        ServerLevel level = (ServerLevel) victim.level();
        ServerPlayer attacker = resolveAttacker(source, level);

        BleedingState state = bleedingPlayers.get(victim.getUUID());
        if (state != null && state.isBleeding()) {
            if (attacker != null && attacker != victim
                    && !teamManager.isFriendly(attacker, victim)) {
                event.setCanceled(true);
                finishPlayer(victim, attacker, source);
            } else {
                event.setCanceled(true);
            }
            return;
        }

        // Bleeding player attacking → cancel
        if (attacker != null && bleedingPlayers.containsKey(attacker.getUUID())) {
            event.setCanceled(true);
        }
    }

    public BleedingState removePlayer(ServerPlayer player) {
        BleedingState state = bleedingPlayers.remove(player.getUUID());
        if (state != null) {
            player.setCustomName(null);
            player.setCustomNameVisible(false);
            player.setForcedPose(null);
            PacketHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BleedingSyncPacket(player.getUUID(), false, 0, null, 0));
        }
        return state;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        BleedingState state = bleedingPlayers.get(player.getUUID());
        if (state == null || !state.isBleeding()) return;

        player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);

        state.bleedTimeRemaining--;
        state.downedTime++;

        if (state.isBleeding() && state.reviverUUID != null) {
            ServerPlayer reviver = player.getServer().getPlayerList().getPlayer(state.reviverUUID);
            if (reviver != null && reviver.isAlive()
                    && reviver.distanceTo(player) <= 5.0
                    && teamManager.isFriendly(reviver, player)) {
                state.reviveProgress++;
            } else {
                state.reviverUUID = null;
                state.reviveProgress = 0;
                syncToClient(player);
            }
        }

        if (state.isRevived()) {
            doRevive(player);
            return;
        }

        if (state.hasBledOut()) {
            doBleedoutDeath(player);
            return;
        }

        if (state.downedTime % 5 == 0) {
            syncToClient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!bleedingPlayers.containsKey(player.getUUID())) return;
        // Allow EntityInteract (used for revive), block everything else
        if (event instanceof PlayerInteractEvent.EntityInteract) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;
        if (!(event.getEntity() instanceof ServerPlayer helper)) return;

        BleedingState state = bleedingPlayers.get(target.getUUID());
        if (state == null || !state.isBleeding()) return;

        event.setCanceled(true);

        if (!teamManager.isFriendly(helper, target)) return;

        state.reviverUUID = helper.getUUID();
        state.reviveProgress = 0;
        syncToClient(target);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BleedingState state = bleedingPlayers.remove(player.getUUID());
        if (state == null) return;

        player.setCustomName(null);
        player.setCustomNameVisible(false);
        player.setForcedPose(null);
    }

    public void reset() {
        bleedingPlayers.clear();
    }

    // === Helpers ===

    private ServerPlayer resolveAttacker(DamageSource src, ServerLevel level) {
        Entity srcEntity = src.getEntity();
        if (srcEntity instanceof ServerPlayer) return (ServerPlayer) srcEntity;

        Entity direct = src.getDirectEntity();
        if (direct instanceof ServerPlayer) return (ServerPlayer) direct;

        if (direct != null) {
            try {
                java.lang.reflect.Method m = direct.getClass().getMethod("getOwner");
                Object result = m.invoke(direct);
                if (result instanceof ServerPlayer) return (ServerPlayer) result;
                if (result instanceof UUID) return level.getServer().getPlayerList().getPlayer((UUID) result);
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Method m = direct.getClass().getMethod("getShooter");
                Object result = m.invoke(direct);
                if (result instanceof ServerPlayer) return (ServerPlayer) result;
            } catch (Exception ignored) {}
            try {
                java.lang.reflect.Method m = direct.getClass().getMethod("getOwnerUUID");
                Object result = m.invoke(direct);
                if (result instanceof UUID) return level.getServer().getPlayerList().getPlayer((UUID) result);
            } catch (Exception ignored) {}
        }
        if (srcEntity != null) {
            try {
                java.lang.reflect.Method m = srcEntity.getClass().getMethod("getOwner");
                Object result = m.invoke(srcEntity);
                if (result instanceof ServerPlayer) return (ServerPlayer) result;
                if (result instanceof UUID) return level.getServer().getPlayerList().getPlayer((UUID) result);
            } catch (Exception ignored) {}
        }
        return null;
    }

}
