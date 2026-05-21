package com.pigeostudios.pwp.vehicle.adapter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SuperbVehicleAdapter implements IVehicleAdapter {
    private static Class<?> vehicleEntityClass;
    private static Method getOwnerUUIDMethod;
    private static boolean checked = false;

    private static void ensureReflection() {
        if (checked) return;
        checked = true;
        try {
            vehicleEntityClass = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            getOwnerUUIDMethod = vehicleEntityClass.getMethod("getOwnerUUID");
        } catch (Exception e) {
            vehicleEntityClass = null;
        }
    }

    @Override
    public boolean isApplicable(Entity entity) {
        ensureReflection();
        if (vehicleEntityClass == null) return false;
        return vehicleEntityClass.isAssignableFrom(entity.getClass());
    }

    @Override
    public String getVehicleId(Entity entity) {
        try {
            CompoundTag tag = new CompoundTag();
            entity.saveWithoutId(tag);
            if (tag.contains("vehicleId")) return tag.getString("vehicleId");
            if (tag.contains("id")) return tag.getString("id");
        } catch (Exception ignored) {}
        return entity.getType().getDescriptionId();
    }

    @Override
    public UUID getOwnerUUID(Entity entity) {
        ensureReflection();
        if (getOwnerUUIDMethod == null) return null;
        try {
            Object result = getOwnerUUIDMethod.invoke(entity);
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
            if (tag.contains("Health")) return tag.getFloat("Health");
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
        entity.getPersistentData().putFloat("Heal", (float) health);
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
