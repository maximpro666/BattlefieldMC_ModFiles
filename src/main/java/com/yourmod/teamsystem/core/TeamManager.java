package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.CombatDataSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.TeamSyncPacket;
import com.yourmod.teamsystem.network.TeamTicketSyncPacket;
import com.yourmod.teamsystem.core.GameManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.network.chat.Component;
import java.util.*;

public class TeamManager extends SavedData {
    private static final String DATA_NAME = "teamsystem_data";
    private static final int DEFAULT_TICKETS = 100;
    private final Map<UUID, PlayerCombatData> playerData = new HashMap<>();
    private final Map<Team, Integer> teamTickets = new EnumMap<>(Team.class);
    private KitManager kitManager;
    private SquadManager squadManager;
    private VehicleManager vehicleManager;
    private final MinecraftServer server;

    public TeamManager(MinecraftServer server) {
        this.server = server;
        this.kitManager = new KitManager();
        this.squadManager = new SquadManager();
        this.vehicleManager = new VehicleManager();
        resetTickets();
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
        // Load ticket data if present
        if (nbt.contains("NatoTickets")) {
            manager.teamTickets.put(Team.NATO, nbt.getInt("NatoTickets"));
            manager.teamTickets.put(Team.RUSSIA, nbt.getInt("RussiaTickets"));
        } else {
            manager.resetTickets();
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
        nbt.putInt("NatoTickets", teamTickets.getOrDefault(Team.NATO, DEFAULT_TICKETS));
        nbt.putInt("RussiaTickets", teamTickets.getOrDefault(Team.RUSSIA, DEFAULT_TICKETS));
        return nbt;
    }

    // ===== Ticket System =====

    public void resetTickets() {
        teamTickets.put(Team.NATO, DEFAULT_TICKETS);
        teamTickets.put(Team.RUSSIA, DEFAULT_TICKETS);
        setDirty();
    }

    public int getTickets(Team team) {
        return teamTickets.getOrDefault(team, DEFAULT_TICKETS);
    }

    public void setTickets(Team team, int amount) {
        teamTickets.put(team, Math.max(0, amount));
        setDirty();
        syncTicketsToAll();
    }

    public void deductTicket(Team team) {
        int current = getTickets(team);
        if (current > 0) {
            teamTickets.put(team, current - 1);
            setDirty();
            syncTicketsToAll();
            TeamSystem.LOGGER.info("{} tickets: {} (team {})", team.getName(), current - 1, team.getName());

            if (current - 1 <= 0) {
                Team winner = team == Team.NATO ? Team.RUSSIA : Team.NATO;
                GameManager game = TeamSystem.getGameManager();
                if (game != null) {
                    game.endGame(winner);
                }
            }
        }
    }

    public Team getTeamWithMostTickets() {
        int nato = getTickets(Team.NATO);
        int russia = getTickets(Team.RUSSIA);
        if (nato > russia) return Team.NATO;
        if (russia > nato) return Team.RUSSIA;
        return Team.NATO;
    }

    public void syncTicketsToAll() {
        int nato = getTickets(Team.NATO);
        int russia = getTickets(Team.RUSSIA);
        TeamTicketSyncPacket packet = new TeamTicketSyncPacket(nato, russia);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    // ===== Player Name Display =====

    public void updatePlayerDisplayName(ServerPlayer player) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        String actualName = player.getName().getString();
        String displayName = data.getDisplayName();
        String nameToShow = displayName.isEmpty() ? actualName : displayName;
        String fullName = data.getPrefix() + nameToShow + data.getSuffix();

        player.setCustomName(Component.literal(fullName));
        player.setCustomNameVisible(true);
    }

    public void syncPlayerNameData(ServerPlayer player) {
        updatePlayerDisplayName(player);
        syncPlayerData(player);
    }

    public PlayerCombatData getOrCreatePlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, id -> new PlayerCombatData());
    }

    public Map<UUID, PlayerCombatData> getPlayerDataCopy() {
        return new HashMap<>(playerData);
    }

    public void setPlayerTeam(ServerPlayer player, Team team) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        data.setTeam(team);

        // Remove player from all other vanilla scoreboard teams before adding to new one
        removePlayerFromAllScoreboardTeams(player);

        addPlayerToScoreboardTeam(player, team);

        syncPlayerTeam(player);
        syncPlayerData(player);
        updatePlayerDisplayName(player);

        setDirty();
        TeamSystem.LOGGER.info("Player {} assigned to team {}", player.getName().getString(), team.name());
    }

    /**
     * Removes the player from all vanilla scoreboard teams to prevent multiple membership.
     */
    private void removePlayerFromAllScoreboardTeams(ServerPlayer player) {
        Scoreboard scoreboard = server.getScoreboard();
        String playerName = player.getScoreboardName();
        // Get all teams that contain this player and remove them
        for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            if (team.getPlayers().contains(playerName)) {
                scoreboard.removePlayerFromTeam(playerName, team);
            }
        }
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
        // Note: syncPlayerData is called by CombatEventHandler after incrementing
    }

    public void incrementDeaths(UUID playerId) {
        PlayerCombatData data = getOrCreatePlayerData(playerId);
        data.addDeath();
        setDirty();
        // Note: syncPlayerData is called by CombatEventHandler after incrementing
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
            data.getDeaths(),
            data.getPrefix(),
            data.getSuffix(),
            data.getDisplayName()
        );
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void fullSyncPlayer(ServerPlayer player) {
        syncPlayerTeam(player);
        syncPlayerData(player);
        updatePlayerDisplayName(player);
    }

    // ===== Player Name Data =====

    public void setPlayerPrefix(ServerPlayer player, String prefix) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        data.setPrefix(prefix);
        setDirty();
        syncPlayerNameData(player);
    }

    public void setPlayerSuffix(ServerPlayer player, String suffix) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        data.setSuffix(suffix);
        setDirty();
        syncPlayerNameData(player);
    }

    public void setPlayerDisplayName(ServerPlayer player, String displayName) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        data.setDisplayName(displayName);
        setDirty();
        syncPlayerNameData(player);
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

    // ===== Rank System =====

    public void setPlayerRank(ServerPlayer player, int rankOrdinal) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        data.setRankOrdinal(rankOrdinal);
        setDirty();
    }

    public int getPlayerRank(UUID playerId) {
        return getOrCreatePlayerData(playerId).getRankOrdinal();
    }

    // ===== Manager Accessors =====

    public KitManager getKitManager() {
        return kitManager;
    }

    public SquadManager getSquadManager() {
        return squadManager;
    }

    public VehicleManager getVehicleManager() {
        return vehicleManager;
    }

    /**
     * TODO: CapturePointManager integration point.
     * In future: manage capture points, track ownership, calculate ticket bleed.
     * For now, reserved for future expansion.
     */

    /**
     * TODO: TicketManager integration point.
     * In future: manage ticket pool per team, deduct on deaths, track ticket state.
     * For now, reserved for future expansion.
     */

    public MinecraftServer getServer() {
        return server;
    }
}
