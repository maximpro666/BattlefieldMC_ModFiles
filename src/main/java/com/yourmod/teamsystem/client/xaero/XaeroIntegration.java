package com.yourmod.teamsystem.client.xaero;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.FOBData;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.network.OpenSpawnSelectionScreenPacket.BeaconInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public final class XaeroIntegration {

    private XaeroIntegration() {}

    private static boolean       available  = false;
    private static final Set<String> activeIds = new HashSet<>();
    private static final String  PREFIX     = "[TS]";

    private static Class<?>  wpClass;
    private static Constructor<?> wpCtor;
    private static Class<?>  storeClass;
    private static Method    getInstance;
    private static Method    addWaypoint;
    private static Method    removeWaypoint;
    private static Method    getWaypoints;
    private static Method    getWaypointName;

    public static void init() {
        try {
            wpClass    = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            storeClass = Class.forName("xaero.common.minimap.waypoints.WaypointStore");

            wpCtor         = wpClass.getConstructor(int.class, int.class, int.class,
                    String.class, String.class, int.class,
                    boolean.class, boolean.class);
            getInstance    = storeClass.getMethod("getInstance");
            addWaypoint    = storeClass.getMethod("addWaypoint", wpClass);
            removeWaypoint = storeClass.getMethod("removeWaypoint", wpClass);
            getWaypoints   = storeClass.getMethod("getWaypoints");
            getWaypointName = wpClass.getMethod("getName");

            available = true;
            TeamSystem.LOGGER.info("[TeamSystem] Xaero's Minimap detected - waypoint integration enabled.");
        } catch (Exception e) {
            available = false;
            TeamSystem.LOGGER.info("[TeamSystem] Xaero's Minimap not found - waypoint integration disabled.");
        }
    }

    public static void clearAllWaypoints() {
        if (!available) return;
        try {
            Object store = getInstance.invoke(null);
            @SuppressWarnings("unchecked")
            List<Object> waypoints = new ArrayList<>((List<Object>) getWaypoints.invoke(store));
            for (Object wp : waypoints) {
                String name = (String) getWaypointName.invoke(wp);
                if (name != null && name.startsWith(PREFIX)) {
                    removeWaypoint.invoke(store, wp);
                }
            }
            activeIds.clear();
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("[TeamSystem] XaeroIntegration: error clearing waypoints", e);
        }
    }

    public static void updateFOBWaypoints(List<FOBData> fobs) {
        if (!available) return;
        removeByPrefix(PREFIX + " FOB");
        if (fobs == null) return;
        Team playerTeam = ClientTeamData.getLocalPlayerTeam();
        for (FOBData fob : fobs) {
            if (playerTeam != Team.SPECTATOR && fob.teamOrdinal() != playerTeam.ordinal()) continue;
            int color = fob.teamOrdinal() == 0 ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;
            addWaypoint(PREFIX + " FOB-" + fob.fobId(),
                    PREFIX + " FOB - " + fob.name(),
                    "\u2691",
                    (int) fob.x(), (int) fob.y(), (int) fob.z(),
                    color);
        }
    }

    public static void updateCapturePointWaypoints(List<CapturePointData> cps) {
        if (!available) return;
        removeByPrefix(PREFIX + " CP");
        if (cps == null) return;
        for (CapturePointData cp : cps) {
            int color;
            if (cp.ownerTeamOrdinal() == 0)      color = UITheme.TEAM_NATO;
            else if (cp.ownerTeamOrdinal() == 1)  color = UITheme.TEAM_RUSSIA;
            else                                   color = 0xFFFFFFFF;
            addWaypoint(PREFIX + " CP-" + cp.id(),
                    PREFIX + " CP - " + cp.name(),
                    "\u25CF",
                    (int) cp.x(), 64, (int) cp.z(),
                    color);
        }
    }

    public static void updateBeaconWaypoints(List<ClientTeamData.ClientBeaconData> beacons) {
        if (!available) return;
        removeByPrefix(PREFIX + " BCN");
        if (beacons == null) return;
        Team playerTeam = ClientTeamData.getLocalPlayerTeam();
        for (ClientTeamData.ClientBeaconData b : beacons) {
            if (playerTeam != Team.SPECTATOR && b.teamOrdinal() != playerTeam.ordinal()) continue;
            addWaypoint(PREFIX + " BCN-" + b.name(),
                    PREFIX + " BCN - " + b.name(),
                    "\u2605",
                    (int) b.x(), (int) b.y(), (int) b.z(),
                    0xFFFFAA00);
        }
    }

    private static void addWaypoint(String id, String name, String symbol,
                                     int x, int y, int z, int color) {
        if (!available) return;
        if (activeIds.contains(id)) return;
        try {
            Object store = getInstance.invoke(null);
            Object wp = wpCtor.newInstance(x, y, z, name, symbol, color, false, true);
            addWaypoint.invoke(store, wp);
            activeIds.add(id);
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("[TeamSystem] XaeroIntegration: failed to add waypoint '{}': {}",
                    id, e.getMessage());
        }
    }

    private static void removeByPrefix(String prefix) {
        if (!available) return;
        try {
            Object store = getInstance.invoke(null);
            @SuppressWarnings("unchecked")
            List<Object> waypoints = new ArrayList<>((List<Object>) getWaypoints.invoke(store));
            for (Object wp : waypoints) {
                String name = (String) getWaypointName.invoke(wp);
                if (name != null && name.startsWith(prefix)) {
                    removeWaypoint.invoke(store, wp);
                }
            }
            activeIds.removeIf(id -> id.startsWith(prefix));
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("[TeamSystem] XaeroIntegration: error removing waypoints", e);
        }
    }
}
