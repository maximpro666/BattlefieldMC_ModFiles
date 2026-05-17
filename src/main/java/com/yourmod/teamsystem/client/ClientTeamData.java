package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.core.Team;

/**
 * Client-side cache of the local player's team and combat data.
 * Updated via CombatDataSyncPacket and TeamSyncPacket from the server.
 *
 * This data structure is used to display the player's team and statistics in the HUD,
 * and serves as the foundation for a custom TAB overlay implementation.
 *
 * TODO: Custom TAB Overlay
 * Future implementation should:
 * - Read localPlayerTeam, localPlayerKills, localPlayerDeaths from this class
 * - Hook into TabListScreenEvent or similar
 * - Render team-colored player names, kills, deaths in a custom scoreboard overlay
 * - Consider sorting by K/D ratio or kills
 * - Support sorting by team/squad for tactical information display
 *
 * Example entry point for custom overlay:
 * @see net.minecraftforge.event.ScreenEvent.Init.Post
 * Use ClientTeamData to populate dynamic HUD elements and leaderboards.
 *
 * Current usage: server syncs data via packets, client caches it here for UI readability.
 */
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
}
