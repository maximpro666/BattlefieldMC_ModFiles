package com.yourmod.teamsystem.client;

public record FOBData(int fobId, String name, double x, double y, double z,
                      String worldKey, int teamOrdinal, float health) {
}
