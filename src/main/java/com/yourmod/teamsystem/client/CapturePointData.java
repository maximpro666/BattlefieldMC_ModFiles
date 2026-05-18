package com.yourmod.teamsystem.client;

public record CapturePointData(int id, double progress, int ownerTeamOrdinal,
                               String name, int capturingTeamOrdinal,
                               double x, double y, double z) {
}
