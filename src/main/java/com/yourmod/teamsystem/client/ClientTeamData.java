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

    public static void setLocalPlayerTeam(Team team) {
        localPlayerTeam = team;
    }

    public static void setLocalPlayerData(Team team, int kills, int deaths) {
        localPlayerTeam = team;
        localPlayerKills = kills;
        localPlayerDeaths = deaths;
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
}
