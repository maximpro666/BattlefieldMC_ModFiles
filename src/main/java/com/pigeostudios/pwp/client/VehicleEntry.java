package com.pigeostudios.pwp.client;

public record VehicleEntry(String id, String name, String description, String iconName, int ticketCost, int bcCost, int minRankOrdinal, long cooldownEndMs) {}
