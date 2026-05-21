package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
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
    private static final int PANEL_H = 28;

    private final BProgressBar natoBar;
    private final BProgressBar russiaBar;

    private float natoSmooth   = 0f;
    private float russiaSmooth = 0f;
    private float timerGlowPhase = 0f;

    public TicketOverlay(int screenWidth) {
        int cx = screenWidth / 2;
        natoBar   = new BProgressBar(cx - BAR_W - 4, 6, BAR_W, BAR_H, COLOR_NATO_BAR);
        russiaBar = new BProgressBar(cx + 4,          6, BAR_W, BAR_H, COLOR_RUS_BAR);
        
        natoBar.setGradient(true);
        russiaBar.setGradient(true);
        natoBar.setShowBorder(false);
        russiaBar.setShowBorder(false);
    }

    public void render(GuiGraphics g, int screenWidth, float partialTick) {
        int natoTickets   = ClientTeamData.getNatoTickets();
        int russiaTickets = ClientTeamData.getRussiaTickets();
        int timeSeconds   = ClientTeamData.matchTimeSeconds;
        int maxTickets    = ClientTeamData.maxTickets;

        natoSmooth   = AnimationHelper.lerp(natoSmooth,   natoTickets   / (float) maxTickets, 0.06f);
        russiaSmooth = AnimationHelper.lerp(russiaSmooth, russiaTickets / (float) maxTickets, 0.06f);

        int cx = screenWidth / 2;
        int totalW = BAR_W * 2 + 16;
        int panelX = cx - totalW / 2;
        int panelY = 0;
        
        Font font = Minecraft.getInstance().font;

        RenderHelper.dropShadow(g, panelX, panelY, totalW, PANEL_H, 2, 80);
        g.fill(panelX, panelY, panelX + totalW, panelY + PANEL_H, 
            AnimationHelper.withAlpha(COLOR_BG, 200));
        
        g.fill(panelX, panelY + PANEL_H - 1, panelX + totalW, panelY + PANEL_H, 
            AnimationHelper.withAlpha(COLOR_BORDER, 180));

        natoBar.setPosition(cx - BAR_W - 4, 6);
        russiaBar.setPosition(cx + 4, 6);

        String natoText = "NATO  " + natoTickets;
        g.drawString(font, natoText, cx - BAR_W - 4, 16, 
            AnimationHelper.withAlpha(COLOR_TEXT, 220));

        String rusText = russiaTickets + "  RUSSIA";
        int rw = font.width(rusText);
        g.drawString(font, rusText, cx + BAR_W + 4 - rw, 16, 
            AnimationHelper.withAlpha(COLOR_TEXT, 220));

        natoBar.setFraction(natoSmooth);
        natoBar.render(g);

        russiaBar.setFraction(russiaSmooth);
        russiaBar.render(g);

        int mins = timeSeconds / 60;
        int secs = timeSeconds % 60;
        String timerStr;
        if (mins < 10) { 
            timerStr = "0" + mins + ":" + (secs < 10 ? "0" + secs : Integer.toString(secs)); 
        } else { 
            timerStr = Integer.toString(mins) + ":" + (secs < 10 ? "0" + secs : Integer.toString(secs)); 
        }
        
        int tw = font.width(timerStr);
        int timerX = cx - tw / 2;
        int timerY = 20;
        
        if (timeSeconds < 60 && timeSeconds > 0) {
            timerGlowPhase = (timerGlowPhase + 0.05f) % 1f;
            float glowIntensity = (float)Math.sin(timerGlowPhase * Math.PI * 2) * 0.5f + 0.5f;
            
            RenderHelper.glow(g, timerX - 2, timerY - 2, tw + 4, font.lineHeight + 4, 
                COLOR_ORANGE, 3, glowIntensity);
        }
        
        g.drawString(font, timerStr, timerX, timerY, 
            AnimationHelper.withAlpha(COLOR_ORANGE, 255));
    }
}
