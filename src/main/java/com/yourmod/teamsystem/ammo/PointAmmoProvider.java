package com.yourmod.teamsystem.ammo;

import com.yourmod.teamsystem.capture.CaptureZone;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PointAmmoProvider implements IAmmoProvider {
    private static final int AMMO_RANGE = 5;

    private final CaptureZone zone;
    private final Map<String, Integer> ammoCatalog;

    public PointAmmoProvider(CaptureZone zone) {
        this.zone = zone;
        this.ammoCatalog = loadAmmoCatalog();
    }

    private static Map<String, Integer> loadAmmoCatalog() {
        Map<String, Integer> catalog = new HashMap<>();
        catalog.put("superbwarfare:rpg_rocket", 80);
        catalog.put("superbwarfare:tbg_rocket", 140);
        catalog.put("superbwarfare:javelin_missile", 400);
        catalog.put("superbwarfare:igla_missile", 350);

        catalog.put("superbwarfare:apfsds_shell", 120);
        catalog.put("superbwarfare:he_shell", 80);
        catalog.put("superbwarfare:heat_shell", 100);
        catalog.put("superbwarfare:atgm_missile", 300);

        catalog.put("superbwarfare:hydra_rocket", 250);
        catalog.put("superbwarfare:hellfire_missile", 350);

        catalog.put("superbwarfare:aim9_missile", 250);
        catalog.put("superbwarfare:aim120_missile", 600);
        catalog.put("superbwarfare:agm114_missile", 500);

        return catalog;
    }

    @Override
    public boolean isInRange(ServerPlayer player) {
        Team owner = zone.getOwnerTeam();
        if (!owner.isPlayable()) return false;
        if (zone.isContested()) return false;
        BlockPos playerPos = player.blockPosition();
        BlockPos center = zone.getCenter();
        double distance = Math.sqrt(
            Math.pow(playerPos.getX() - center.getX(), 2) +
            Math.pow(playerPos.getZ() - center.getZ(), 2)
        );
        return distance <= AMMO_RANGE;
    }

    @Override
    public Team getOwner() { return zone.getOwnerTeam(); }

    @Override
    public String getName() { return zone.getName(); }

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

    @Override
    public long getChunkKey() {
        BlockPos center = zone.getCenter();
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}