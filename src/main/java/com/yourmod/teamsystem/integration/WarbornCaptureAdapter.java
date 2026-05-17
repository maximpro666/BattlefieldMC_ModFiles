package com.yourmod.teamsystem.integration;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.TicketManager;
import net.minecraftforge.fml.ModList;

/**
 * Adapter for integrating with the WarBorn Capture Points mod.
 *
 * This class does not attempt to call WarBorn classes directly (to avoid hard
 * compile-time deps). Instead it detects presence of the mod and exposes a
 * public entrypoint method onPointsUpdated that WarBorn (or any integration)
 * can call to notify this mod about current owned-point counts.
 *
 * Behaviour:
 * - When WarBorn is present we disable internal ticking of CapturePointManager
 *   in GameManager (TeamSystem queries this via isEnabled()).
 * - External mods should call onPointsUpdated(natoPoints, russiaPoints) when
 *   ownership changes; this will update bleed rates on TicketManager.
 */
public final class WarbornCaptureAdapter {
    private static boolean enabled = false;

    private WarbornCaptureAdapter() {}

    public static void init() {
        // Common mod ids to try; the CurseForge file used name 'wrbcapturepoint'
        // but this is non-guaranteed. We'll check for a few likely ids.
        ModList ml = ModList.get();
        if (ml.isLoaded("wrbcapturepoint") || ml.isLoaded("warborncapturepoints") || ml.isLoaded("warborn-capture-points") || ml.isLoaded("warborn")) {
            enabled = true;
            TeamSystem.LOGGER.info("Warborn Capture Points detected - enabling integration adapter");
        } else {
            enabled = false;
            TeamSystem.LOGGER.info("Warborn Capture Points not found - integration adapter disabled");
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * External entrypoint: notify the teamsystem about current owned capture points.
     * Call this with the number of points currently owned by each team. This method
     * will compute bleed rates and update the TicketManager accordingly.
     */
    public static void onPointsUpdated(int natoPoints, int russiaPoints) {
        TicketManager tm = TeamSystem.getTicketManager();
        if (tm == null) return;

        int natoBleed = Math.max(0, russiaPoints - natoPoints);
        int russiaBleed = Math.max(0, natoPoints - russiaPoints);

        tm.setBleedRate(com.yourmod.teamsystem.core.Team.NATO, natoBleed);
        tm.setBleedRate(com.yourmod.teamsystem.core.Team.RUSSIA, russiaBleed);
    }
}
