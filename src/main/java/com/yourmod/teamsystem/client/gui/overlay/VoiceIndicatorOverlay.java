package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class VoiceIndicatorOverlay {

    private static final int COLOR_BG     = UITheme.BG_HUD;
    private static final int COLOR_ACTIVE = UITheme.STATUS_OK;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;
    private static final int ENTRY_H      = 14;
    private static final int PANEL_W      = 130;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        List<String> speaking = ClientTeamData.speakingPlayers;
        if (speaking == null || speaking.isEmpty()) return;

        int x = 4;
        int y = screenHeight - 60 - speaking.size() * (ENTRY_H + 2);

        for (String name : speaking) {
            g.fill(x, y, x + PANEL_W, y + ENTRY_H, AnimationHelper.withAlpha(COLOR_BG, 180));

            g.fill(x + 3, y + ENTRY_H / 2 - 3, x + 9, y + ENTRY_H / 2 + 3,
                AnimationHelper.withAlpha(COLOR_ACTIVE, 255));

            g.drawString(Minecraft.getInstance().font, name, x + 13, y + 3,
                AnimationHelper.withAlpha(COLOR_TEXT, 230));

            y += ENTRY_H + 2;
        }
    }
}
