package com.pigeostudios.pwp.core;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

@Deprecated
public class VehicleData {
    private String vehicleId;
    private String displayName;
    private Team team;
    private int minRankOrdinal;
    private int ticketCost;
    private int cooldownSeconds;
    private int maxActive;
    private String entityType;
    private String nbtData;

    public VehicleData(String vehicleId, String displayName, Team team, int minRankOrdinal, int ticketCost) {
        this.vehicleId = vehicleId;
        this.displayName = displayName;
        this.team = team;
        this.minRankOrdinal = Math.max(0, minRankOrdinal);
        this.ticketCost = Math.max(1, ticketCost);
        this.cooldownSeconds = 60;
        this.maxActive = 3;
        this.entityType = "";
        this.nbtData = "";
    }

    public String getVehicleId() { return vehicleId; }
    public String getDisplayName() { return displayName; }
    public Team getTeam() { return team; }
    public int getMinRankOrdinal() { return minRankOrdinal; }
    public int getTicketCost() { return ticketCost; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int s) { this.cooldownSeconds = s; }
    public int getMaxActive() { return maxActive; }
    public void setMaxActive(int m) { this.maxActive = m; }
    public String getEntityType() { return entityType; }
    public String getNbtData() { return nbtData; }

    public void setEntityData(String entityType, CompoundTag nbt) {
        this.entityType = entityType;
        this.nbtData = nbt != null ? nbt.toString() : "";
    }

    public EntityType<?> resolveEntityType() {
        if (entityType == null || entityType.isEmpty()) return null;
        ResourceLocation id = new ResourceLocation(entityType);
        return BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    public CompoundTag resolveNbt() {
        if (nbtData == null || nbtData.isEmpty()) return null;
        try { return TagParser.parseTag(nbtData); } catch (Exception e) { return null; }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("vehicleId", vehicleId);
        obj.addProperty("displayName", displayName);
        obj.addProperty("team", team.getName());
        obj.addProperty("minRankOrdinal", minRankOrdinal);
        obj.addProperty("ticketCost", ticketCost);
        obj.addProperty("cooldownSeconds", cooldownSeconds);
        obj.addProperty("maxActive", maxActive);
        if (entityType != null && !entityType.isEmpty()) obj.addProperty("entityType", entityType);
        if (nbtData != null && !nbtData.isEmpty()) obj.addProperty("nbt", nbtData);
        return obj;
    }

    public static VehicleData fromJson(JsonObject obj) {
        String vehicleId = obj.get("vehicleId").getAsString();
        String displayName = obj.get("displayName").getAsString();
        Team team = Team.fromString(obj.get("team").getAsString());
        int minRankOrdinal = obj.get("minRankOrdinal").getAsInt();
        int ticketCost = obj.get("ticketCost").getAsInt();
        VehicleData data = new VehicleData(vehicleId, displayName, team, minRankOrdinal, ticketCost);
        if (obj.has("cooldownSeconds")) data.cooldownSeconds = obj.get("cooldownSeconds").getAsInt();
        if (obj.has("maxActive")) data.maxActive = obj.get("maxActive").getAsInt();
        if (obj.has("entityType")) data.entityType = obj.get("entityType").getAsString();
        if (obj.has("nbt")) data.nbtData = obj.get("nbt").getAsString();
        return data;
    }
}
