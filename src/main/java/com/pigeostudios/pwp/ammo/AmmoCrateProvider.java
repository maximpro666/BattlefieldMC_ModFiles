package com.pigeostudios.pwp.ammo;

import com.pigeostudios.pwp.core.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AmmoCrateProvider implements IAmmoProvider {
    private static final int CRATE_RANGE = 2;

    private final Team owner;
    private final BlockPos pos;
    private final String name;
    private final Map<String, Integer> ammoCatalog;

    public AmmoCrateProvider(Team owner, BlockPos pos, String name) {
        this.owner = owner;
        this.pos = pos;
        this.name = name;
        this.ammoCatalog = loadAmmoCatalog();
    }

    private static Map<String, Integer> loadAmmoCatalog() {
        Map<String, Integer> catalog = new HashMap<>();
        catalog.put("superbwarfare:rpg_rocket", 120);
        return catalog;
    }

    @Override
    public boolean isInRange(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        double distance = Math.sqrt(
            Math.pow(playerPos.getX() - pos.getX(), 2) +
            Math.pow(playerPos.getY() - pos.getY(), 2) +
            Math.pow(playerPos.getZ() - pos.getZ(), 2)
        );
        return distance <= CRATE_RANGE;
    }

    @Override
    public Team getOwner() { return owner; }

    @Override
    public String getName() { return name; }

    @Override
    public boolean providesFreeAmmo() { return true; }

    @Override
    public boolean providesPaidAmmo() { return false; }

    @Override
    public Map<String, Integer> getAmmoCatalog() { return Collections.unmodifiableMap(ammoCatalog); }

    @Override
    public int getAmmoCost(String ammoId) {
        return ammoCatalog.getOrDefault(ammoId, 0);
    }

    @Override
    public long getChunkKey() {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public BlockPos getPos() { return pos; }
}
