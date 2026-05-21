package com.yourmod.teamsystem.vehicle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VehicleDefinition {
    private String id;
    private String displayName;
    private String category;
    private String certification;
    private Costs costs;
    private Pressure pressure;
    private Map<String, Integer> ammo;
    private List<String> phases;
    private Map<String, Integer> populationLimits;
    private String entityType;
    private String nbt;
    private int cooldownSeconds = 60;
    private int ticketCost = 0;

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getCategory() { return category; }
    public String getCertification() { return certification; }
    public Costs getCosts() { return costs != null ? costs : new Costs(); }
    public Pressure getPressure() { return pressure != null ? pressure : new Pressure(); }
    public Map<String, Integer> getAmmo() { return ammo != null ? ammo : Collections.emptyMap(); }
    public List<String> getPhases() { return phases != null ? phases : List.of("TOTAL_WAR"); }
    public Map<String, Integer> getPopulationLimits() { return populationLimits != null ? populationLimits : Map.of("10", 1); }
    public String getEntityType() { return entityType; }
    public String getNbt() { return nbt; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public int getTicketCost() { return ticketCost; }

    public int getAmmoCost(String ammoId) {
        return ammo != null ? ammo.getOrDefault(ammoId, 0) : 0;
    }

    public int getPopulationLimit(int playerCount) {
        if (populationLimits == null) return 1;
        int best = 1;
        for (Map.Entry<String, Integer> entry : populationLimits.entrySet()) {
            try {
                int threshold = Integer.parseInt(entry.getKey());
                if (playerCount >= threshold) {
                    best = Math.max(best, entry.getValue());
                }
            } catch (NumberFormatException ignored) {}
        }
        return best;
    }

    public EntityType<?> resolveEntityType() {
        if (entityType == null || entityType.isEmpty()) return null;
        ResourceLocation id = new ResourceLocation(entityType);
        return BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    public CompoundTag resolveNbt() {
        if (nbt == null || nbt.isEmpty()) return null;
        try { return TagParser.parseTag(nbt); } catch (Exception e) { return null; }
    }

    public static class Costs {
        private int deployBC = 0;
        private int deployVC = 0;
        public int getDeployBC() { return deployBC; }
        public int getDeployVC() { return deployVC; }
    }

    public static class Pressure {
        private int ground = 0;
        private int air = 0;
        private int siege = 0;
        public int getGround() { return ground; }
        public int getAir() { return air; }
        public int getSiege() { return siege; }
    }

    private Upkeep upkeep;

    public Upkeep getUpkeep() { return upkeep != null ? upkeep : new Upkeep(); }

    public static class Upkeep {
        private int intervalSeconds = 0;
        private int bcCost = 0;
        private Map<String, Integer> ammoDrain;

        public int getIntervalSeconds() { return intervalSeconds; }
        public int getBcCost() { return bcCost; }
        public boolean isEnabled() { return intervalSeconds > 0 && bcCost > 0; }
        public Map<String, Integer> getAmmoDrain() {
            return ammoDrain != null ? ammoDrain : Collections.emptyMap();
        }
    }
}
