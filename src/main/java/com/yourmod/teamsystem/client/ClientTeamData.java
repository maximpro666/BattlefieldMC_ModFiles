package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.core.Team;

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
