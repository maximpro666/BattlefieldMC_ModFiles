package com.pigeostudios.pwp.vehicle.adapter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class AshVehicleAdapter implements IVehicleAdapter {
    private static Class<?> ashVehicleClass;
    private static boolean checked = false;

    private static void ensureReflection() {
        if (checked) return;
        checked = true;
        try {
            ashVehicleClass = Class.forName("com.ashvehicle.entity.VehicleEntity");
        } catch (Exception e) {
            ashVehicleClass = null;
        }
    }

    @Override
    public boolean isApplicable(Entity entity) {
        ensureReflection();
        if (ashVehicleClass == null) return false;
        return ashVehicleClass.isAssignableFrom(entity.getClass());
    }

    @Override
    public String getVehicleId(Entity entity) {
        try {
            CompoundTag tag = new CompoundTag();
            entity.saveWithoutId(tag);
            if (tag.contains("vehicleId")) return tag.getString("vehicleId");
        } catch (Exception ignored) {}
        return entity.getType().getDescriptionId();
    }

    @Override
    public UUID getOwnerUUID(Entity entity) {
        try {
            var method = entity.getClass().getMethod("getOwnerUUID");
            Object result = method.invoke(entity);
            if (result instanceof UUID) return (UUID) result;
        } catch (Exception ignored) {}
        try {
            var method = entity.getClass().getMethod("getOwner");
            Object result = method.invoke(entity);
            if (result instanceof UUID) return (UUID) result;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public int getSeatCount(Entity entity) {
        return entity.getPassengers().size() + 1;
    }

    @Override
    public int getOccupiedSeatIndex(Entity entity, Player player) {
        return entity.getPassengers().indexOf(player);
    }

    @Override
    public double getHealth(Entity entity) {
        try {
            CompoundTag tag = new CompoundTag();
            entity.saveWithoutId(tag);
            if (tag.contains("Health")) return tag.getFloat("Health");
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public double getMaxHealth(Entity entity) {
        try {
            CompoundTag tag = new CompoundTag();
            entity.saveWithoutId(tag);
            if (tag.contains("MaxHealth")) return tag.getFloat("MaxHealth");
        } catch (Exception ignored) {}
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            try {
                var attr = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (attr != null) return attr.getValue();
            } catch (Exception ignored) {}
        }
        return 100;
    }

    @Override
    public double getFuel(Entity entity) {
        return 100;
    }

    @Override
    public void setHealth(Entity entity, double health) {
        entity.getPersistentData().putFloat("Health", (float) health);
    }

    @Override
    public void setFuel(Entity entity, double fuel) {}

    @Override
    public boolean isDriver(Entity entity, Player player) {
        return getOccupiedSeatIndex(entity, player) == 0;
    }

    @Override
    public String getCategory(Entity entity) {
        return "mbt";
    }
}
