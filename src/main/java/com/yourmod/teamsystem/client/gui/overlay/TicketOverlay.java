package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class TicketOverlay {

    private static final int COLOR_NATO_BAR  = UITheme.TEAM_NATO;
    private static final int COLOR_RUS_BAR   = UITheme.TEAM_RUSSIA;
    private static final int COLOR_BG        = UITheme.BG_HUD;
    private static final int COLOR_TEXT      = UITheme.TEXT_PRIMARY;
    private static final int COLOR_ORANGE    = UITheme.ACCENT;
    private static final int COLOR_BORDER    = UITheme.BORDER;

    private static final int BAR_W = 140;
    private static final int BAR_H = 6;

    private final BProgressBar natoBar;
    private final BProgressBar russiaBar;

    private float natoSmooth   = 0f;
    private float russiaSmooth = 0f;

    public TicketOverlay(int screenWidth) {
        int cx = screenWidth / 2;
        natoBar   = new BProgressBar(cx - BAR_W - 4, 4, BAR_W, BAR_H, COLOR_NATO_BAR);
        russiaBar = new BProgressBar(cx + 4,          4, BAR_W, BAR_H, COLOR_RUS_BAR);
    }

    public void render(GuiGraphics g, int screenWidth, float partialTick) {
        int natoTickets   = ClientTeamData.getNatoTickets();
        int russiaTickets = ClientTeamData.getRussiaTickets();
        int timeSeconds   = ClientTeamData.matchTimeSeconds;
        int maxTickets    = ClientTeamData.maxTickets;

        natoSmooth   = AnimationHelper.lerp(natoSmooth,   natoTickets   / (float) maxTickets, 0.06f);
        russiaSmooth = AnimationHelper.lerp(russiaSmooth, russiaTickets / (float) maxTickets, 0.06f);

        int cx = screenWidth / 2;
        Font font = Minecraft.getInstance().font;

        natoBar.setPosition(cx - BAR_W - 4, 6);
        russiaBar.setPosition(cx + 4, 6);

        g.fill(cx - BAR_W - 8, 0, cx + BAR_W + 8, 28, COLOR_BG);
        g.fill(cx - BAR_W - 8, 27, cx + BAR_W + 8, 28, COLOR_BORDER);

        String natoText = "NATO  " + natoTickets;
        g.drawString(font, natoText, cx - BAR_W - 4, 16, AnimationHelper.withAlpha(COLOR_TEXT, 220));

        String rusText = russiaTickets + "  RUSSIA";
        int rw = font.width(rusText);
        g.drawString(font, rusText, cx + BAR_W + 4 - rw, 16, AnimationHelper.withAlpha(COLOR_TEXT, 220));

        natoBar.setFraction(natoSmooth);
        natoBar.render(g);

        russiaBar.setFraction(russiaSmooth);
        russiaBar.render(g);

        int mins = timeSeconds / 60;
        int secs = timeSeconds % 60;
        String timerStr;
        if (mins < 10) { timerStr = "0" + mins + ":" + (secs < 10 ? "0" + secs : Integer.toString(secs)); }
        else { timerStr = Integer.toString(mins) + ":" + (secs < 10 ? "0" + secs : Integer.toString(secs)); }
        int tw = font.width(timerStr);
        g.drawString(font, timerStr, cx - tw / 2, 20, AnimationHelper.withAlpha(COLOR_ORANGE, 255));
    }

}
