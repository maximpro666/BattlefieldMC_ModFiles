package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.data.CentralDatabase;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.data.KitConfigServerHelper;
import com.pigeostudios.pwp.network.CombatDataSyncPacket;
import com.pigeostudios.pwp.network.KitConfigSyncPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.RankSyncPacket;
import com.pigeostudios.pwp.network.TeamSyncPacket;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import java.util.*;

public class TeamManager extends SavedData {
    private static final String DATA_NAME = "pwp_data";
    private final Map<UUID, PlayerCombatData> playerData = new HashMap<>();
    private KitManager kitManager;
    private SquadManager squadManager;
    private VehicleManager vehicleManager;
    private final MinecraftServer server;

    public TeamManager(MinecraftServer server) {
        this.server = server;
        this.kitManager = new KitManager();
        this.squadManager = new SquadManager();
        this.vehicleManager = new VehicleManager();
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

    public void loadFromCentralDatabase() {
        CentralDatabase.init();
        Map<UUID, PlayerCombatData> dbData = CentralDatabase.loadAllPlayers();
        if (dbData.isEmpty()) return;
        int count = 0;
        for (Map.Entry<UUID, PlayerCombatData> entry : dbData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerCombatData dbPlayer = entry.getValue();
            PlayerCombatData existing = playerData.get(uuid);
            if (existing != null) {
                mergeData(dbPlayer, existing);
            } else {
                playerData.put(uuid, dbPlayer);
            }
            count++;
        }
        setDirty();
        PWP.LOGGER.info("Loaded {} players from CentralDatabase", count);
    }

    private void mergeData(PlayerCombatData from, PlayerCombatData to) {
        to.setKills(Math.max(to.getKills(), from.getKills()));
        to.setDeaths(Math.max(to.getDeaths(), from.getDeaths()));
        to.setWins(Math.max(to.getWins(), from.getWins()));
        to.setRating(Math.max(to.getRating(), from.getRating()));
        to.setRankOrdinal(Math.max(to.getRankOrdinal(), from.getRankOrdinal()));
        to.setBattleCredits(Math.max(to.getBattleCredits(), from.getBattleCredits()));
        to.setWarCredits(Math.max(to.getWarCredits(), from.getWarCredits()));
        to.setCallsign(from.getCallsign());
        to.setDisplayName(from.getDisplayName());
        to.setHasReceivedDogTag(from.hasReceivedDogTag());
        to.setAdmin(from.isAdmin());
        to.setDonatTier(from.getDonatTier());
        to.setPlayerTitle(from.getPlayerTitle());
        to.setLoadoutConfig(from.getLoadoutConfig());
        for (String k : from.getUnlockedKits()) to.unlockKit(k);
        for (String r : from.getUnlockedRoles()) to.unlockRole(r);
        for (String l : from.getUnlockedLoadouts()) to.unlockLoadout(l);
        for (String c : from.getCertifications()) to.grantCertification(c);
        for (Map.Entry<String, net.minecraft.nbt.CompoundTag> att : from.getSavedAttachments().entrySet()) {
            to.getSavedAttachments().put(att.getKey(), att.getValue());
        }
    }

    public void syncToDatabase() {
        if (ProxyMessenger.isMatchServer()) return;
        CentralDatabase.init();
        CentralDatabase.saveAllPlayers(playerData);
        PWP.LOGGER.info("Synced {} players to CentralDatabase", playerData.size());
    }

    // ===== Player Name Display =====

    public void updatePlayerDisplayName(ServerPlayer player) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        Team team = data.getTeam();

        if (team == Team.SPECTATOR) {
            player.setCustomName(Component.literal("§7[Spectator] " + player.getName().getString()));
            player.setCustomNameVisible(true);
            return;
        }

        boolean russian = "ru".equals(PWP.getConfig().getLanguage());
        String callsign = data.getCallsign();
        String actualName = player.getName().getString();

        String rankPrefix = "";
        try {
            Rank rank = Rank.fromOrdinal(data.getRankOrdinal());
            rankPrefix = rank.getPrefix(russian);
            data.setRankPrefix(rankPrefix);
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to load Rank enum: {}", e.getMessage());
        }

        String fullName;
        if (!callsign.isEmpty()) {
            fullName = rankPrefix + " " + callsign;
        } else {
            fullName = rankPrefix + " " + actualName;
        }

        if (data.isAdmin()) {
            fullName = "§c[Admin] " + fullName;
        } else if (data.getDonatTier() > 0) {
            fullName = "§6[Donat] " + fullName;
        }
        String title = data.getPlayerTitle();
        if (!title.isEmpty()) {
            fullName = "§e[" + title + "] " + fullName;
        }

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

    public List<ServerPlayer> getPlayersByTeam(Team team) {
        MinecraftServer server = getServer();
        if (server == null) return List.of();
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerCombatData data = playerData.get(p.getUUID());
            if (data != null && data.getTeam() == team) {
                result.add(p);
            }
        }
        return result;
    }

    public void setPlayerTeam(ServerPlayer player, Team team) {
        PlayerCombatData data = getOrCreatePlayerData(player.getUUID());
        Team oldTeam = data.getTeam();
        data.setTeam(team);

        // Remove player from all other vanilla scoreboard teams before adding to new one
        removePlayerFromAllScoreboardTeams(player);

        addPlayerToScoreboardTeam(player, team);

        syncPlayerTeam(player);
        syncPlayerData(player);
        updatePlayerDisplayName(player);
        syncRank(player);
        BattlefieldRuntime.getInstance().syncAll(player);

        // Clear inventory on team change
        if (oldTeam != null && oldTeam != team) {
            player.getInventory().clearContent();
        }

        // Set game mode based on team
        if (team.isPlayable()) {
            player.setGameMode(GameType.SURVIVAL);
        } else {
            player.setGameMode(GameType.SPECTATOR);
        }

        setDirty();
        PWP.LOGGER.info("Player {} assigned to team {}", player.getName().getString(), team.name());

        GameManager game = PWP.getGameManager();
        if (game != null) {
            if (team.isPlayable() && game.isPlaying()) {
                ResourceKey<Level> dimKey = game.getCurrentDimKey();
                if (dimKey != null) {
                    MapConfig map = game.getCurrentMap();
                    ServerLevel target = server.getLevel(dimKey);
                    if (target != null && map != null) {
                        double x = map.getWorldBorderCenterX() + 0.5;
                        double y = 65;
                        double z = map.getWorldBorderCenterZ() + 0.5;
                        if (map.hasTeamSpawns()) {
                            int[] spawn = team == Team.NATO ? map.getNatoSpawn() : map.getRussiaSpawn();
                            if (spawn != null && spawn.length >= 3) {
                                x = spawn[0] + 0.5; y = spawn[1]; z = spawn[2] + 0.5;
                            }
                        }
                        player.teleportTo(target, x, y, z, 0, 0);
                        player.fallDistance = 0;
                    }
                }
            } else {
                game.teleportPlayerToLobby(player);
                game.setLobbyRespawn(player);
            }
        }

        var vgm = TeamVoicePlugin.getGroupManager();
        if (vgm != null && vgm.isAvailable()) {
            vgm.joinChannel(player, 0);
        }
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
        Squad squad = squadManager.getPlayerSquad(player.getUUID());
        String squadName = squad != null ? squad.getName() : "";
        boolean isLeader = squad != null && squad.isLeader(player.getUUID());
        int rawDonat = data.getDonatTier();
        if (isLeader) rawDonat |= 128;
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        int wc = runtime.getWC(player.getUUID());
        int bc = runtime.getBC(player.getUUID());
        CombatDataSyncPacket packet = new CombatDataSyncPacket(
            player.getUUID(),
            data.getTeam().ordinal(),
            data.getKills(),
            data.getDeaths(),
            data.getPrefix(),
            data.getSuffix(),
            data.getDisplayName(),
            data.getCallsign(),
            data.getRankOrdinal(),
            squadName,
            wc,
            bc,
            rawDonat,
            data.hasReceivedDogTag()
        );
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void fullSyncPlayer(ServerPlayer player) {
        syncPlayerTeam(player);
        syncPlayerData(player);
        updatePlayerDisplayName(player);
        syncConfig(player);
        syncRank(player);
        syncKitConfig(player);
        syncVehicles(player);
        syncFOBs(player);
        BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
        runtime.syncAll(player);
    }

    public void syncConfig(ServerPlayer player) {
        com.pigeostudios.pwp.network.ConfigSyncPacket packet =
            new com.pigeostudios.pwp.network.ConfigSyncPacket(PWP.getConfig().getLanguage());
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void syncRank(ServerPlayer player) {
        int ordinal = getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        boolean russian = "ru".equals(PWP.getConfig().getLanguage());
        RankSyncPacket packet = new RankSyncPacket(ordinal, russian);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void syncFOBs(ServerPlayer player) {
        PWP.getFOBManager().syncToPlayer(player);
    }

    public void syncKits(ServerPlayer player) {
        List<Kit> kitList = kitManager.getAvailableKits(player, this);
        List<String> names = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        List<Integer> minRanks = new ArrayList<>();
        for (Kit kit : kitList) {
            names.add(kit.getName());
            displayNames.add(kit.getDisplayName());
            minRanks.add(kit.getMinRankOrdinal());
        }
        com.pigeostudios.pwp.network.KitSyncPacket packet =
            new com.pigeostudios.pwp.network.KitSyncPacket(names, displayNames, minRanks);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void syncKitConfig(ServerPlayer player) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return;
        String json = KitConfig.GSON.toJson(cfg);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new KitConfigSyncPacket(json));
    }

    public void syncVehicles(ServerPlayer player) {
        List<VehicleData> vehicleList = vehicleManager.getAvailableVehicles(player, this);
        List<String> ids = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        List<Integer> costs = new ArrayList<>();
        List<Integer> minRanks = new ArrayList<>();
        List<Integer> bcCosts = new ArrayList<>();
        List<Integer> vcCosts = new ArrayList<>();
        for (VehicleData v : vehicleList) {
            ids.add(v.getVehicleId());
            displayNames.add(v.getDisplayName());
            costs.add(v.getTicketCost());
            minRanks.add(v.getMinRankOrdinal());
            bcCosts.add(v.getDeployBC());
            vcCosts.add(v.getDeployVC());
        }
        com.pigeostudios.pwp.network.VehicleSyncPacket packet =
            new com.pigeostudios.pwp.network.VehicleSyncPacket(ids, displayNames, costs, minRanks, bcCosts, vcCosts);
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
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
        PWP.LOGGER.info("Player {} assigned to squad {}", playerId, squadId);
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
        RankSyncPacket packet = new RankSyncPacket(rankOrdinal,
            "ru".equals(PWP.getConfig().getLanguage()));
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
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
