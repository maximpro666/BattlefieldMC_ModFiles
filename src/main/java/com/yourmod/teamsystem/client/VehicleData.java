package com.yourmod.teamsystem.client;

public record VehicleData(String vehicleId, String displayName, String description, String icon,
                          int ticketCost, int minRank, int cooldown, boolean available) {
}
