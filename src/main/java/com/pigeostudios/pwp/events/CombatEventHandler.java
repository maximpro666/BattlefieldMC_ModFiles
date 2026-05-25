package com.pigeostudios.pwp.events;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;

import com.pigeostudios.pwp.network.*;
import com.pigeostudios.pwp.service.EconomyService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatEventHandler {
    private final TeamManager teamManager;
    private static net.minecraft.server.MinecraftServer serverInstance;

    private static final Map<Class<?>, Map<String, Method>> REFLECT_METHOD_CACHE = new HashMap<>();

    private static BufferedWriter killLogWriter;
    private static Path killLogPath;

    private static BufferedWriter getKillLogWriter() {
        if (killLogWriter != null) return killLogWriter;
        try {
            killLogPath = Paths.get("world/pwp/logs/kills.log");
            Files.createDirectories(killLogPath.getParent());
            killLogWriter = Files.newBufferedWriter(killLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            PWP.LOGGER.error("Failed to open kill log: {}", e.getMessage());
        }
        return killLogWriter;
    }

    public static void flushKillLog() {
        if (killLogWriter != null) {
            try { killLogWriter.flush(); } catch (IOException ignored) {}
        }
    }

    public static void closeKillLog() {
        if (killLogWriter != null) {
            try { killLogWriter.close(); } catch (IOException ignored) {}
            killLogWriter = null;
        }
    }

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

    public static void setServer(net.minecraft.server.MinecraftServer server) {
        serverInstance = server;
    }

    // Dedup kill credit across both handlers within the same tick
    private final Set<String> killsThisTick = new HashSet<>();
    private int killsTickGameTime;

    public CombatEventHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    private net.minecraft.server.MinecraftServer getServer(ServerLevel level) {
        return level.getServer();
    }

    private void logKill(String killerName, String victimName, String type) {
        BufferedWriter writer = getKillLogWriter();
        if (writer == null) return;
        try {
            String line = String.format("[%s] %s -> %s (%s)%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                killerName, victimName, type);
            writer.write(line);
        } catch (Exception ignored) {}
    }

    /**
     * Resolves the actual attacker from a DamageSource, handling both direct players
     * and projectiles/entities owned by players. Supports TACZ, SuperbWarfare,
     * and custom damage sources via reflection-based owner detection.
     *
     * @param src The DamageSource to resolve
     * @param level The ServerLevel for player lookups
     * @return The ServerPlayer attacker, or null if not found
     */
    private ServerPlayer resolveAttacker(DamageSource src, ServerLevel level) {
        // 1. Try direct entity from damage source
        Entity srcEntity = src.getEntity();
        if (srcEntity instanceof ServerPlayer) {
            PWP.LOGGER.debug("resolveAttacker: direct entity is ServerPlayer");
            return (ServerPlayer) srcEntity;
        }

        // 2. Try direct entity (projectile, etc.)
        Entity direct = src.getDirectEntity();
        if (direct instanceof ServerPlayer) {
            PWP.LOGGER.debug("resolveAttacker: direct entity is ServerPlayer");
            return (ServerPlayer) direct;
        }

        // 3. Try common owner getter methods
        if (direct != null) {
            ServerPlayer owner = tryCommonOwnerGetters(direct, level);
            if (owner != null) {
                PWP.LOGGER.debug("resolveAttacker: found owner via common getter from {}", direct.getClass().getName());
                return owner;
            }

            // 4. Try reflection-based owner detection
            owner = tryReflectionOwnerDetection(direct, level);
            if (owner != null) {
                PWP.LOGGER.debug("resolveAttacker: found owner via reflection from {}", direct.getClass().getName());
                return owner;
            }
        }

        // 5. Try source entity as owned entity (vehicle explosions, etc.)
        if (srcEntity != null) {
            ServerPlayer owner = tryCommonOwnerGetters(srcEntity, level);
            if (owner != null) return owner;
            owner = tryReflectionOwnerDetection(srcEntity, level);
            if (owner != null) return owner;
        }

        // 6. Try VehicleManager owner lookup for registered vehicle entities
        Entity vehEntity = srcEntity != null ? srcEntity : direct;
        if (vehEntity != null) {
            VehicleManager vm = PWP.getVehicleManager();
            if (vm != null && vm.isSpawnedVehicle(vehEntity.getUUID())) {
                UUID ownerUUID = vm.getVehicleOwner(vehEntity.getUUID());
                if (ownerUUID != null) {
                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
                    if (owner != null) return owner;
                }
            }
        }

        return null;
    }

    /**
     * Attempts to find owner via well-known public methods.
     */
    private ServerPlayer tryCommonOwnerGetters(Entity entity, ServerLevel level) {
        String[] methodNames = {"getOwner", "getShooter", "getOwnerEntity"};
        Class<?> clazz = entity.getClass();

        for (String methodName : methodNames) {
            try {
                Method method = resolveMethod(clazz, methodName);
                if (method == null) continue;
                Object result = method.invoke(entity);

                if (result instanceof ServerPlayer sp) return sp;
                if (result instanceof UUID uuid) {
                    return level.getServer().getPlayerList().getPlayer(uuid);
                }
                if (result instanceof net.minecraft.world.entity.LivingEntity le
                        && le instanceof ServerPlayer sp2) return sp2;
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private ServerPlayer tryReflectionOwnerDetection(Entity entity, ServerLevel level) {
        String[] methodNames = {"getOwnerUUID", "getOwnerId", "getCreator"};
        Class<?> clazz = entity.getClass();

        for (String methodName : methodNames) {
            try {
                Method method = resolveMethod(clazz, methodName);
                if (method == null) continue;
                Object result = method.invoke(entity);

                if (result instanceof UUID) {
                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer((UUID) result);
                    if (owner != null) return owner;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private ServerPlayer resolveAttackerFallback(DamageSource source, ServerLevel level) {
        Entity direct = source.getDirectEntity();
        if (direct == null) return null;
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
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        GameManager game = PWP.getGameManager();
        if (game != null && !game.isPlaying()) {
            event.setCanceled(true);
            return;
        }

        DamageSource source = event.getSource();
        ServerLevel level = (ServerLevel) victim.level();
        ServerPlayer attacker = resolveAttacker(source, level);

        // Anti-abuse #8: remove attacker's invulnerability on attack
        if (attacker != null) {
            attacker.getPersistentData().remove("pwp:spawn_invuln");
            attacker.getPersistentData().remove("pwp:spawn_invuln_ticks");
        }

        GameManager gm = PWP.getGameManager();
        if (gm != null && gm.isInOwnBase(victim)) {
            event.setCanceled(true);
            return;
        }

        // Anti-abuse #8: protect victim with spawn invulnerability
        if (victim.getPersistentData().getBoolean("pwp:spawn_invuln")) {
            event.setCanceled(true);
            return;
        }

        PWPConfig pwpCfg = PWP.getConfig();
        boolean bleedingEnabled = pwpCfg != null && pwpCfg.isBleedingEnabled();

        // Bleeding player being attacked — finish or cancel damage
        if (bleedingEnabled && com.pigeostudios.pwp.bleeding.BleedingHandler.isBleeding(victim)) {
            if (attacker != null && attacker != victim
                    && !teamManager.isFriendly(attacker, victim)) {
                event.setCanceled(true);
                com.pigeostudios.pwp.bleeding.BleedingHandler.getInstance()
                    .finishPlayer(victim, attacker, source);
                return;
            }
            event.setCanceled(true);
            return;
        }

        // Lethal damage — bleed if possible, die if bypass
        if (bleedingEnabled && victim.getHealth() - event.getAmount() <= 0f
                && !shouldBypassBleeding(source, victim)) {
            if (attacker == null || attacker == victim
                    || !teamManager.isFriendly(attacker, victim)) {
                event.setCanceled(true);
                ServerPlayer bleedAttacker = (attacker == victim || !(attacker instanceof ServerPlayer)) ? null : (ServerPlayer) attacker;
                com.pigeostudios.pwp.bleeding.BleedingHandler.getInstance()
                    .startBleeding(victim, bleedAttacker, source);
                return;
            }
        }

        if (attacker != null) {
            if (attacker != victim && teamManager.isFriendly(attacker, victim)) {
                event.setCanceled(true);
                PWP.LOGGER.debug("Blocked friendly fire from {} to {}",
                    attacker.getName().getString(),
                    victim.getName().getString());
                return;
            }
        }

        SquadmateRespawnCooldownManager.onPlayerAttacked(victim);
    }

    private boolean isHeadShot(DamageSource source, ServerPlayer victim) {
        Entity direct = source.getDirectEntity();
        if (direct == null || direct instanceof ServerPlayer) return false;
        // 1) TACZ / SuperbWarfare headshot flag via reflection
        String[] headshotMethods = {"isHeadShot", "getIsHeadShot"};
        for (String name : headshotMethods) {
            try {
                Method m = resolveMethod(direct.getClass(), name);
                if (m != null && m.getReturnType() == boolean.class) {
                    return (boolean) m.invoke(direct);
                }
            } catch (Exception ignored) {}
        }
        // 2) Fallback: Y‑position relative to eye height
        double projY = direct.getY();
        double eyeY = victim.getEyeY();
        return Math.abs(projY - eyeY) < 0.4;
    }

    private boolean shouldBypassBleeding(DamageSource source, ServerPlayer victim) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return true;
        if (isHeadShot(source, victim)) return true;
        Entity direct = source.getDirectEntity();
        if (direct != null) {
            VehicleManager vm = PWP.getVehicleManager();
            if (vm != null && vm.isVehicleEntityType(direct)) return true;
        }
        PWPConfig cfg = PWP.getConfig();
        if (cfg != null && cfg.getBleedingBypassSources() != null) {
            String srcName = source.getMsgId();
            for (String bypass : cfg.getBleedingBypassSources()) {
                if (srcName.equals(bypass.strip())) return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (event.isCanceled()) return;

        DamageSource source = event.getSource();
        ServerLevel level = (ServerLevel) victim.level();
        ServerPlayer killer = resolveAttacker(source, level);

        GameManager game = PWP.getGameManager();
        boolean isPlaying = game != null && game.isPlaying();

        // RedeployCommand sets this flag to prevent double-counting deaths
        boolean deathAlreadyCounted = victim.getPersistentData().getBoolean("pwp:instant_death");
        victim.getPersistentData().remove("pwp:instant_death");

        // Bleedout death (timer expired) — credit the original attacker who downed the player
        if (!deathAlreadyCounted && victim.getPersistentData().hasUUID("pwp:bleedout_attacker")) {
            UUID downerUUID = victim.getPersistentData().getUUID("pwp:bleedout_attacker");
            victim.getPersistentData().remove("pwp:bleedout_attacker");
            victim.getPersistentData().remove("pwp:bleedout_attacker_name");
            ServerPlayer downer = level.getServer().getPlayerList().getPlayer(downerUUID);
            if (downer != null && !downer.getUUID().equals(victim.getUUID())
                    && !teamManager.isFriendly(downer, victim)) {
                killer = downer;
            }
        }

        // Clean up bleeding state if player died while bleeding (edge cases)
        PWPConfig deathCfg = PWP.getConfig();
        if (deathCfg != null && deathCfg.isBleedingEnabled()) {
            var bleedHandler = com.pigeostudios.pwp.bleeding.BleedingHandler.getInstance();
            if (bleedHandler != null) {
                bleedHandler.removePlayer(victim);
            }
        }

        if (!deathAlreadyCounted) {
            teamManager.incrementDeaths(victim.getUUID());

            ContributionManager contrib = PWP.getContributionManager();
            if (contrib != null && isPlaying) {
                contrib.addDeath(victim.getUUID(), victim.getName().getString());
            }

            Team victimTeam = teamManager.getOrCreatePlayerData(victim.getUUID()).getTeam();
            if (victimTeam.isPlayable() && isPlaying) {
                var ticketSvc = PWP.getServiceRegistry().getTickets();
                if (ticketSvc != null) ticketSvc.deductTicket(victimTeam);
            }

            if (killer != null && !killer.getUUID().equals(victim.getUUID())) {
                if (!isDedupKill(killer, victim)) return;
                teamManager.incrementKills(killer.getUUID());
                teamManager.syncPlayerData(killer);

                if (contrib != null && isPlaying) {
                    contrib.addKill(killer.getUUID(), killer.getName().getString());
                }

                EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
                PWPConfig cfg = PWP.getConfig();
                int bcReward = cfg != null ? cfg.getKillRewardBC() : 5;

                if (isPlaying) {
                    // M1: kill-farm cooldown — skip BC if killing the same victim too fast
                    var aa = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getAntiAbuse() : null;
                    boolean passKillFarm = aa == null || aa.checkKillFarm(killer, victim.getUUID());
                    if (passKillFarm) {
                        if (eco != null) {
                            eco.addBCAndActivity(killer.getUUID(), bcReward, BattlefieldRuntime.SCORE_DAMAGE);
                            eco.syncBC(killer);
                        } else {
                            BattlefieldRuntime.getInstance().addBC(killer.getUUID(), bcReward);
                            BattlefieldRuntime.getInstance().addActivity(killer.getUUID(), BattlefieldRuntime.SCORE_DAMAGE);
                            BattlefieldRuntime.getInstance().syncBC(killer);
                        }
                    }
                }

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("killer", killer.getName().getString());
                placeholders.put("victim", victim.getName().getString());
                placeholders.put("bc", String.valueOf(bcReward));

                victim.sendSystemMessage(Component.translatable("pwp.chat.combat.killed_by",
                    killer.getName().getString(), (int) victim.distanceTo(killer)), false);
                logKill(killer.getName().getString(), victim.getName().getString(), "player");
                killer.sendSystemMessage(
                    Component.literal("§6" + victim.getName().getString() + " §a"
                        + (cfg != null ? cfg.getMessage("kill_reward", placeholders)
                        : "+" + bcReward + " BC")), false);

                Rank oldRank = Rank.fromKills(teamManager.getOrCreatePlayerData(killer.getUUID()).getKills() - 1);
                Rank newRank = Rank.fromKills(teamManager.getOrCreatePlayerData(killer.getUUID()).getKills());
                if (oldRank.ordinal() < newRank.ordinal()) {
                    teamManager.setPlayerRank(killer, newRank.ordinal());
                    Map<String, String> rankPl = new HashMap<>();
                    rankPl.put("rank", newRank.getDisplayName());
                    killer.sendSystemMessage(
                        cfg != null
                            ? Component.literal(cfg.getMessage("rank_up", rankPl))
                            : Component.translatable("pwp.chat.combat.rank_up", newRank.getDisplayName()), false);
                }
            }
        }

        victim.getPersistentData().remove("pwp:finished_death");
        victim.getPersistentData().remove("pwp:direct_kill");

        if (isPlaying) {
            event.setCanceled(true);
            onPlayerDeath(victim, killer);
            instantRespawn(victim);
        }
        teamManager.syncPlayerData(victim);
    }

    private void onPlayerDeath(ServerPlayer victim, ServerPlayer killer) {
        victim.getInventory().clearContent();
    }

    private boolean isDedupKill(ServerPlayer killer, net.minecraft.world.entity.Entity victim) {
        int gameTime = 0;
        if (killer != null && killer.serverLevel() != null) {
            gameTime = (int) killer.serverLevel().getGameTime();
        }
        if (gameTime != killsTickGameTime) {
            killsThisTick.clear();
            killsTickGameTime = gameTime;
        }
        String key = killer.getUUID().toString() + ":" + victim.getUUID().toString();
        if (killsThisTick.contains(key)) return false;
        killsThisTick.add(key);
        return true;
    }

    private void instantRespawn(ServerPlayer player) {
        GameManager game = PWP.getGameManager();
        if (game == null) {
            player.setHealth(player.getMaxHealth());
            return;
        }
        player.setHealth(player.getMaxHealth());
        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

        player.getPersistentData().remove("pwp:direct_kill");

        game.teleportPlayerToLobby(player);
        openSpawnSelectionScreen(player);
    }

    // Anti-abuse #8: spawn invulnerability — called after player actually spawns into the world
    public static void applySpawnInvulnerability(ServerPlayer player) {
        PWPConfig cfg = PWP.getConfig();
        int ticks = cfg != null ? cfg.getSpawnProtectionTicks() : 100;
        if (ticks <= 0) return;
        player.getPersistentData().putInt("pwp:spawn_invuln_ticks", ticks);
        player.getPersistentData().putBoolean("pwp:spawn_invuln", true);
    }

    public static void openSpawnSelectionScreen(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        GameManager game = PWP.getGameManager();
        if (game == null || !game.isPlaying()) return;

        TeamManager tm = PWP.getTeamManager();
        if (tm == null) return;
        Team team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();

        List<OpenSpawnSelectionScreenPacket.SquadmateInfo> squadmates = new java.util.ArrayList<>();
        SquadManager squadManager = PWP.getSquadManager();
        if (squadManager != null) {
            Squad squad = squadManager.getPlayerSquad(player.getUUID());
            if (squad != null) {
                for (UUID memberId : squad.getMembers()) {
                    if (memberId.equals(player.getUUID())) continue;
                    ServerPlayer member = server.getPlayerList().getPlayer(memberId);
                    if (member == null) continue;
                    String callsign = tm.getOrCreatePlayerData(memberId).getCallsign();
                    int memberTeam = tm.getOrCreatePlayerData(memberId).getTeam().ordinal();
                    int cd = SquadmateRespawnCooldownManager.getSquadmateCooldownTicks(
                            member.serverLevel(), memberId);
                    squadmates.add(new OpenSpawnSelectionScreenPacket.SquadmateInfo(
                            memberId, callsign, memberTeam, cd));
                }
            }
        }

        List<com.pigeostudios.pwp.client.FOBData> fobList = new java.util.ArrayList<>();
        FOBManager fobManager = PWP.getFOBManager();
        if (fobManager != null && team != null && team.isPlayable()) {
            for (FOBManager.SavedFOB fob : fobManager.getFOBs()) {
                if (fob.teamOrdinal == team.ordinal()) {
                    fobList.add(new com.pigeostudios.pwp.client.FOBData(
                            fob.fobId, fob.name, fob.x, fob.y, fob.z,
                            fob.dimension, fob.teamOrdinal, fob.health));
                }
            }
        }

        List<OpenSpawnSelectionScreenPacket.BeaconInfo> beacons = new java.util.ArrayList<>();
        RespawnManager respawnManager = PWP.getRespawnManager();
        if (respawnManager != null) {
            for (RespawnManager.SavedBeacon b : respawnManager.getBeaconsInDimension(
                    player.serverLevel().dimension().location().toString())) {
                if (java.util.Objects.equals(b.uuid, player.getUUID().toString())) {
                    beacons.add(new OpenSpawnSelectionScreenPacket.BeaconInfo(
                            b.name, b.x, b.y, b.z, b.teamOrdinal));
                }
            }
        }

        int teamOrd = team != null ? team.ordinal() : 2;

        String selectedKit = tm.getOrCreatePlayerData(player.getUUID()).getSelectedKit();
        PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new OpenSpawnSelectionScreenPacket(squadmates, fobList, beacons, teamOrd, selectedKit));
    }

    @SubscribeEvent
    public void onLivingDeathEntity(LivingDeathEvent event) {
        net.minecraft.world.entity.Entity ent = event.getEntity();
        if (ent instanceof ServerPlayer) return;
        if (!(ent instanceof LivingEntity le)) return;
        if (le.level().isClientSide) return;

        GameManager game = PWP.getGameManager();
        if (game == null || !game.isPlaying()) return;

        VehicleManager vm = PWP.getVehicleManager();
        if (vm == null) return;
        if (!vm.isVehicleEntityType(ent)) return;

        ServerLevel level = (ServerLevel) le.level();
        ServerPlayer killer = resolveAttacker(event.getSource(), level);
        if (killer == null) return;
        if (!isDedupKill(killer, le)) return;

        // C5 fix: check team — don't reward killing your own team's vehicle
        UUID vehicleOwnerId = vm.getVehicleOwner(le.getUUID());
        if (vehicleOwnerId != null) {
            Team killerTeam = teamManager.getOrCreatePlayerData(killer.getUUID()).getTeam();
            Team ownerTeam = teamManager.getOrCreatePlayerData(vehicleOwnerId).getTeam();
            if (killerTeam == ownerTeam && killerTeam.isPlayable()) return;
        }

        PWPConfig cfg = PWP.getConfig();
        int vBcReward = cfg != null ? cfg.getVehicleKillRewardBC() : 10;

        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        if (eco != null) {
            eco.addBCAndActivity(killer.getUUID(), vBcReward, BattlefieldRuntime.SCORE_DAMAGE);
            eco.syncBC(killer);
        } else {
            BattlefieldRuntime.getInstance().addBC(killer.getUUID(), vBcReward);
            BattlefieldRuntime.getInstance().addActivity(killer.getUUID(), BattlefieldRuntime.SCORE_DAMAGE);
            BattlefieldRuntime.getInstance().syncBC(killer);
        }

        String name = le.hasCustomName() ? le.getCustomName().getString() : le.getType().getDescription().getString();
        Map<String, String> vPl = new HashMap<>();
        vPl.put("killer", killer.getName().getString());
        vPl.put("vehicle", name);
        vPl.put("bc", String.valueOf(vBcReward));
        String vMsg = "\u2699 " + name + " \u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d\u0430 \u043a\u043e\u043c\u0430\u043d\u0434\u043e\u0439 " + killer.getName().getString();
        NotificationPacket vPkt = new NotificationPacket(vMsg, "warning", 4000, "");
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), vPkt);
        logKill(killer.getName().getString(), name, "vehicle");
        NotificationPacket rewardPkt = new NotificationPacket("+" + vBcReward + " BC \u0437\u0430 \u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d\u0438\u0435 \u0442\u0435\u0445\u043d\u0438\u043a\u0438", "success", 3000, "");
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> killer), rewardPkt);
        if (vm != null) vm.unregisterSpawnedVehicle(ent.getUUID());
        BattlefieldRuntime.getInstance().trackVehicleDestroy(ent.getUUID());

        var tkSvc = PWP.getServiceRegistry().getTickets();
        if (tkSvc != null && vehicleOwnerId != null) {
            Team ownerTeam = teamManager.getOrCreatePlayerData(vehicleOwnerId).getTeam();
            if (ownerTeam.isPlayable()) tkSvc.deductVehicleLossCost(ownerTeam);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        GameManager game = PWP.getGameManager();
        if (game == null || !game.isPlaying()) return;
        Level level = event.getLevel();
        if (level.isClientSide) return;
        // Copy and clear so vanilla won't re-destroy, then destroy blocks without drops
        GameManager gm = PWP.getGameManager();
        List<BlockPos> blocks = List.copyOf(event.getExplosion().getToBlow());
        event.getExplosion().clearToBlow();
        for (BlockPos pos : blocks) {
            if (gm != null && gm.isInAnyBase(pos)) continue;
            // Clean up FOBs destroyed by explosion (BreakEvent is not fired)
            if (level instanceof ServerLevel serverLevel) {
                String dim = serverLevel.dimension().location().toString();
                FOBManager fobManager = PWP.getFOBManager();
                if (fobManager != null) {
                    FOBManager.SavedFOB fob = fobManager.getFOBAt(pos, dim);
                    if (fob != null) {
                        fobManager.removeFOB(fob.fobId);
                        NotificationPacket fobPkt = new NotificationPacket("\u2699 FOB " + fob.name + " \u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d\u0430!", "error", 4000, "");
                        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), fobPkt);
                    }
                }
            }
            level.destroyBlock(pos, false);
        }
    }

    // Anti-abuse #4/#5: heal tracking for rate-limit and reward
    private static final int HEAL_RATE_LIMIT = 5;
    private static final long HEAL_WINDOW_MS = 60_000;
    private final Map<UUID, List<Long>> healTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastPlayerHealth = new ConcurrentHashMap<>();

    private void tickHealTracking() {
        if (serverInstance == null) return;
        long now = System.currentTimeMillis();
        GameManager gm = PWP.getGameManager();
        boolean isPlaying = gm != null && gm.isPlaying();
        EconomyService eco = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getEconomy() : null;
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            float currentHp = player.getHealth();
            Float lastHp = lastPlayerHealth.get(uuid);
            if (isPlaying && lastHp != null && currentHp > lastHp + 0.001f) {
                float healed = currentHp - lastHp;
                // Skip small regen ticks (natural regen is 0.5 HP)
                if (healed < 1.5f) continue;
                ServerPlayer healer = findNearestFriendlyHealer(player, gm);
                if (healer != null && healer != player && eco != null) {
                    var cfg = PWP.getServiceRegistry() != null ? PWP.getServiceRegistry().getConfig() : null;
                    int reward = cfg != null ? cfg.getHealRewardBC() : 1;
                    List<Long> timestamps = healTimestamps.get(healer.getUUID());
                    if (timestamps == null) {
                        timestamps = new ArrayList<>();
                        healTimestamps.put(healer.getUUID(), timestamps);
                    }
                    timestamps.removeIf(t -> now - t > HEAL_WINDOW_MS);
                    if (timestamps.size() < HEAL_RATE_LIMIT) {
                        timestamps.add(now);
                        eco.addBCAndActivity(healer.getUUID(), reward, BattlefieldRuntime.SCORE_HEAL);
                        eco.syncBC(healer);
                    }
                }
            }
            lastPlayerHealth.put(uuid, currentHp);
        }
    }

    private ServerPlayer findNearestFriendlyHealer(ServerPlayer patient, GameManager gm) {
        ServerPlayer best = null;
        double bestDist = 10.0;
        for (ServerPlayer p : serverInstance.getPlayerList().getPlayers()) {
            if (p == patient) continue;
            if (!teamManager.isFriendly(p, patient)) continue;
            double dist = patient.distanceTo(p);
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private int squadmateSyncTickCounter = 0;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        // Anti-abuse #8: tick spawn invulnerability timers
        if (serverInstance != null) {
            for (ServerPlayer p : serverInstance.getPlayerList().getPlayers()) {
                if (p.getPersistentData().getBoolean("pwp:spawn_invuln")) {
                    int ticks = p.getPersistentData().getInt("pwp:spawn_invuln_ticks") - 1;
                    if (ticks <= 0) {
                        p.getPersistentData().remove("pwp:spawn_invuln");
                        p.getPersistentData().remove("pwp:spawn_invuln_ticks");
                    } else {
                        p.getPersistentData().putInt("pwp:spawn_invuln_ticks", ticks);
                    }
                }
            }
            // Anti-abuse #4/#5: heal reward tracking
            tickHealTracking();
        }

        squadmateSyncTickCounter++;
        if (squadmateSyncTickCounter < 40) return;
        squadmateSyncTickCounter = 0;

        if (serverInstance == null) return;

        List<SquadmateStatusSyncPacket.SyncEntry> entries = new java.util.ArrayList<>();
        for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
            int cd = SquadmateRespawnCooldownManager.getSquadmateCooldownTicks(
                    player.serverLevel(), player.getUUID());
            if (cd > 0) {
                entries.add(new SquadmateStatusSyncPacket.SyncEntry(player.getUUID(), cd));
            }
        }
        if (!entries.isEmpty()) {
            PacketHandler.CHANNEL.send(
                    PacketDistributor.ALL.noArg(),
                    new SquadmateStatusSyncPacket(entries));
        }
    }
}
