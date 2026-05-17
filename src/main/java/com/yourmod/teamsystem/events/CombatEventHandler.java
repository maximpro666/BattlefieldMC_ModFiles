package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.ContributionManager;
import com.yourmod.teamsystem.core.DownedManager;
import com.yourmod.teamsystem.core.EconomyManager;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.core.TeamSystemConfig;
import com.yourmod.teamsystem.core.TicketManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatEventHandler {
    private final TeamManager teamManager;

    public CombatEventHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    private net.minecraft.server.MinecraftServer getServer(ServerLevel level) {
        return level.getServer();
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
            if (teamManager.isFriendly(attacker, victim)) {
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

        if (isPlaying) {
            DownedManager dm = TeamSystem.getDownedManager();
            boolean isBleedoutKill = dm != null && dm.isBleedoutKill(victim.getUUID());
            if (dm != null && !dm.isDowned(victim.getUUID()) && !isBleedoutKill) {
                event.setCanceled(true);
                dm.setDowned(victim, killer);
                victim.setHealth(1.0F);
                victim.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cYou are downed! Wait for revive or bleed out."),
                    false);
                if (killer != null && !killer.getUUID().equals(victim.getUUID())) {
                    killer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aEnemy downed: " + victim.getName().getString()),
                        false);
                }
                TeamSystem.LOGGER.info("Player {} downed by {}", victim.getName().getString(),
                    killer != null ? killer.getName().getString() : "environment");
                teamManager.syncPlayerData(victim);
                return;
            }
            if (isBleedoutKill) dm.clearBleedoutKill(victim.getUUID());
        }

        teamManager.incrementDeaths(victim.getUUID());

        ContributionManager contrib = TeamSystem.getContributionManager();
        if (contrib != null && isPlaying) {
            contrib.addDeath(victim.getUUID(), victim.getName().getString());
        }

        Team victimTeam = teamManager.getOrCreatePlayerData(victim.getUUID()).getTeam();
        if (victimTeam.isPlayable() && isPlaying) {
            TicketManager ticketMgr = TeamSystem.getTicketManager();
            if (ticketMgr != null) {
                ticketMgr.deductTicket(victimTeam);
            } else {
                teamManager.deductTicket(victimTeam);
            }
        }

        if (killer != null && !killer.getUUID().equals(victim.getUUID())) {
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

            getServer(level).getPlayerList().broadcastSystemMessage(
                Component.literal(cfg != null
                    ? cfg.getMessage("kill_feed_player", placeholders)
                    : "§6" + victim.getName().getString() + " §eубит §6" + killer.getName().getString()), false);
            killer.sendSystemMessage(
                Component.literal(cfg != null
                    ? cfg.getMessage("kill_reward", placeholders)
                    : "§a+" + bcReward + " BC §7+" + spReward + " SP"), false);

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

        if (victim != null && killer != null && !(victim instanceof ServerPlayer)) {
            TeamSystemConfig vcfg = TeamSystem.getConfig();
            int vSpReward = vcfg != null ? vcfg.getVehicleKillRewardSP() : 20;
            int vBcReward = vcfg != null ? vcfg.getVehicleKillRewardBC() : 10;
            EconomyManager econ = TeamSystem.getEconomyManager();
            if (econ != null && isPlaying) {
                econ.addSP(killer.getUUID(), vSpReward);
                econ.addBC(killer.getUUID(), vBcReward);
                econ.syncAll(killer);
            }
            Map<String, String> vPl = new HashMap<>();
            vPl.put("killer", killer.getName().getString());
            vPl.put("vehicle", victim.getName().getString());
            vPl.put("bc", String.valueOf(vBcReward));
            vPl.put("sp", String.valueOf(vSpReward));
            getServer(level).getPlayerList().broadcastSystemMessage(
                Component.literal(vcfg != null
                    ? vcfg.getMessage("kill_feed_vehicle", vPl)
                    : "§cТехника " + victim.getName().getString() + " §cуничтожена §6" + killer.getName().getString()), false);
            killer.sendSystemMessage(
                Component.literal(vcfg != null
                    ? vcfg.getMessage("kill_reward", vPl)
                    : "§a+" + vBcReward + " BC §7+" + vSpReward + " SP"), false);
        }
        teamManager.syncPlayerData(victim);
    }

    @SubscribeEvent
    public void onLivingDeathEntity(LivingDeathEvent event) {
        net.minecraft.world.entity.Entity ent = event.getEntity();
        if (ent instanceof ServerPlayer) return;
        if (!(ent instanceof LivingEntity le)) return;
        if (le.level().isClientSide) return;

        GameManager game = TeamSystem.getGameManager();
        if (game == null || !game.isPlaying()) return;

        ServerLevel level = (ServerLevel) le.level();
        ServerPlayer killer = resolveAttacker(event.getSource(), level);
        if (killer == null) return;

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
        killer.sendSystemMessage(
            Component.literal(cfg != null
                ? cfg.getMessage("kill_reward", vPl)
                : "§a+" + vBcReward + " BC §7+" + vSpReward + " SP"), false);
    }
}
