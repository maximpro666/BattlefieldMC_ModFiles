package com.yourmod.teamsystem.vehicle.adapter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public interface IVehicleAdapter {
    boolean isApplicable(Entity entity);
    String getVehicleId(Entity entity);
    UUID getOwnerUUID(Entity entity);
    int getSeatCount(Entity entity);
    int getOccupiedSeatIndex(Entity entity, Player player);
    double getHealth(Entity entity);
    double getMaxHealth(Entity entity);
    double getFuel(Entity entity);
    void setHealth(Entity entity, double health);
    void setFuel(Entity entity, double fuel);
    boolean isDriver(Entity entity, Player player);
    String getCategory(Entity entity);
}
