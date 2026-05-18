package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.ContributionManager;
import com.yourmod.teamsystem.core.EconomyManager;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.core.TeamSystemConfig;
import com.yourmod.teamsystem.core.TicketManager;
import com.yourmod.teamsystem.core.VehicleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CombatEventHandler {
    private final TeamManager teamManager;

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
        try {
            Path logDir = Paths.get("world/teamsystem/logs");
            Files.createDirectories(logDir);
            String line = String.format("[%s] %s -> %s (%s)%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                killerName, victimName, type);
            Files.writeString(logDir.resolve("kills.log"), line,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
            TeamSystem.LOGGER.debug("resolveAttacker: direct entity is ServerPlayer");
            return (ServerPlayer) srcEntity;
        }

        // 2. Try direct entity (projectile, etc.)
        Entity direct = src.getDirectEntity();
        if (direct instanceof ServerPlayer) {
            TeamSystem.LOGGER.debug("resolveAttacker: direct entity is ServerPlayer");
            return (ServerPlayer) direct;
        }

        // 3. Try common owner getter methods
        if (direct != null) {
            ServerPlayer owner = tryCommonOwnerGetters(direct, level);
            if (owner != null) {
                TeamSystem.LOGGER.debug("resolveAttacker: found owner via common getter from {}", direct.getClass().getName());
                return owner;
            }

            // 4. Try reflection-based owner detection
            owner = tryReflectionOwnerDetection(direct, level);
            if (owner != null) {
                TeamSystem.LOGGER.debug("resolveAttacker: found owner via reflection from {}", direct.getClass().getName());
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
            VehicleManager vm = TeamSystem.getVehicleManager();
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

        for (String methodName : methodNames) {
            try {
                Method method = entity.getClass().getMethod(methodName);
                method.setAccessible(false);
                Object result = method.invoke(entity);

                if (result instanceof ServerPlayer) {
                    return (ServerPlayer) result;
                }
                if (result instanceof UUID) {
                    return level.getServer().getPlayerList().getPlayer((UUID) result);
                }
            } catch (Exception ignored) {
                // Method not found or invocation failed; continue
            }
        }

        return null;
    }

    /**
     * Attempts to find owner via reflection on less-common methods (getOwnerUUID, getOwnerId, getCreator, etc.).
     */
    private ServerPlayer tryReflectionOwnerDetection(Entity entity, ServerLevel level) {
        String[] methodNames = {"getOwnerUUID", "getOwnerId", "getCreator"};

        for (String methodName : methodNames) {
            try {
                Method method = entity.getClass().getMethod(methodName);
                method.setAccessible(false);
                Object result = method.invoke(entity);

                if (result instanceof UUID) {
                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer((UUID) result);
                    if (owner != null) return owner;
                }
            } catch (Exception ignored) {
                // Method not found or invocation failed; continue
            }
        }

        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        GameManager game = TeamSystem.getGameManager();
        if (game != null && !game.isPlaying()) {
            event.setCanceled(true);
            return;
        }

        DamageSource source = event.getSource();
        ServerLevel level = (ServerLevel) victim.level();
        ServerPlayer attacker = resolveAttacker(source, level);

        if (attacker != null) {
            if (attacker != victim && teamManager.isFriendly(attacker, victim)) {
                event.setCanceled(true);
                TeamSystem.LOGGER.debug("Blocked friendly fire from {} to {}",
                    attacker.getName().getString(),
                    victim.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        DamageSource source = event.getSource();
        ServerLevel level = (ServerLevel) victim.level();
        ServerPlayer killer = resolveAttacker(source, level);

        GameManager game = TeamSystem.getGameManager();
        boolean isPlaying = game != null && game.isPlaying();

        teamManager.incrementDeaths(victim.getUUID());

        ContributionManager contrib = TeamSystem.getContributionManager();
        if (contrib != null && isPlaying) {
            contrib.addDeath(victim.getUUID(), victim.getName().getString());
        }

        Team victimTeam = teamManager.getOrCreatePlayerData(victim.getUUID()).getTeam();
        if (victimTeam.isPlayable() && isPlaying) {
            TicketManager ticketMgr = TeamSystem.getTicketManager();
            if (ticketMgr != null) ticketMgr.deductTicket(victimTeam);
        }

        if (killer != null && !killer.getUUID().equals(victim.getUUID())) {
            if (!isDedupKill(killer, victim)) return;
            teamManager.incrementKills(killer.getUUID());
            teamManager.syncPlayerData(killer);

            if (contrib != null && isPlaying) {
                contrib.addKill(killer.getUUID(), killer.getName().getString());
            }

            EconomyManager econ = TeamSystem.getEconomyManager();
            TeamSystemConfig cfg = TeamSystem.getConfig();
            int spReward = cfg != null ? cfg.getKillRewardSP() : 10;
            int bcReward = cfg != null ? cfg.getKillRewardBC() : 5;

            if (econ != null && isPlaying) {
                econ.addSP(killer.getUUID(), spReward);
                econ.addBC(killer.getUUID(), bcReward);
                econ.syncAll(killer);
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("killer", killer.getName().getString());
            placeholders.put("victim", victim.getName().getString());
            placeholders.put("bc", String.valueOf(bcReward));
            placeholders.put("sp", String.valueOf(spReward));

            victim.sendSystemMessage(Component.literal("§cВас убил §6" + killer.getName().getString()
                + " §cс дистанции §6" + (int) victim.distanceTo(killer) + "§cм"), false);
            logKill(killer.getName().getString(), victim.getName().getString(), "player");
            killer.sendSystemMessage(
                Component.literal("§6" + victim.getName().getString() + " §a"
                    + (cfg != null ? cfg.getMessage("kill_reward", placeholders)
                    : "+" + bcReward + " BC +" + spReward + " SP")), false);

            Rank oldRank = Rank.fromKills(teamManager.getOrCreatePlayerData(killer.getUUID()).getKills() - 1);
            Rank newRank = Rank.fromKills(teamManager.getOrCreatePlayerData(killer.getUUID()).getKills());
            if (oldRank.ordinal() < newRank.ordinal()) {
                teamManager.setPlayerRank(killer, newRank.ordinal());
                Map<String, String> rankPl = new HashMap<>();
                rankPl.put("rank", newRank.getDisplayName());
                killer.sendSystemMessage(
                    Component.literal(cfg != null
                        ? cfg.getMessage("rank_up", rankPl)
                        : "§6[PROMOTION] You are now " + newRank.getDisplayName() + "!"), false);
            }
        }

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
        GameManager game = TeamSystem.getGameManager();
        if (game == null) {
            player.setHealth(player.getMaxHealth());
            return;
        }
        MapConfig map = game.getCurrentMap();
        if (map == null) {
            player.setHealth(player.getMaxHealth());
            return;
        }
        Team team = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        player.setHealth(player.getMaxHealth());
        if (team != null && team.isPlayable()) {
            game.teleportPlayerToMapAtTeamSpawn(player, map, team);
            game.setMapRespawn(player, map, team);
        } else {
            game.teleportPlayerToLobby(player);
        }
    }

    @SubscribeEvent
    public void onLivingDeathEntity(LivingDeathEvent event) {
        net.minecraft.world.entity.Entity ent = event.getEntity();
        if (ent instanceof ServerPlayer) return;
        if (!(ent instanceof LivingEntity le)) return;
        if (le.level().isClientSide) return;

        GameManager game = TeamSystem.getGameManager();
        if (game == null || !game.isPlaying()) return;

        VehicleManager vm = TeamSystem.getVehicleManager();
        if (vm == null) return;
        if (!vm.isVehicleEntityType(ent)) return;

        ServerLevel level = (ServerLevel) le.level();
        ServerPlayer killer = resolveAttacker(event.getSource(), level);
        if (killer == null) return;
        if (!isDedupKill(killer, le)) return;

        TeamSystemConfig cfg = TeamSystem.getConfig();
        int vSpReward = cfg != null ? cfg.getVehicleKillRewardSP() : 20;
        int vBcReward = cfg != null ? cfg.getVehicleKillRewardBC() : 10;

        EconomyManager econ = TeamSystem.getEconomyManager();
        if (econ != null) {
            econ.addSP(killer.getUUID(), vSpReward);
            econ.addBC(killer.getUUID(), vBcReward);
            econ.syncAll(killer);
        }

        String name = le.hasCustomName() ? le.getCustomName().getString() : le.getType().getDescription().getString();
        Map<String, String> vPl = new HashMap<>();
        vPl.put("killer", killer.getName().getString());
        vPl.put("vehicle", name);
        vPl.put("bc", String.valueOf(vBcReward));
        vPl.put("sp", String.valueOf(vSpReward));
        getServer(level).getPlayerList().broadcastSystemMessage(
            Component.literal(cfg != null
                ? cfg.getMessage("kill_feed_vehicle", vPl)
                : "§cТехника " + name + " §cуничтожена §6" + killer.getName().getString()), false);
        logKill(killer.getName().getString(), name, "vehicle");
        killer.sendSystemMessage(
            Component.literal(cfg != null
                ? cfg.getMessage("kill_reward", vPl)
                : "§a+" + vBcReward + " BC §7+" + vSpReward + " SP"), false);
        if (vm != null) vm.unregisterSpawnedVehicle(ent.getUUID());
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        GameManager game = TeamSystem.getGameManager();
        if (game == null || !game.isPlaying()) return;
        Level level = event.getLevel();
        if (level.isClientSide) return;
        // Copy and clear so vanilla won't re-destroy, then destroy blocks without drops
        List<net.minecraft.core.BlockPos> blocks = List.copyOf(event.getExplosion().getToBlow());
        event.getExplosion().clearToBlow();
        for (net.minecraft.core.BlockPos pos : blocks) {
            level.destroyBlock(pos, false);
        }
    }
}
