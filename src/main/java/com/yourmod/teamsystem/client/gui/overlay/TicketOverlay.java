package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
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
    private static final int MAX_TICKETS = 500;

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

        natoSmooth   = AnimationHelper.lerp(natoSmooth,   natoTickets   / (float) MAX_TICKETS, 0.06f);
        russiaSmooth = AnimationHelper.lerp(russiaSmooth, russiaTickets / (float) MAX_TICKETS, 0.06f);

        int cx = screenWidth / 2;

        g.fill(cx - BAR_W - 8, 0, cx + BAR_W + 8, 26, COLOR_BG);
        g.fill(cx - BAR_W - 8, 25, cx + BAR_W + 8, 26, COLOR_BORDER);

        String natoText = "NATO  " + natoTickets;
        g.drawString(null, natoText, cx - BAR_W - 4, 14, AnimationHelper.withAlpha(COLOR_TEXT, 220));

        String rusText = russiaTickets + "  RUSSIA";
        int rw = Minecraft.getInstance().font.width(rusText);
        g.drawString(null, rusText, cx + BAR_W + 4 - rw, 14, AnimationHelper.withAlpha(COLOR_TEXT, 220));

        natoBar.setFraction(natoSmooth);
        natoBar.render(g);

        russiaBar.setFraction(russiaSmooth);
        russiaBar.render(g);

        int mins = timeSeconds / 60;
        int secs = timeSeconds % 60;
        String timerStr = String.format("%02d:%02d", mins, secs);
        int tw = Minecraft.getInstance().font.width(timerStr);
        g.drawString(Minecraft.getInstance().font, timerStr, cx - tw / 2, 8, AnimationHelper.withAlpha(COLOR_ORANGE, 255));
    }
}
