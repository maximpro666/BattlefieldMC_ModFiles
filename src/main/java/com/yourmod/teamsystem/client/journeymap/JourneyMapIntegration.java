package com.yourmod.teamsystem.client.journeymap;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.FOBData;

import java.util.List;

public final class JourneyMapIntegration {

    private JourneyMapIntegration() {}

    public static boolean isAvailable() { return false; }

    public static void init() {
        TeamSystem.LOGGER.info("[TeamSystem] JourneyMap integration disabled");
    }

    static void onPluginInit(Object api) {
    }

    static void resendAll() {
    }

    public static void clearAll() {
    }

    public static void updateFOBWaypoints(List<FOBData> fobs) {
    }

    public static void updateCapturePointWaypoints(List<CapturePointData> cps) {
    }

    public static void updateBeaconWaypoints(List<ClientTeamData.ClientBeaconData> beacons) {
    }

    public static void updateMarkerWaypoints() {
    }

    public static void updateBaseWaypoints() {
    }
}
