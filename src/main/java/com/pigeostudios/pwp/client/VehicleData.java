package com.pigeostudios.pwp.client;

public record VehicleData(String vehicleId, String displayName, String description, String icon,
                          int ticketCost, int bcCost, int vcCost, int minRank, int cooldown, boolean available) {
}
