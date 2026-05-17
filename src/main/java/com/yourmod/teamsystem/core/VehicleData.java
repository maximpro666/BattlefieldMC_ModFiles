package com.yourmod.teamsystem.core;

import com.google.gson.JsonObject;

public class VehicleData {
    private String vehicleId;
    private String displayName;
    private Team team;
    private int minRankOrdinal;
    private int ticketCost;

    public VehicleData(String vehicleId, String displayName, Team team, int minRankOrdinal, int ticketCost) {
        this.vehicleId = vehicleId;
        this.displayName = displayName;
        this.team = team;
        this.minRankOrdinal = Math.max(0, minRankOrdinal);
        this.ticketCost = Math.max(1, ticketCost);
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Team getTeam() {
        return team;
    }

    public int getMinRankOrdinal() {
        return minRankOrdinal;
    }

    public int getTicketCost() {
        return ticketCost;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("vehicleId", vehicleId);
        obj.addProperty("displayName", displayName);
        obj.addProperty("team", team.getName());
        obj.addProperty("minRankOrdinal", minRankOrdinal);
        obj.addProperty("ticketCost", ticketCost);
        return obj;
    }

    public static VehicleData fromJson(JsonObject obj) {
        String vehicleId = obj.get("vehicleId").getAsString();
        String displayName = obj.get("displayName").getAsString();
        Team team = Team.fromString(obj.get("team").getAsString());
        int minRankOrdinal = obj.get("minRankOrdinal").getAsInt();
        int ticketCost = obj.get("ticketCost").getAsInt();

        return new VehicleData(vehicleId, displayName, team, minRankOrdinal, ticketCost);
    }
}
