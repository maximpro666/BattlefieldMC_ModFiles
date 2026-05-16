package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CombatEventHandler {
    private final TeamManager teamManager;

    public CombatEventHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker) {
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

        teamManager.incrementDeaths(victim.getUUID());

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer killer) {
            if (!killer.getUUID().equals(victim.getUUID())) {
                teamManager.incrementKills(killer.getUUID());

                TeamSystem.LOGGER.info("Player {} killed {} (K/D: {}/{})",
                    killer.getName().getString(),
                    victim.getName().getString(),
                    teamManager.getOrCreatePlayerData(killer.getUUID()).getKills(),
                    teamManager.getOrCreatePlayerData(killer.getUUID()).getDeaths());
            }
        }
    }
}
