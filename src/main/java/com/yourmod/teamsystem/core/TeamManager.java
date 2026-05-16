package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.CombatDataSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.TeamSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class TeamManager extends SavedData {
    private static final String DATA_NAME = "teamsystem_data";
    private final Map<UUID, PlayerCombatData> playerData = new HashMap<>();
    private final MinecraftServer server;

    public TeamManager(MinecraftServer server) {
        this.server = server;
    }

    public static TeamManager load(MinecraftServer server, CompoundTag nbt) {
        TeamManager manager = new TeamManager(server);
        ListTag playerList = nbt.getList("Players", Tag.TAG_COMPOUND);
        for (Tag tag : playerList) {
            CompoundTag playerTag = (CompoundTag) tag;
            UUID uuid = playerTag.getUUID("UUID");
            PlayerCombatData data = PlayerCombatData.fromNBT(playerTag.getCompound("Data"));
            manager.playerData.put(uuid, data);
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, PlayerCombatData> entry : playerData.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("UUID", entry.getKey());
            playerTag.put("Data", entry.getValue().serializeNBT());
            playerList.add(playerTag);
        }
        nbt.put("Players", playerList);
        return nbt;
    }

    public PlayerCombatData getOrCreatePlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> new PlayerCombatData());
    }

    public void setPlayerTeam(ServerPlayer player, Team team) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        data.setTeam(team);

        addPlayerToScoreboardTeam(player, team);

        syncPlayerTeam(player);
        syncPlayerData(player);

        setDirty();
        TeamSystem.LOGGER.info("Player {} assigned to team {}", player.getName().getString(), team.name());
    }

    private void addPlayerToScoreboardTeam(ServerPlayer player, Team team) {
        Scoreboard scoreboard = server.getScoreboard();
        String teamName = team.getScoreboardName();
        PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(teamName);

        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.addPlayerTeam(teamName);
            scoreboardTeam.setColor(team.getChatColor());
            scoreboardTeam.setAllowFriendlyFire(false);
        }

        scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboardTeam);
    }

    public void incrementKills(UUID playerId) {
        PlayerCombatData data = getOrCreatePlayerData(playerId);
        data.addKill();
        setDirty();
    }

    public void incrementDeaths(UUID playerId) {
        PlayerCombatData data = getOrCreatePlayerData(playerId);
        data.addDeath();
        setDirty();
    }

    public boolean isFriendly(ServerPlayer player1, ServerPlayer player2) {
        Team team1 = getOrCreatePlayerData(player1.getUUID()).getTeam();
        Team team2 = getOrCreatePlayerData(player2.getUUID()).getTeam();
        return team1 == team2 && team1 != Team.SPECTATOR;
    }

    public void syncPlayerTeam(ServerPlayer player) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        TeamSyncPacket packet = new TeamSyncPacket(data.getTeam().ordinal());
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void syncPlayerData(ServerPlayer player) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        CombatDataSyncPacket packet = new CombatDataSyncPacket(
            data.getTeam().ordinal(),
            data.getKills(),
            data.getDeaths()
        );
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void fullSyncPlayer(ServerPlayer player) {
        syncPlayerTeam(player);
        syncPlayerData(player);
    }

    public void assignSquad(UUID playerId, int squadId) {
        PlayerCombatData data = getOrCreatePlayerData(playerId);
        data.setSquadId(squadId);
        setDirty();
        TeamSystem.LOGGER.info("Player {} assigned to squad {}", playerId, squadId);
    }

    public List<UUID> getPlayersInSquad(int squadId) {
        List<UUID> members = new ArrayList<>();
        for (Map.Entry<UUID, PlayerCombatData> entry : playerData.entrySet()) {
            if (entry.getValue().getSquadId() == squadId) {
                members.add(entry.getKey());
            }
        }
        return members;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
