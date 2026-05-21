package com.pigeostudios.pwp.client.journeymap;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.client.CapturePointData;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.FOBData;

import java.util.List;

public final class JourneyMapIntegration {

    private JourneyMapIntegration() {}

    public static boolean isAvailable() { return false; }

    public static void init() {
        PWP.LOGGER.info("[PWP] JourneyMap integration disabled");
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
