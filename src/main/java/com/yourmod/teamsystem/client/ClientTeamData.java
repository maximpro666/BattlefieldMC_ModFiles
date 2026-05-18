package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.DownedData;
import com.yourmod.teamsystem.client.VehicleData;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

public class ClientTeamData {
    private static Team localPlayerTeam = Team.SPECTATOR;
    private static int localPlayerKills = 0;
    private static int localPlayerDeaths = 0;
    private static String localPlayerPrefix = "";
    private static String localPlayerSuffix = "";
    private static String localPlayerDisplayName = "";
    private static int natoTickets = 0;
    private static int russiaTickets = 0;
    private static int gamePhase = 0;
    private static String currentMapName = "";
    private static int winningTeamOrdinal = -1;
    public static int localPlayerRank = 0;
    public static String localPlayerSquad = "";
    public static int localPlayerBC = 0;
    public static int localPlayerSP = 0;
    public static List<DownedData> downedPlayers = new ArrayList<>();

    public static List<KitData> kits = new ArrayList<>();
    public static List<VehicleData> vehicles = new ArrayList<>();
    public static List<FOBData> fobs = new ArrayList<>();
    public static List<CapturePointData> capturePoints = new ArrayList<>();
    public static String language = "ru";
    public static List<SpeakingPlayer> speakingPlayerDisplay = new ArrayList<>();
    public static KitData selectedKit = null;

    public static final Map<UUID, PlayerListEntry> playerDataMap = new HashMap<>();
    public static final Map<UUID, Integer> playerTeamMap = new HashMap<>();
    public static final List<KitEntry> availableKits = new ArrayList<>();
    public static final List<VehicleEntry> availableVehicles = new ArrayList<>();
    public static int matchTimeSeconds = 0;
    public static final List<String> speakingPlayers = new ArrayList<>();
    public static float guiVolume = 1.0f;
    public static float guiScale = 1.0f;
    public static float guiOpacity = 1.0f;
    public static int maxTickets = 100;

    public static PlayerListEntry getPlayerData(UUID uuid) { return playerDataMap.get(uuid); }
    public static List<String> getSpeakingPlayers() { return Collections.unmodifiableList(speakingPlayers); }

    public static void setLocalPlayerTeam(Team team) {
        localPlayerTeam = team;
    }

    public static void setLocalPlayerData(Team team, int kills, int deaths) {
        setLocalPlayerData(team, kills, deaths, "", "", "");
    }

    public static void setLocalPlayerData(Team team, int kills, int deaths, String prefix, String suffix, String displayName) {
        localPlayerTeam = team;
        localPlayerKills = kills;
        localPlayerDeaths = deaths;
        localPlayerPrefix = prefix != null ? prefix : "";
        localPlayerSuffix = suffix != null ? suffix : "";
        localPlayerDisplayName = displayName != null ? displayName : "";
    }

    public static Team getLocalPlayerTeam() {
        return localPlayerTeam;
    }

    public static int getLocalPlayerKills() {
        return localPlayerKills;
    }

    public static int getLocalPlayerDeaths() {
        return localPlayerDeaths;
    }

    public static String getLocalPlayerPrefix() {
        return localPlayerPrefix;
    }

    public static String getLocalPlayerSuffix() {
        return localPlayerSuffix;
    }

    public static String getLocalPlayerDisplayName() {
        return localPlayerDisplayName;
    }

    public static void setTickets(int natoTickets, int russiaTickets) {
        ClientTeamData.natoTickets = natoTickets;
        ClientTeamData.russiaTickets = russiaTickets;
    }

    public static int getNatoTickets() {
        return natoTickets;
    }

    public static int getRussiaTickets() {
        return russiaTickets;
    }

    public static void setGamePhase(int phase) {
        gamePhase = phase;
    }

    public static int getGamePhase() {
        return gamePhase;
    }

    public static void setCurrentMapName(String name) {
        currentMapName = name != null ? name : "";
    }

    public static String getCurrentMapName() {
        return currentMapName;
    }

    public static void setWinningTeamOrdinal(int ordinal) {
        winningTeamOrdinal = ordinal;
    }

    public static int getWinningTeamOrdinal() {
        return winningTeamOrdinal;
    }

    public static String getLocalPlayerSquad() {
        return localPlayerSquad;
    }
}
