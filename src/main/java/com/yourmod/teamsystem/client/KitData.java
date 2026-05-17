package com.yourmod.teamsystem.client;

public record KitData(String name, String displayName, String description, String icon,
                      int minRank, int cooldown, boolean available, String loadoutJson) {
}
