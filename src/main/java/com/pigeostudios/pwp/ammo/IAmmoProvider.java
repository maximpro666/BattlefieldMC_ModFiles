package com.pigeostudios.pwp.ammo;

import com.pigeostudios.pwp.core.Team;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public interface IAmmoProvider {
    boolean isInRange(ServerPlayer player);
    Team getOwner();
    String getName();
    boolean providesFreeAmmo();
    boolean providesPaidAmmo();
    Map<String, Integer> getAmmoCatalog();
    int getAmmoCost(String ammoId);

    default long getChunkKey() { return Long.MIN_VALUE; }
}
