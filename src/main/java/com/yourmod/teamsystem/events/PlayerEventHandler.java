package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapConfig;
import com.yourmod.teamsystem.core.MarkerManager;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
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
                    game.setLobbyRespawn(player);
                }
            }
            MarkerManager mm = TeamSystem.getMarkerManager();
            if (mm != null) {
                mm.syncToPlayer(player);
            }
            TeamSystem.LOGGER.info("Synced team data for player: {}", player.getName().getString());
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameManager game = TeamSystem.getGameManager();

            if (game == null) {
                teamManager.fullSyncPlayer(player);
                return;
            }

            if (game.isPlaying()) {
                MapConfig map = TeamSystem.getMapPoolManager().getCurrentMap().orElse(null);
                if (map != null && !map.hasRespawn()) {
                    teamManager.setPlayerTeam(player, Team.SPECTATOR);
                    game.teleportPlayerToLobby(player);
                    game.setLobbyRespawn(player);
                    player.sendSystemMessage(Component.literal("Respawn is disabled on this map. You are now spectating.")
                        .withStyle(ChatFormatting.RED));
                    return;
                }
            }

            if (game.isLobby()) {
                game.teleportPlayerToLobby(player);
                game.setLobbyRespawn(player);
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
