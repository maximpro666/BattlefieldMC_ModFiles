package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.CombatDataSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {
    private final MinecraftServer server;
    private final Map<UUID, PlayerCombatData> playerData;
    private final Map<Team, PlayerTeam> scoreboardTeams;

    public TeamManager(MinecraftServer server) {
        this.server = server;
        this.playerData = new ConcurrentHashMap<>();
        this.scoreboardTeams = new HashMap<>();

        initializeScoreboardTeams();
    }

    private void initializeScoreboardTeams() {
        Scoreboard scoreboard = server.getScoreboard();

        for (Team team : Team.values()) {
            String teamName = team.getScoreboardName();
            PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(teamName);

            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.addPlayerTeam(teamName);
            }

            scoreboardTeam.setColor(team.getChatColor());
            scoreboardTeam.setDisplayName(team.getColoredName());
            scoreboardTeam.setAllowFriendlyFire(false);
            scoreboardTeam.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.ALWAYS);

            scoreboardTeams.put(team, scoreboardTeam);
        }

        TeamSystem.LOGGER.info("Scoreboard teams initialized");
    }

    private void addPlayerToScoreboardTeam(ServerPlayer player, Team team) {
        Scoreboard scoreboard = server.getScoreboard();
        String playerName = player.getScoreboardName();

        for (PlayerTeam scoreboardTeam : scoreboardTeams.values()) {
            scoreboardTeam.getPlayers().remove(playerName);
        }

        PlayerTeam scoreboardTeam = scoreboardTeams.get(team);
        if (scoreboardTeam != null) {
            scoreboard.addPlayerToTeam(playerName, scoreboardTeam);
        }
    }

    public PlayerCombatData getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, k -> new PlayerCombatData());
    }

    public PlayerCombatData getPlayerData(ServerPlayer player) {
        return getPlayerData(player.getUUID());
    }

    public void setPlayerTeam(ServerPlayer player, Team team) {
        PlayerCombatData data = getPlayerData(player);
        Team oldTeam = data.getTeam();

        if (oldTeam == team) {
            return;
        }

        data.setTeam(team);
        addPlayerToScoreboardTeam(player, team);
        syncPlayerData(player);

        TeamSystem.LOGGER.info("Player {} changed team from {} to {}",
            player.getName().getString(), oldTeam.getName(), team.getName());
    }

    public Team getPlayerTeam(UUID playerId) {
        return getPlayerData(playerId).getTeam();
    }

    public Team getPlayerTeam(ServerPlayer player) {
        return getPlayerData(player).getTeam();
    }

    public boolean isFriendly(ServerPlayer player1, ServerPlayer player2) {
        Team team1 = getPlayerTeam(player1);
        Team team2 = getPlayerTeam(player2);
        return team1.isFriendly(team2);
    }

    public boolean isFriendly(UUID playerId1, UUID playerId2) {
        Team team1 = getPlayerTeam(playerId1);
        Team team2 = getPlayerTeam(playerId2);
        return team1.isFriendly(team2);
    }

    public void addKill(ServerPlayer player) {
        PlayerCombatData data = getPlayerData(player);
        data.addKill();
        syncPlayerData(player);

        TeamSystem.LOGGER.debug("Player {} kill count: {}",
            player.getName().getString(), data.getKills());
    }

    public void addDeath(ServerPlayer player) {
        PlayerCombatData data = getPlayerData(player);
        data.addDeath();
        syncPlayerData(player);

        TeamSystem.LOGGER.debug("Player {} death count: {}",
            player.getName().getString(), data.getDeaths());
    }

    public void resetPlayerStats(ServerPlayer player) {
        PlayerCombatData data = getPlayerData(player);
        data.resetStats();
        syncPlayerData(player);
    }

    public List<ServerPlayer> getPlayersInTeam(Team team) {
        List<ServerPlayer> players = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (getPlayerTeam(player) == team) {
                players.add(player);
            }
        }

        return players;
    }

    public int getTeamPlayerCount(Team team) {
        return (int) playerData.values().stream()
            .filter(data -> data.getTeam() == team)
            .count();
    }

    public int getTeamBalance() {
        int natoCount = getTeamPlayerCount(Team.NATO);
        int russiaCount = getTeamPlayerCount(Team.RUSSIA);
        return Math.abs(natoCount - russiaCount);
    }

    public void syncPlayerData(ServerPlayer player) {
        PlayerCombatData data = getPlayerData(player);
        PacketHandler.getChannel().send(
            PacketDistributor.PLAYER.with(() -> player),
            new CombatDataSyncPacket(data.getTeam().ordinal(), data.getKills(), data.getDeaths())
        );
    }

    public void syncAllPlayersData(ServerPlayer player) {
        syncPlayerData(player);
    }

    public void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerCombatData data = getPlayerData(playerId);
        addPlayerToScoreboardTeam(player, data.getTeam());
        syncPlayerData(player);

        TeamSystem.LOGGER.info("Player {} joined, team: {}",
            player.getName().getString(), data.getTeam().getName());
    }

    public void onPlayerLeave(ServerPlayer player) {
        TeamSystem.LOGGER.debug("Player {} left", player.getName().getString());
    }

    public void save() {
        try {
            File dataFile = getDataFile();
            CompoundTag rootTag = new CompoundTag();
            ListTag playerList = new ListTag();

            for (Map.Entry<UUID, PlayerCombatData> entry : playerData.entrySet()) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID("UUID", entry.getKey());
                playerTag.put("Data", entry.getValue().serializeNBT());
                playerList.add(playerTag);
            }

            rootTag.put("Players", playerList);

            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                NbtIo.writeCompressed(rootTag, fos);
            }

            TeamSystem.LOGGER.info("Saved data for {} players", playerData.size());
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to save team data", e);
        }
    }

    public void load() {
        try {
            File dataFile = getDataFile();
            if (!dataFile.exists()) {
                TeamSystem.LOGGER.info("No saved data found, starting fresh");
                return;
            }

            CompoundTag rootTag;
            try (FileInputStream fis = new FileInputStream(dataFile)) {
                rootTag = NbtIo.readCompressed(fis);
            }

            ListTag playerList = rootTag.getList("Players", Tag.TAG_COMPOUND);

            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag playerTag = playerList.getCompound(i);
                UUID playerId = playerTag.getUUID("UUID");
                PlayerCombatData data = PlayerCombatData.fromNBT(playerTag.getCompound("Data"));
                playerData.put(playerId, data);
            }

            TeamSystem.LOGGER.info("Loaded data for {} players", playerData.size());
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to load team data", e);
        }
    }

    private File getDataFile() {
        File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
        return new File(worldDir, "teamsystem_data.dat");
    }

    public MinecraftServer getServer() {
        return server;
    }

    public Map<UUID, PlayerCombatData> getAllPlayerData() {
        return Collections.unmodifiableMap(playerData);
    }
}
