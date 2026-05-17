package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.GameStateSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class GameManager {
    public enum GamePhase {
        LOBBY,
        PLAYING,
        ENDING
    }

    private static final ResourceLocation LOBBY_DIMENSION_ID = new ResourceLocation("teamsystem:lobby");

    private final MinecraftServer server;
    private GamePhase currentPhase;
    private int endingTimer;
    private Team winningTeam;
    private int lobbyWaitTimer;
    private boolean countdownActive;

    public GameManager(MinecraftServer server) {
        this.server = server;
        this.currentPhase = GamePhase.LOBBY;
        this.endingTimer = 0;
        this.winningTeam = null;
        this.lobbyWaitTimer = 0;
        this.countdownActive = false;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public Team getWinningTeam() {
        return winningTeam;
    }

    public boolean isPlaying() {
        return currentPhase == GamePhase.PLAYING;
    }

    public boolean isLobby() {
        return currentPhase == GamePhase.LOBBY;
    }

    public void startGame() {
        TeamManager teamManager = TeamSystem.getTeamManager();
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();

        if (mapPool.getMaps().isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("No maps configured! Add maps to teamsystem_maps.json")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        MapConfig map = mapPool.getCurrentMap().orElse(null);
        if (map == null) {
            map = mapPool.nextMap();
            if (map == null) return;
        }

        currentPhase = GamePhase.PLAYING;
        winningTeam = null;
        teamManager.resetTickets();
        teamManager.setTickets(Team.NATO, map.getTickets());
        teamManager.setTickets(Team.RUSSIA, map.getTickets());

        applyMapConfig(map);
        teleportAllPlayersToMap(map);
        broadcastToAll(Component.literal("Game started on map: ")
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            .append(Component.literal(map.getName()).withStyle(ChatFormatting.YELLOW)));

        syncPhaseToAll();
        TeamSystem.LOGGER.info("Game started on map: {}", map.getName());
    }

    public void endGame(Team winner) {
        if (currentPhase != GamePhase.PLAYING) return;

        currentPhase = GamePhase.ENDING;
        winningTeam = winner;
        endingTimer = 100;

        broadcastToAll(Component.literal("=== GAME OVER ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        broadcastToAll(Component.literal("Team ").withStyle(ChatFormatting.WHITE)
            .append(winner.getColoredName())
            .append(Component.literal(" wins!").withStyle(ChatFormatting.WHITE)));

        syncPhaseToAll();
        TeamSystem.LOGGER.info("Game ended. Winner: {}", winner.getName());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (currentPhase == GamePhase.ENDING) {
            endingTimer--;
            if (endingTimer <= 0) {
                returnToLobby();
            }
        }

        if (currentPhase == GamePhase.LOBBY && countdownActive) {
            lobbyWaitTimer--;
            if (lobbyWaitTimer % 20 == 0 && lobbyWaitTimer > 0) {
                int seconds = lobbyWaitTimer / 20;
                broadcastToAll(Component.literal("Next round in " + seconds + " seconds...")
                    .withStyle(ChatFormatting.AQUA));
            }
            if (lobbyWaitTimer <= 0) {
                countdownActive = false;
                startGame();
            }
        }
    }

    public void startCountdown(int seconds) {
        if (currentPhase != GamePhase.LOBBY) return;
        lobbyWaitTimer = seconds * 20;
        countdownActive = true;
        broadcastToAll(Component.literal("Game starting in " + seconds + " seconds...")
            .withStyle(ChatFormatting.GREEN));
    }

    public void cancelCountdown() {
        countdownActive = false;
        lobbyWaitTimer = 0;
        broadcastToAll(Component.literal("Countdown cancelled.")
            .withStyle(ChatFormatting.RED));
    }

    private void returnToLobby() {
        currentPhase = GamePhase.LOBBY;
        winningTeam = null;
        endingTimer = 0;

        TeamManager teamManager = TeamSystem.getTeamManager();
        MapPoolManager mapPool = TeamSystem.getMapPoolManager();

        MapConfig nextMap = mapPool.nextMap();
        if (nextMap != null) {
            broadcastToAll(Component.literal("Next map: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(nextMap.getName()).withStyle(ChatFormatting.YELLOW)));
        }

        teleportAllToLobby();
        syncPhaseToAll();

        int waitTime = nextMap != null ? nextMap.getLobbyWaitTime() : 30;
        startCountdown(waitTime);

        TeamSystem.LOGGER.info("Returned to lobby");
    }

    public void teleportAllPlayersToMap(MapConfig map) {
        ServerLevel targetWorld = getMapWorld(map);
        if (targetWorld == null) {
            TeamSystem.LOGGER.error("Cannot find world for map: {}", map.getName());
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        TeamManager tm = TeamSystem.getTeamManager();

        for (ServerPlayer player : players) {
            Team team = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (team == Team.SPECTATOR) continue;

            player.teleportTo(targetWorld, 0, 64, 0, 0, 0);
        }
    }

    public void teleportAllToLobby() {
        ServerLevel lobbyWorld = getLobbyWorld();
        if (lobbyWorld == null) {
            TeamSystem.LOGGER.error("Lobby world not found!");
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.teleportTo(lobbyWorld, 0, 64, 0, 0, 0);
        }
    }

    public void teleportPlayerToLobby(ServerPlayer player) {
        ServerLevel lobbyWorld = getLobbyWorld();
        if (lobbyWorld == null) return;
        player.teleportTo(lobbyWorld, 0, 64, 0, 0, 0);
    }

    private ServerLevel getMapWorld(MapConfig map) {
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("teamsystem", map.getWorldFolder()));
        ServerLevel world = server.getLevel(worldKey);
        if (world == null) {
            world = server.overworld();
        }
        return world;
    }

    private ServerLevel getLobbyWorld() {
        ResourceKey<Level> lobbyKey = ResourceKey.create(Registries.DIMENSION, LOBBY_DIMENSION_ID);
        ServerLevel world = server.getLevel(lobbyKey);
        if (world == null) {
            world = server.overworld();
        }
        return world;
    }

    private void applyMapConfig(MapConfig map) {
        ServerLevel mapWorld = getMapWorld(map);
        if (mapWorld == null) return;

        GameRules gameRules = mapWorld.getGameRules();
        gameRules.getRule(GameRules.RULE_NATURAL_REGENERATION).set(map.hasRegen(), server);
        gameRules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(!map.hasRespawn(), server);

        if (map.hasWorldBorder()) {
            mapWorld.getWorldBorder().setCenter(map.getWorldBorderCenterX(), map.getWorldBorderCenterZ());
            mapWorld.getWorldBorder().setSize(map.getWorldBorderSize());
        } else {
            mapWorld.getWorldBorder().setSize(6.0E7);
        }
    }

    private void broadcastToAll(Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    public void syncPhaseToAll() {
        GameStateSyncPacket packet = new GameStateSyncPacket(
            currentPhase.ordinal(),
            TeamSystem.getMapPoolManager().getCurrentMap().map(MapConfig::getName).orElse(""),
            winningTeam != null ? winningTeam.ordinal() : -1
        );
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public void syncPhaseToPlayer(ServerPlayer player) {
        GameStateSyncPacket packet = new GameStateSyncPacket(
            currentPhase.ordinal(),
            TeamSystem.getMapPoolManager().getCurrentMap().map(MapConfig::getName).orElse(""),
            winningTeam != null ? winningTeam.ordinal() : -1
        );
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
