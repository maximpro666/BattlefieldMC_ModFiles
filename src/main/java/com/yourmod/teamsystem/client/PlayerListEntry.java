package com.yourmod.teamsystem.client;

import java.util.UUID;

public record PlayerListEntry(int rank, String callsign, String squad, int kills, int deaths, int teamOrdinal, String squadName, int donatTier) {
    public PlayerListEntry(int rank, String callsign, String squad, int kills, int deaths, int teamOrdinal, String squadName) {
        this(rank, callsign, squad, kills, deaths, teamOrdinal, squadName, 0);
    }
}
