package com.pigeostudios.pwp.integration;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerReviveIntegration {
    private static PlayerReviveIntegration INSTANCE;
    private final Map<UUID, Boolean> previousBleedingState = new ConcurrentHashMap<>();
    private boolean modChecked = false;
    private boolean modPresent = false;

    private static final Map<Class<?>, Map<String, Method>> REFLECT_METHOD_CACHE = new HashMap<>();

    private static Method resolveMethod(Class<?> clazz, String name) {
        Map<String, Method> classCache = REFLECT_METHOD_CACHE.get(clazz);
        if (classCache != null) {
            Method m = classCache.get(name);
            if (m != null) return m;
            if (classCache.containsKey(name)) return null;
        }
        try {
            Method m = clazz.getMethod(name);
            REFLECT_METHOD_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(name, m);
            return m;
        } catch (NoSuchMethodException e) {
            REFLECT_METHOD_CACHE.computeIfAbsent(clazz, k -> new HashMap<>()).put(name, null);
            return null;
        }
    }

    public static PlayerReviveIntegration getInstance() {
        if (INSTANCE == null) INSTANCE = new PlayerReviveIntegration();
        return INSTANCE;
    }

    public void tick(MinecraftServer server) {
        if (server == null) return;
        if (!isModPresent()) {
            previousBleedingState.clear();
            return;
        }

        TeamManager tm = PWP.getTeamManager();
        ContributionManager contrib = PWP.getContributionManager();
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        GameManager game = PWP.getGameManager();
        TicketManager ticketMgr = PWP.getTicketManager();
        if (tm == null || contrib == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            boolean isBleeding = isPlayerBleeding(player);
            Boolean wasBleeding = previousBleedingState.getOrDefault(uuid, false);

            if (wasBleeding && !isBleeding) {
                String name = player.getName().getString();
                contrib.addRevive(uuid, name);
                runtime.addActivity(uuid, BattlefieldRuntime.SCORE_REVIVE);
                runtime.syncAll(player);
            }

            if (!wasBleeding && isBleeding) {
                tm.incrementDeaths(uuid);
                contrib.addDeath(uuid, player.getName().getString());

                if (game != null && game.isPlaying()) {
                    Team victimTeam = tm.getOrCreatePlayerData(uuid).getTeam();
                    if (victimTeam != null && victimTeam.isPlayable() && ticketMgr != null) {
                        ticketMgr.deductTicket(victimTeam);
                    }
                }

                DamageSource lastSrc = player.getLastDamageSource();
                if (lastSrc != null) {
                    ServerPlayer attacker = resolveAttacker(lastSrc, server);
                    if (attacker != null && !attacker.getUUID().equals(uuid)) {
                        tm.incrementKills(attacker.getUUID());
                        tm.syncPlayerData(attacker);
                        contrib.addKill(attacker.getUUID(), attacker.getName().getString());

                        if (game != null && game.isPlaying()) {
                            PWPConfig cfg = PWP.getConfig();
                            int bcReward = cfg != null ? cfg.getKillRewardBC() : 5;
                            runtime.addBC(attacker.getUUID(), bcReward);
                            runtime.addActivity(attacker.getUUID(), BattlefieldRuntime.SCORE_DAMAGE);
                            runtime.syncBC(attacker);
                        }
                    }
                }
            }

            previousBleedingState.put(uuid, isBleeding);
        }

        for (UUID uuid : previousBleedingState.keySet()) {
            if (server.getPlayerList().getPlayer(uuid) == null) {
                previousBleedingState.remove(uuid);
            }
        }
    }

    private static ServerPlayer resolveAttacker(DamageSource src, MinecraftServer server) {
        Entity srcEntity = src.getEntity();
        if (srcEntity instanceof ServerPlayer) return (ServerPlayer) srcEntity;

        Entity direct = src.getDirectEntity();
        if (direct instanceof ServerPlayer) return (ServerPlayer) direct;

        if (direct != null) {
            ServerPlayer owner = tryOwnerGetters(direct, server);
            if (owner != null) return owner;
        }

        if (srcEntity != null) {
            ServerPlayer owner = tryOwnerGetters(srcEntity, server);
            if (owner != null) return owner;
        }

        return null;
    }

    private static ServerPlayer tryOwnerGetters(Entity entity, MinecraftServer server) {
        String[] methods = {"getOwner", "getShooter", "getOwnerEntity", "getOwnerUUID", "getOwnerId"};
        Class<?> clazz = entity.getClass();
        for (String name : methods) {
            try {
                Method m = resolveMethod(clazz, name);
                if (m == null) continue;
                Object result = m.invoke(entity);
                if (result instanceof ServerPlayer sp) return sp;
                if (result instanceof UUID uuid) return server.getPlayerList().getPlayer(uuid);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean isPlayerBleeding(ServerPlayer player) {
        try {
            CompoundTag data = player.getPersistentData();
            if (data.contains("playerrevive:bleeding", 10))
                return data.getCompound("playerrevive:bleeding").getBoolean("bleeding");
            return data.getBoolean("playerrevive:bleeding");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isModPresent() {
        try {
            Class.forName("team.creative.playerrevive.PlayerRevive");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isPlayerDowned(ServerPlayer player) {
        if (!isModPresent()) return false;
        return isPlayerBleeding(player);
    }
}
