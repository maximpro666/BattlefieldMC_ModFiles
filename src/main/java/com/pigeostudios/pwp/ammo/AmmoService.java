package com.pigeostudios.pwp.ammo;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class AmmoService {
    private final List<IAmmoProvider> providers = new ArrayList<>();
    private final Map<Long, List<IAmmoProvider>> spatialIndex = new HashMap<>();

    public void registerProvider(IAmmoProvider provider) {
        providers.add(provider);
        long key = provider.getChunkKey();
        if (key != Long.MIN_VALUE) {
            spatialIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(provider);
        }
    }

    public void unregisterProvider(IAmmoProvider provider) {
        providers.remove(provider);
        long key = provider.getChunkKey();
        if (key != Long.MIN_VALUE) {
            List<IAmmoProvider> list = spatialIndex.get(key);
            if (list != null) {
                list.remove(provider);
                if (list.isEmpty()) spatialIndex.remove(key);
            }
        }
    }

    public void clear() {
        providers.clear();
        spatialIndex.clear();
    }

    public List<IAmmoProvider> getProvidersForPlayer(ServerPlayer player) {
        List<IAmmoProvider> result = new ArrayList<>();
        for (IAmmoProvider provider : getProvidersInRange(player)) {
            if (provider.isInRange(player)) result.add(provider);
        }
        return result;
    }

    public boolean hasAnyProvider(ServerPlayer player) {
        for (IAmmoProvider provider : getProvidersInRange(player)) {
            if (provider.isInRange(player)) return true;
        }
        return false;
    }

    public boolean hasFriendlyProvider(ServerPlayer player) {
        Team playerTeam = PWP.getTeamManager()
            .getOrCreatePlayerData(player.getUUID()).getTeam();
        for (IAmmoProvider provider : getProvidersInRange(player)) {
            if (provider.isInRange(player) && provider.getOwner() == playerTeam) return true;
        }
        return false;
    }

    public IAmmoProvider getBestProvider(ServerPlayer player) {
        List<IAmmoProvider> available = getProvidersForPlayer(player);
        if (available.isEmpty()) return null;
        return available.get(0);
    }

    public int getProviderCount() {
        return providers.size();
    }

    private List<IAmmoProvider> getProvidersInRange(ServerPlayer player) {
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        Set<IAmmoProvider> seen = new HashSet<>();
        List<IAmmoProvider> nearby = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = ((long) (cx + dx) << 32) | ((cz + dz) & 0xFFFFFFFFL);
                List<IAmmoProvider> bucket = spatialIndex.get(key);
                if (bucket != null) {
                    for (IAmmoProvider p : bucket) {
                        if (seen.add(p)) nearby.add(p);
                    }
                }
            }
        }

        for (IAmmoProvider p : providers) {
            if (p.getChunkKey() == Long.MIN_VALUE) {
                if (seen.add(p)) nearby.add(p);
            }
        }

        return nearby;
    }
}
