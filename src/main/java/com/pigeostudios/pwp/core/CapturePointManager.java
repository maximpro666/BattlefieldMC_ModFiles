package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.ammo.AmmoService;
import com.pigeostudios.pwp.ammo.PointAmmoProvider;
import com.pigeostudios.pwp.capture.CaptureProcessor;
import com.pigeostudios.pwp.capture.CaptureZone;
import com.pigeostudios.pwp.capture.ZoneDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.*;

public class CapturePointManager {
    private final ZoneDataManager zoneData;
    private final Map<String, PointAmmoProvider> pointAmmoProviders = new HashMap<>();

    public CapturePointManager() {
        this.zoneData = new ZoneDataManager();
    }

    private AmmoService ammo() {
        return BattlefieldRuntime.getInstance().getAmmoService();
    }

    private void registerAmmoProvidersForDimension(String dimension) {
        for (CaptureZone zone : zoneData.getZones(dimension)) {
            if (!pointAmmoProviders.containsKey(zone.getId())) {
                PointAmmoProvider provider = new PointAmmoProvider(zone);
                pointAmmoProviders.put(zone.getId(), provider);
                ammo().registerProvider(provider);
            }
        }
    }

    private void unregisterAmmoProvidersForDimension(String dimension) {
        for (CaptureZone zone : zoneData.getZones(dimension)) {
            PointAmmoProvider provider = pointAmmoProviders.remove(zone.getId());
            if (provider != null) {
                ammo().unregisterProvider(provider);
            }
        }
    }

    private void unregisterAllAmmoProviders() {
        for (PointAmmoProvider provider : pointAmmoProviders.values()) {
            ammo().unregisterProvider(provider);
        }
        pointAmmoProviders.clear();
    }

    public ZoneDataManager getZoneData() { return zoneData; }

    public boolean isActive() {
        return zoneData.isActive();
    }

    public void setActive(boolean a) {
        zoneData.setActive(a);
    }

    public boolean isCaptureEnabled() {
        MapPoolManager pool = PWP.getMapPoolManager();
        return pool.getCurrentMap().map(MapConfig::hasCapturePoints).orElse(true);
    }

    public void clearPoints() {
        unregisterAllAmmoProviders();
        zoneData.clearAll();
    }

    public void resetAllPoints() {
        zoneData.resetAll();
    }

    public List<CaptureZone> getActiveZones() {
        ResourceLocation dimId = getCurrentMapDimension();
        if (dimId == null) return Collections.emptyList();
        return zoneData.getZones(dimId.toString());
    }

    public void loadFromMapConfig(MapConfig map, int zOffset) {
        ResourceLocation dimId = getCurrentMapDimension();
        if (dimId != null) {
            String dimension = dimId.toString();
            unregisterAmmoProvidersForDimension(dimension);
            zoneData.loadFromMapConfig(map, dimension, zOffset);
            registerAmmoProvidersForDimension(dimension);
        }
    }

    public void addPointFromConfig(MapConfig map, String name) {
        ResourceLocation dimId = getCurrentMapDimension();
        if (dimId == null) return;
        String dimension = dimId.toString();
        for (var entry : map.getCapturePoints()) {
            if (entry.name.equals(name)) {
                BlockPos center = new BlockPos(entry.x, entry.y, entry.z);
                int radius = (int) Math.ceil(entry.radius);
                int captureSec = (int) Math.ceil(entry.captureSpeed * 10);
                String id = "map_" + entry.name + "_" + entry.x + "_" + entry.y + "_" + entry.z;
                if (zoneData.getZone(id, dimension) != null) return;
                CaptureZone zone = new CaptureZone(id, name, dimension,
                    center.offset(-radius, -radius, -radius),
                    center.offset(radius, radius, radius),
                    Math.max(5, captureSec));
                zoneData.addZone(zone);
                PointAmmoProvider provider = new PointAmmoProvider(zone);
                pointAmmoProviders.put(zone.getId(), provider);
                ammo().registerProvider(provider);
                return;
            }
        }
    }

    public void tickCapturePoints(ServerLevel level) {
        if (!isCaptureEnabled() || !zoneData.isActive()) return;
        ResourceLocation currentDim = level.dimension().location();
        ResourceLocation mapDim = getCurrentMapDimension();
        if (mapDim != null && !mapDim.equals(currentDim)) return;
        CaptureProcessor.tick(level, zoneData);
    }

    public void syncToAll() {
        ResourceLocation dimId = getCurrentMapDimension();
        if (dimId == null) return;
        List<CaptureZone> zones = zoneData.getZones(dimId.toString());
        if (zones.isEmpty()) return;
        ServerLevel level = getCurrentMapLevel();
        if (level != null) CaptureProcessor.syncToAll(level, zones);
    }

    public void syncToPlayer(ServerPlayer player) {
        ResourceLocation dimId = getCurrentMapDimension();
        if (dimId == null) return;
        List<CaptureZone> zones = zoneData.getZones(dimId.toString());
        if (zones.isEmpty()) return;
        CaptureProcessor.syncToPlayer(player, zones);
    }

    ResourceLocation getCurrentMapDimension() {
        return DynamicDimensionManager.getDimKey().location();
    }

    private ServerLevel getCurrentMapLevel() {
        return PWP.getGameManager().getServer().getLevel(
            DynamicDimensionManager.getDimKey());
    }
}
