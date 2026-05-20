package com.yourmod.teamsystem.client.journeymap;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.FOBData;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public final class JourneyMapIntegration {

    private JourneyMapIntegration() {}

    private static boolean available = false;
    private static final Set<String> activeIds = new HashSet<>();
    private static final String PREFIX = "[TS]";

    private static Object apiInstance;
    private static Method showMethod;
    private static Method removeMethod;
    private static Constructor<?> waypointCtor;

    public static boolean isAvailable() { return available; }

    public static void init() {
        TeamSystem.LOGGER.info("[TeamSystem] JourneyMap integration waiting for plugin...");
    }

    static void onPluginInit(Object api) {
        try {
            apiInstance = api;
            showMethod = api.getClass().getMethod("show", Class.forName("journeymap.client.api.display.Displayable"));
            removeMethod = api.getClass().getMethod("remove", Class.forName("journeymap.client.api.display.Displayable"));
            waypointCtor = Class.forName("journeymap.client.api.display.Waypoint")
                    .getConstructor(String.class, String.class, String.class, int.class, BlockPos.class);
            available = true;
            TeamSystem.LOGGER.info("[TeamSystem] JourneyMap integration enabled.");
        } catch (Exception e) {
            available = false;
            TeamSystem.LOGGER.warn("[TeamSystem] JourneyMap integration failed to initialize: {}", e.getMessage());
        }
    }

    static void resendAll() {
        if (!available) return;
        activeIds.clear();
        List<FOBData> fobs = ClientTeamData.fobs;
        if (fobs != null) updateFOBWaypoints(fobs);
        List<CapturePointData> cps = ClientTeamData.capturePoints;
        if (cps != null) updateCapturePointWaypoints(cps);
        List<ClientTeamData.ClientBeaconData> beacons = ClientTeamData.beacons;
        if (beacons != null) updateBeaconWaypoints(beacons);
    }

    public static void clearAll() {
        if (!available || apiInstance == null) return;
        try {
            for (String id : activeIds) {
                Object wp = waypointCtor.newInstance("teamsystem", id, id, Minecraft.getInstance().level.dimensionType().hashCode(), BlockPos.ZERO);
                removeMethod.invoke(apiInstance, wp);
            }
            activeIds.clear();
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("[TeamSystem] JourneyMapIntegration: error clearing waypoints", e);
        }
    }

    public static void updateFOBWaypoints(List<FOBData> fobs) {
        if (!available) return;
        removeByPrefix(PREFIX + " FOB");
        if (fobs == null) return;
        Team playerTeam = ClientTeamData.getLocalPlayerTeam();
        int dim = getDimension();
        for (FOBData fob : fobs) {
            if (playerTeam != Team.SPECTATOR && fob.teamOrdinal() != playerTeam.ordinal()) continue;
            int color = fob.teamOrdinal() == 0 ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;
            addWaypoint(PREFIX + " FOB-" + fob.fobId(), PREFIX + " FOB - " + fob.name(),
                    color, (int) fob.x(), (int) fob.y(), (int) fob.z(), dim);
        }
    }

    public static void updateCapturePointWaypoints(List<CapturePointData> cps) {
        if (!available) return;
        removeByPrefix(PREFIX + " CP");
        if (cps == null) return;
        int dim = getDimension();
        for (CapturePointData cp : cps) {
            int color;
            if (cp.ownerTeamOrdinal() == 0)      color = UITheme.TEAM_NATO;
            else if (cp.ownerTeamOrdinal() == 1)  color = UITheme.TEAM_RUSSIA;
            else                                   color = 0xFFFFFFFF;
            addWaypoint(PREFIX + " CP-" + cp.id(), PREFIX + " CP - " + cp.name(),
                    color, (int) cp.x(), 64, (int) cp.z(), dim);
        }
    }

    public static void updateBeaconWaypoints(List<ClientTeamData.ClientBeaconData> beacons) {
        if (!available) return;
        removeByPrefix(PREFIX + " BCN");
        if (beacons == null) return;
        Team playerTeam = ClientTeamData.getLocalPlayerTeam();
        int dim = getDimension();
        for (ClientTeamData.ClientBeaconData b : beacons) {
            if (playerTeam != Team.SPECTATOR && b.teamOrdinal() != playerTeam.ordinal()) continue;
            addWaypoint(PREFIX + " BCN-" + b.name(), PREFIX + " BCN - " + b.name(),
                    0xFFFFAA00, (int) b.x(), (int) b.y(), (int) b.z(), dim);
        }
    }

    private static void addWaypoint(String id, String name, int colorArgb,
                                     int x, int y, int z, int dim) {
        if (!available || apiInstance == null) return;
        if (activeIds.contains(id)) return;
        try {
            BlockPos pos = new BlockPos(x, y, z);
            Object wp = waypointCtor.newInstance("teamsystem", id, name, dim, pos);
            wp.getClass().getMethod("setColor", int.class).invoke(wp, colorArgb & 0xFFFFFF);
            wp.getClass().getMethod("setEditable", boolean.class).invoke(wp, false);
            wp.getClass().getMethod("setPersistent", boolean.class).invoke(wp, false);
            showMethod.invoke(apiInstance, wp);
            activeIds.add(id);
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("[TeamSystem] JourneyMapIntegration: failed to add waypoint '{}': {}", id, e.getMessage());
        }
    }

    private static void removeByPrefix(String prefix) {
        if (!available || apiInstance == null) return;
        activeIds.removeIf(id -> {
            if (id.startsWith(prefix)) {
                try {
                    Object wp = waypointCtor.newInstance("teamsystem", id, id, 0, BlockPos.ZERO);
                    removeMethod.invoke(apiInstance, wp);
                } catch (Exception ignored) {}
                return true;
            }
            return false;
        });
    }

    private static int getDimension() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.dimensionType().hashCode() : 0;
    }

}