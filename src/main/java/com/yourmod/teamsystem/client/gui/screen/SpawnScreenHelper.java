package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.FOBData;
import com.yourmod.teamsystem.network.OpenSpawnSelectionScreenPacket;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class SpawnScreenHelper {
    private SpawnScreenHelper() {}

    private static final long STALE_MS = 120_000L; // cache valid for 2 minutes
    private static long lastUpdated;
    private static List<OpenSpawnSelectionScreenPacket.SquadmateInfo> lastSquadmates;
    private static List<FOBData> lastFobs;
    private static List<OpenSpawnSelectionScreenPacket.BeaconInfo> lastBeacons;
    private static int lastTeamOrdinal;
    private static String lastSelectedKit;

    public static boolean hasCachedData() {
        return lastSquadmates != null && (System.currentTimeMillis() - lastUpdated) < STALE_MS;
    }

    public static void openSpawnSelectionScreen(
            List<OpenSpawnSelectionScreenPacket.SquadmateInfo> squadmates,
            List<FOBData> fobs,
            List<OpenSpawnSelectionScreenPacket.BeaconInfo> beacons,
            int teamOrdinal,
            String selectedKit) {
        lastUpdated = System.currentTimeMillis();
        lastSquadmates = squadmates;
        lastFobs = fobs;
        lastBeacons = beacons;
        lastTeamOrdinal = teamOrdinal;
        lastSelectedKit = selectedKit;
        Minecraft.getInstance().setScreen(new SpawnSelectionScreen(
                squadmates, fobs, beacons, teamOrdinal, selectedKit));
    }

    public static void reopen() {
        if (hasCachedData()) {
            Minecraft.getInstance().setScreen(new SpawnSelectionScreen(
                    lastSquadmates, lastFobs, lastBeacons, lastTeamOrdinal, lastSelectedKit));
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public static void updateSelectedKit(String kit) {
        lastSelectedKit = kit;
        lastUpdated = System.currentTimeMillis();
    }

    public static String getLastSelectedKit() {
        return lastSelectedKit;
    }

    public static void closeSpawnScreen() {
        Minecraft.getInstance().setScreen(null);
    }

    public static void clear() {
        lastUpdated = 0;
        lastSquadmates = null;
        lastFobs = null;
        lastBeacons = null;
        lastTeamOrdinal = 0;
        lastSelectedKit = null;
    }
}
