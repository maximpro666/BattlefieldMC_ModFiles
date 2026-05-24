package com.pigeostudios.pwp.client;

import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.client.VehicleData;
import com.pigeostudios.pwp.network.TicketListSyncPacket;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ClientTeamData {

    public record ClientBeaconData(String name, double x, double y, double z, int teamOrdinal) {}
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
    public static int localPlayerWC = 0;
    public static int natoVC = 0;
    public static int russiaVC = 0;
    public static List<KitData> kits = new ArrayList<>();
    public static List<VehicleData> vehicles = new ArrayList<>();
    public static List<FOBData> fobs = new ArrayList<>();
    public static List<CapturePointData> capturePoints = new ArrayList<>();
    public static String language = "ru";
    public static KitData selectedKit = null;

    public static final Map<UUID, PlayerListEntry> playerDataMap = new HashMap<>();
    /** Full KitConfig JSON received from server for admin editing */
    public static String kitConfigEditJson = "";
    /** KitConfig JSON received from server for all players */
    public static String receivedKitConfigJson = "";
    public static final Map<UUID, Integer> playerTeamMap = new HashMap<>();
    public static final List<KitEntry> availableKits = new ArrayList<>();
    public static final List<VehicleEntry> availableVehicles = new ArrayList<>();
    public static int matchTimeSeconds = 0;
    public static float guiVolume = 1.0f;
    public static float guiScale = 1.0f;
    public static float guiOpacity = 1.0f;
    public static int maxTickets = 100;
    public static List<ClientBeaconData> beacons = new ArrayList<>();

    // Bleeding state (synced from server)
    public static boolean isBleeding = false;
    public static int bleedTimeRemaining = 0;
    public static UUID reviverUUID = null;
    public static int reviveProgress = 0;

    // HUD element visibility toggles
    public static boolean showCompass = true;
    public static boolean showTicketBar = true;
    public static boolean showSquad = true;
    public static boolean showVitals = true;
    public static boolean showHotbar = true;
    public static boolean showKillFeed = true;
    public static List<TicketListSyncPacket.TicketEntry> ticketList = new ArrayList<>();

    // Vote overlay data
    private static List<String> voteMapNames = new ArrayList<>();
    private static int[] voteCounts = new int[0];
    private static int voteRemainingSeconds = 0;
    private static String votedMap = "";

    public static void setVoteData(List<String> mapNames, int[] counts, int remainingSecs) {
        voteMapNames = mapNames != null ? new ArrayList<>(mapNames) : new ArrayList<>();
        voteCounts = counts != null ? counts.clone() : new int[0];
        voteRemainingSeconds = remainingSecs;
    }

    public static void resetVoteData() {
        voteMapNames = new ArrayList<>();
        voteCounts = new int[0];
        voteRemainingSeconds = 0;
        votedMap = "";
    }

    public static void updateVoteCounts(int remainingSecs, int[] counts) {
        voteRemainingSeconds = remainingSecs;
        if (counts != null && voteCounts.length == counts.length) {
            voteCounts = counts.clone();
        }
    }

    public static List<String> getVoteMapNames() { return voteMapNames; }
    public static int[] getVoteCounts() { return voteCounts; }
    public static int getVoteRemainingSeconds() { return voteRemainingSeconds; }
    public static String getVotedMap() { return votedMap; }
    public static void setVotedMap(String name) { votedMap = name != null ? name : ""; }
    public static Map<UUID, Integer> squadmateStatuses = new HashMap<>();

    public static final long SPEAKING_TIMEOUT_MS = 800;

    // Border zone data for client rendering (parallel lists)
    public static List<byte[]> borderZoneTypes = new ArrayList<>();
    public static List<double[]> borderZoneData = new ArrayList<>();

    // Base spawn positions
    private static int[] natoBasePos = null;
    private static int[] russiaBasePos = null;
    private static int baseRadius = 30;

    public static void setNatoBase(int x, int y, int z) { natoBasePos = new int[]{x, y, z}; }
    public static void setRussiaBase(int x, int y, int z) { russiaBasePos = new int[]{x, y, z}; }
    public static void setBaseRadius(int r) { baseRadius = r; }

    public static int[] getNatoBasePos() { return natoBasePos; }
    public static int[] getRussiaBasePos() { return russiaBasePos; }
    public static int getBaseRadius() { return baseRadius; }

    public static void clearBases() {
        natoBasePos = null;
        russiaBasePos = null;
        baseRadius = 30;
    }

    public static PlayerListEntry getPlayerData(UUID uuid) { return playerDataMap.get(uuid); }
    private static String lobbyStatus = "";

    public static void setLobbyStatus(String status) {
        lobbyStatus = status != null ? status : "";
    }

    public static String getLobbyStatus() {
        return lobbyStatus;
    }

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
        if (phase == 0 || phase == 1 || phase == 3) {
            clearBases();
        }
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
