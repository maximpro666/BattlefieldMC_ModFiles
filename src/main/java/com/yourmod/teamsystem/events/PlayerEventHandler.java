package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerEventHandler {
    private final TeamManager teamManager;

    public PlayerEventHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            teamManager.fullSyncPlayer(player);
            GameManager game = TeamSystem.getGameManager();
            if (game != null) {
                game.syncPhaseToPlayer(player);
                if (game.isLobby()) {
                    game.teleportPlayerToLobby(player);
                }
            }
            TeamSystem.LOGGER.info("Synced team data for player: {}", player.getName().getString());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameManager game = TeamSystem.getGameManager();
            MapConfig map = TeamSystem.getMapPoolManager().getCurrentMap().orElse(null);

            if (map != null && !map.hasRespawn() && game != null && game.isPlaying()) {
                teamManager.setPlayerTeam(player, Team.SPECTATOR);
                game.teleportPlayerToLobby(player);
                player.sendSystemMessage(Component.literal("Respawn is disabled on this map. You are now spectating.")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            teamManager.fullSyncPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            teamManager.fullSyncPlayer(player);
        }
    }
}
