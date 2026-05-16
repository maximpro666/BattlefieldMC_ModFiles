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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker) {
            TeamManager manager = TeamSystem.getTeamManager();

            if (manager != null && manager.isFriendly(attacker, victim)) {
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

        TeamManager manager = TeamSystem.getTeamManager();
        if (manager == null) {
            return;
        }

        manager.addDeath(victim);

        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer killer) {
            if (!killer.getUUID().equals(victim.getUUID())) {
                manager.addKill(killer);

                TeamSystem.LOGGER.info("Player {} killed {} (K/D: {}/{})",
                    killer.getName().getString(),
                    victim.getName().getString(),
                    manager.getPlayerData(killer).getKills(),
                    manager.getPlayerData(killer).getDeaths());
            }
        }
    }
}
