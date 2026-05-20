package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.capture.CaptureProcessor;
import com.yourmod.teamsystem.capture.CaptureZone;
import com.yourmod.teamsystem.capture.ZoneDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.stream.Collectors;

public class CapturePointManager {
    private final ZoneDataManager zoneData;

    public CapturePointManager() {
        this.zoneData = new ZoneDataManager();
    }

    public ZoneDataManager getZoneData() { return zoneData; }

    public boolean isActive() {
        return zoneData.isActive();
    }

    public void setActive(boolean a) {
        zoneData.setActive(a);
    }

    public boolean isCaptureEnabled() {
        MapPoolManager pool = TeamSystem.getMapPoolManager();
        return pool.getCurrentMap().map(MapConfig::hasCapturePoints).orElse(true);
    }

    public void clearPoints() {
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
            zoneData.loadFromMapConfig(map, dimId.toString(), zOffset);
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
                String id = "map_" + entry.hashCode();
                if (zoneData.getZone(id, dimension) != null) return;
                zoneData.addZone(new CaptureZone(id, name, dimension,
                    center.offset(-radius, -radius, -radius),
                    center.offset(radius, radius, radius),
                    Math.max(5, captureSec)));
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
        return TeamSystem.getGameManager().getServer().getLevel(
            DynamicDimensionManager.getDimKey());
    }
}
