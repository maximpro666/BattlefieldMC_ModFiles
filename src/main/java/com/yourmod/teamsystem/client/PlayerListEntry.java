package com.yourmod.teamsystem.client;

import java.util.UUID;

public record PlayerListEntry(String rank, String callsign, String squad, int kills, int deaths, int teamOrdinal, boolean isDowned) {}
