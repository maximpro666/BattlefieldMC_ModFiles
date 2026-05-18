package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.core.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;
import java.util.UUID;

public class SquadOverlay {

    private static final int COLOR_BG     = UITheme.BG_HUD;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;
    private static final int ROW_H        = 20;
    private static final int PANEL_W      = 140;

    public void render(GuiGraphics g, int screenHeight) {
        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        String mySquad = ClientTeamData.localPlayerSquad;
        if (map == null || mySquad == null || mySquad.isEmpty()) return;

        int startY = screenHeight / 2 - 60;
        int x = 4;
        int idx = 0;

        Font font = Minecraft.getInstance().font;

        for (Map.Entry<UUID, PlayerListEntry> entry : map.entrySet()) {
            PlayerListEntry ple = entry.getValue();
            if (!mySquad.equals(ple.squad())) continue;
            int y = startY + idx * ROW_H;

            g.fill(x, y, x + PANEL_W, y + ROW_H - 1, AnimationHelper.withAlpha(COLOR_BG, 180));

            Rank rank = Rank.fromOrdinal(ple.rank());
            String callsign = ple.callsign();
            if (callsign == null) callsign = "Unknown";
            String label = (rank != null ? rank.getPrefix(false) : "") + " " + callsign;
            g.drawString(font, label, x + 4, y + 6, AnimationHelper.withAlpha(COLOR_TEXT, 230));

            idx++;
            if (idx >= 6) break;
        }
    }
}

