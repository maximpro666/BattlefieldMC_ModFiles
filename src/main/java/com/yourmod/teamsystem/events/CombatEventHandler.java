package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.ContributionManager;
import com.yourmod.teamsystem.core.DownedManager;
import com.yourmod.teamsystem.core.EconomyManager;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.core.TicketManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.UUID;

public class CombatEventHandler {
    private final TeamManager teamManager;

    public CombatEventHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
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
            if (dm != null && !dm.isDowned(victim.getUUID())) {
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
            if (econ != null && isPlaying) {
                int spReward = 10;
                int bcReward = 5;
                econ.addSP(killer.getUUID(), spReward);
                econ.addBC(killer.getUUID(), bcReward);
                econ.syncAll(killer);
            }

            Rank oldRank = Rank.fromKills(teamManager.getOrCreatePlayerData(killer.getUUID()).getKills() - 1);
            Rank newRank = Rank.fromKills(teamManager.getOrCreatePlayerData(killer.getUUID()).getKills());
            if (oldRank.ordinal() < newRank.ordinal()) {
                teamManager.setPlayerRank(killer, newRank.ordinal());
                killer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§6[PROMOTION] You are now " + newRank.getDisplayName() + "!"),
                    false
                );
            }

            TeamSystem.LOGGER.info("Player {} killed {} (K/D: {}/{})",
                killer.getName().getString(),
                victim.getName().getString(),
                teamManager.getOrCreatePlayerData(killer.getUUID()).getKills(),
                teamManager.getOrCreatePlayerData(killer.getUUID()).getDeaths());
        }
        teamManager.syncPlayerData(victim);
    }
}
