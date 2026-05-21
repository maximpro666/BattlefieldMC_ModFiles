package com.yourmod.teamsystem.ammo;

import com.yourmod.teamsystem.core.Team;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SupplyTruckAmmoProvider implements IAmmoProvider {
    private static final int SUPPLY_RANGE = 4;

    private final Team owner;
    private final UUID entityUuid;
    private final String name;
    private final Map<String, Integer> ammoCatalog;

    public SupplyTruckAmmoProvider(Team owner, UUID entityUuid, String name) {
        this.owner = owner;
        this.entityUuid = entityUuid;
        this.name = name;
        this.ammoCatalog = loadAmmoCatalog();
    }

    private static Map<String, Integer> loadAmmoCatalog() {
        Map<String, Integer> catalog = new HashMap<>();
        catalog.put("superbwarfare:rpg_rocket", 100);
        catalog.put("superbwarfare:tbg_rocket", 180);
        catalog.put("superbwarfare:igla_missile", 400);
        return catalog;
    }

    @Override
    public boolean isInRange(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntity(entityUuid);
        if (entity == null || !entity.isAlive()) return false;
        double distance = player.distanceTo(entity);
        return distance <= SUPPLY_RANGE;
    }

    @Override
    public Team getOwner() { return owner; }

    @Override
    public String getName() { return name; }

    @Override
    public boolean providesFreeAmmo() { return true; }

    @Override
    public boolean providesPaidAmmo() { return true; }

    @Override
    public Map<String, Integer> getAmmoCatalog() { return Collections.unmodifiableMap(ammoCatalog); }

    @Override
    public int getAmmoCost(String ammoId) {
        return ammoCatalog.getOrDefault(ammoId, 0);
    }

    public UUID getEntityUuid() { return entityUuid; }
}
