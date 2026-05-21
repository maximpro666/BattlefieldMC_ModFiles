package com.yourmod.teamsystem.capture;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.MapConfig;
import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.stream.Collectors;

public class ZoneDataManager {
    private final Map<String, List<CaptureZone>> zonesByDimension = new HashMap<>();
    private boolean active;

    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }

    public List<CaptureZone> getZones(String dimension) {
        return zonesByDimension.getOrDefault(dimension, Collections.emptyList());
    }

    public List<CaptureZone> getAllZones() {
        return zonesByDimension.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public void addZone(CaptureZone zone) {
        zonesByDimension.computeIfAbsent(zone.getDimension(), k -> new ArrayList<>()).add(zone);
    }

    public void removeZone(String id, String dimension) {
        List<CaptureZone> list = zonesByDimension.get(dimension);
        if (list != null) list.removeIf(z -> z.getId().equals(id));
    }

    public CaptureZone getZone(String id, String dimension) {
        List<CaptureZone> list = zonesByDimension.get(dimension);
        if (list == null) return null;
        return list.stream().filter(z -> z.getId().equals(id)).findFirst().orElse(null);
    }

    public void clearZones(String dimension) {
        zonesByDimension.remove(dimension);
    }

    public void clearAll() {
        zonesByDimension.clear();
    }

    public void resetAll() {
        for (List<CaptureZone> list : zonesByDimension.values()) {
            for (CaptureZone zone : list) zone.reset();
        }
    }

    public void loadFromMapConfig(MapConfig map, String dimension, int zOffset) {
        clearZones(dimension);
        List<MapConfig.CapturePointEntry> entries = map.getCapturePoints();
        if (entries == null || entries.isEmpty()) return;

        int index = 0;
        for (MapConfig.CapturePointEntry entry : entries) {
            String id = "map_" + index;
            String name = entry.name != null ? entry.name : id;
            BlockPos center = new BlockPos(entry.x, entry.y, entry.z + zOffset);
            int radius = (int) Math.ceil(entry.radius);
            int captureSec = (int) Math.ceil(entry.captureSpeed * 20);
            CaptureZone zone = new CaptureZone(
                id, name, dimension,
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius),
                Math.max(5, captureSec),
                entry.type
            );
            zone.setVcRate(entry.getVcRate());
            addZone(zone);
            index++;
        }
        if (index > 0) {
            TeamSystem.LOGGER.info("Loaded {} capture zones from map config (zOffset={})", index, zOffset);
        }
    }
}
