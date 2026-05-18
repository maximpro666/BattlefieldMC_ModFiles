package com.yourmod.teamsystem.client;

import java.util.UUID;

public record PlayerListEntry(int rank, String callsign, String squad, int kills, int deaths, int teamOrdinal) {}
