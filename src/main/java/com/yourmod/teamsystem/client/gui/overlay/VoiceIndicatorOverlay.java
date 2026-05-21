package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.ClientVoiceHandler;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class VoiceIndicatorOverlay {

    private static final int PANEL_W = 110;
    private static final int ENTRY_H = 14;
    private static final int CHANNEL_H = 16;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Font font = Minecraft.getInstance().font;
        if (font == null) return;

        int ch = ClientVoiceHandler.getActiveChannel();
        String channelLabel;
        int channelColor;
        switch (ch) {
            case 1:
                channelLabel = "\u266B SQUAD";
                channelColor = UITheme.VOICE_SQUAD;
                break;
            case 2:
                channelLabel = "\u266B TEAM";
                channelColor = UITheme.VOICE_TEAM;
                break;
            default:
                channelLabel = "\u266B LOCAL";
                channelColor = UITheme.VOICE_LOCAL;
                break;
        }

        int x = 6;
        int y = screenHeight / 2 + 40;

        RenderHelper.roundedRect(g, x, y, PANEL_W, CHANNEL_H, 2,
            AnimationHelper.withAlpha(UITheme.BG_HUD, 180));
        g.drawString(font, channelLabel, x + 8, y + 4,
            AnimationHelper.withAlpha(channelColor, 240));

        List<String> speaking = ClientVoiceHandler.getActiveSpeakingPlayers();
        if (speaking.isEmpty()) return;

        int sy = y + CHANNEL_H + 2;
        for (String name : speaking) {
            int labelW = font.width(name);
            int pw = Math.max(PANEL_W, labelW + 24);
            RenderHelper.roundedRect(g, x, sy, pw, ENTRY_H, 2,
                AnimationHelper.withAlpha(UITheme.BG_HUD, 160));

            g.fill(x + 4, sy + ENTRY_H / 2 - 3, x + 10, sy + ENTRY_H / 2 + 3,
                AnimationHelper.withAlpha(channelColor, 255));

            g.drawString(font, name, x + 14, sy + 3,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 230));

            sy += ENTRY_H + 1;
        }
    }
}
