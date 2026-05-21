package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class TicketOverlay {

    private static final int BAR_H = 7;
    private static final int PANEL_H = 28;

    private float natoSmooth = 0f;
    private float russiaSmooth = 0f;
    private float timerGlowPhase = 0f;

    public TicketOverlay(int screenWidth) {}

    public void render(GuiGraphics g, int screenWidth, float partialTick) {
        int natoTickets = ClientTeamData.getNatoTickets();
        int russiaTickets = ClientTeamData.getRussiaTickets();
        int timeSeconds = ClientTeamData.matchTimeSeconds;
        int maxTickets = ClientTeamData.maxTickets;

        if (maxTickets <= 0) maxTickets = 1;
        natoSmooth = AnimationHelper.lerp(natoSmooth, natoTickets / (float) maxTickets, 0.08f);
        russiaSmooth = AnimationHelper.lerp(russiaSmooth, russiaTickets / (float) maxTickets, 0.08f);

        int cx = screenWidth / 2;
        int totalW = 290;
        int barW = 120;
        int panelX = cx - totalW / 2;
        int panelY = 0;

        Font font = Minecraft.getInstance().font;

        RenderHelper.dropShadow(g, panelX, panelY, totalW, PANEL_H, 3, 100);
        RenderHelper.roundedRect(g, panelX, panelY, totalW, PANEL_H, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, UITheme.ALPHA_HUD));

        int natoBarX = panelX + 8;
        int barY = 18;
        g.fill(natoBarX, barY, natoBarX + barW, barY + BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, UITheme.ALPHA_HUD));
        int natoFillW = (int) (barW * natoSmooth);
        if (natoFillW > 0) {
            RenderHelper.gradientRectH(g, natoBarX, barY, natoFillW, BAR_H,
                0xFF0D3A6B, UITheme.TEAM_NATO);
        }

        int russiaBarX = panelX + totalW - 8 - barW;
        g.fill(russiaBarX, barY, russiaBarX + barW, barY + BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, UITheme.ALPHA_HUD));
        int russiaFillW = (int) (barW * russiaSmooth);
        if (russiaFillW > 0) {
            RenderHelper.gradientRectH(g, russiaBarX, barY, russiaFillW, BAR_H,
                UITheme.TEAM_RUSSIA, 0xFF6B0D0D);
        }

        String natoLabel = "NATO";
        g.drawString(font, natoLabel, natoBarX, 3,
            AnimationHelper.withAlpha(UITheme.TEAM_NATO, 255));
        String natoCount = String.valueOf(natoTickets);
        g.drawString(font, natoCount, natoBarX, 11,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 220));

        String rusLabel = "RUSSIA";
        int rlw = font.width(rusLabel);
        g.drawString(font, rusLabel, russiaBarX + barW - rlw, 3,
            AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, 255));
        String rusCount = String.valueOf(russiaTickets);
        int rcw = font.width(rusCount);
        g.drawString(font, rusCount, russiaBarX + barW - rcw, 11,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 220));

        int mins = timeSeconds / 60;
        int secs = timeSeconds % 60;
        String timerStr = String.format("%02d:%02d", mins, secs);
        int tw = font.width(timerStr);
        int timerX = cx - tw / 2;
        int timerY = 4;

        if (timeSeconds < 60 && timeSeconds > 0) {
            timerGlowPhase = (timerGlowPhase + 0.08f) % 1f;
            float glowIntensity = (float) Math.sin(timerGlowPhase * Math.PI * 2) * 0.5f + 0.5f;
            RenderHelper.glow(g, timerX - 4, timerY - 2, tw + 8, font.lineHeight + 4,
                UITheme.ACCENT, 5, glowIntensity * 0.7f);
        }

        g.drawString(font, timerStr, timerX, timerY,
            AnimationHelper.withAlpha(UITheme.ACCENT, 255));
    }
}
