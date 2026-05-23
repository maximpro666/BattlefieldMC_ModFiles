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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerReviveIntegration {
    private static PlayerReviveIntegration INSTANCE;
    private final Map<UUID, Boolean> previousBleedingState = new ConcurrentHashMap<>();
    private boolean modChecked = false;
    private boolean modPresent = false;

    private final Map<UUID, UUID> downedByAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, String> downedByAttackerName = new ConcurrentHashMap<>();
    private final Set<UUID> bypassBleed = ConcurrentHashMap.newKeySet();

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
                if (player.isAlive() && player.getHealth() > 0f) {
                    String name = player.getName().getString();
                    contrib.addRevive(uuid, name);
                    runtime.addActivity(uuid, BattlefieldRuntime.SCORE_REVIVE);
                    runtime.syncAll(player);
                } else {
                    awardKill(tm, contrib, runtime, game, ticketMgr, player, uuid, server);
                }
                downedByAttacker.remove(uuid);
                downedByAttackerName.remove(uuid);
            }

            if (!wasBleeding && isBleeding) {
                DamageSource lastSrc = player.getLastDamageSource();
                ServerPlayer attacker = null;
                if (lastSrc != null) {
                    attacker = resolveAttacker(lastSrc, server);
                }

                if (attacker != null && !attacker.getUUID().equals(uuid)) {
                    downedByAttacker.put(uuid, attacker.getUUID());
                    downedByAttackerName.put(uuid, attacker.getName().getString());
                }

                if (bypassBleed.remove(uuid)) {
                    awardKill(tm, contrib, runtime, game, ticketMgr, player, uuid, server);
                    downedByAttacker.remove(uuid);
                    downedByAttackerName.remove(uuid);
                    CompoundTag data = player.getPersistentData();
                    data.putBoolean("pwp:instant_death", true);
                    data.remove("playerrevive:bleeding");
                    player.setHealth(0f);
                    player.die(player.damageSources().fellOutOfWorld());
                    previousBleedingState.put(uuid, false);
                    continue;
                }
            }

            previousBleedingState.put(uuid, isBleeding);
        }

        for (UUID uuid : previousBleedingState.keySet()) {
            if (server.getPlayerList().getPlayer(uuid) == null) {
                previousBleedingState.remove(uuid);
                downedByAttacker.remove(uuid);
                downedByAttackerName.remove(uuid);
            }
        }
    }

    private void awardKill(TeamManager tm, ContributionManager contrib, BattlefieldRuntime runtime,
                           GameManager game, TicketManager ticketMgr, ServerPlayer victim,
                           UUID victimUUID, MinecraftServer server) {
        UUID attackerUUID = downedByAttacker.get(victimUUID);
        String attackerName = downedByAttackerName.get(victimUUID);

        tm.incrementDeaths(victimUUID);
        contrib.addDeath(victimUUID, victim.getName().getString());

        if (game != null && game.isPlaying()) {
            Team vTeam = tm.getOrCreatePlayerData(victimUUID).getTeam();
            if (vTeam != null && vTeam.isPlayable() && ticketMgr != null) {
                ticketMgr.deductTicket(vTeam);
            }
        }

        if (attackerUUID != null && !attackerUUID.equals(victimUUID)) {
            ServerPlayer attacker = server.getPlayerList().getPlayer(attackerUUID);
            if (attacker != null) {
                tm.incrementKills(attackerUUID);
                tm.syncPlayerData(attacker);
                contrib.addKill(attackerUUID, attackerName != null ? attackerName : attacker.getName().getString());

                if (game != null && game.isPlaying()) {
                    PWPConfig cfg = PWP.getConfig();
                    int bcReward = cfg != null ? cfg.getKillRewardBC() : 5;
                    runtime.addBC(attackerUUID, bcReward);
                    runtime.addActivity(attackerUUID, BattlefieldRuntime.SCORE_DAMAGE);
                    runtime.syncBC(attacker);
                }
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

    public void finishPlayer(ServerPlayer victim) {
        UUID victimUUID = victim.getUUID();
        if (!isPlayerBleeding(victim)) return;

        TeamManager tm = PWP.getTeamManager();
        ContributionManager contrib = PWP.getContributionManager();
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        GameManager game = PWP.getGameManager();
        TicketManager ticketMgr = PWP.getTicketManager();
        if (tm == null || contrib == null) return;

        awardKill(tm, contrib, runtime, game, ticketMgr, victim, victimUUID, victim.getServer());
        downedByAttacker.remove(victimUUID);
        downedByAttackerName.remove(victimUUID);

        CompoundTag data = victim.getPersistentData();
        data.remove("playerrevive:bleeding");
        data.putBoolean("pwp:finished_death", true);
        previousBleedingState.put(victimUUID, false);

        victim.setHealth(0f);
        victim.die(victim.damageSources().fellOutOfWorld());
    }

    public UUID getDownedBy(UUID victimUUID) {
        return downedByAttacker.get(victimUUID);
    }

    public void markBypassBleed(UUID playerUUID) {
        bypassBleed.add(playerUUID);
    }
}

