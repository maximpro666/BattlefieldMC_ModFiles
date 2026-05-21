package com.yourmod.teamsystem.ammo;

import com.yourmod.teamsystem.core.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FOBAmmoProvider implements IAmmoProvider {
    private static final int FOB_AMMO_RANGE = 8;

    private final Team owner;
    private final BlockPos center;
    private final String name;
    private final Map<String, Integer> ammoCatalog;

    public FOBAmmoProvider(Team owner, BlockPos center, String name) {
        this.owner = owner;
        this.center = center;
        this.name = name;
        this.ammoCatalog = new HashMap<>();
        ammoCatalog.put("superbwarfare:rpg_rocket", 80);
        ammoCatalog.put("superbwarfare:tbg_rocket", 140);
    }

    @Override
    public boolean isInRange(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        double distance = Math.sqrt(
            Math.pow(playerPos.getX() - center.getX(), 2) +
            Math.pow(playerPos.getZ() - center.getZ(), 2)
        );
        return distance <= FOB_AMMO_RANGE;
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

    @Override
    public long getChunkKey() {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
