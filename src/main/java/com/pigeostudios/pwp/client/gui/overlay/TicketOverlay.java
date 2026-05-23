package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class TicketOverlay {

    private static final int PANEL_W = 310;
    private static final int PANEL_H = 32;
    private static final int BAR_W = 120;
    private static final int BAR_H = 6;
    private static final int BAR_Y = 24;

    private float natoSmooth = 0f;
    private float russiaSmooth = 0f;
    private float timerGlowPhase = 0f;
    private float pulsePhase = 0f;

    public TicketOverlay(int screenWidth) {}

    public void render(GuiGraphics g, int screenWidth, float partialTick) {
        int phase = ClientTeamData.getGamePhase();
        if (phase < 2) {
            renderLobbyStatus(g, screenWidth);
            return;
        }
        int natoTickets = ClientTeamData.getNatoTickets();
        int russiaTickets = ClientTeamData.getRussiaTickets();
        int timeSeconds = ClientTeamData.matchTimeSeconds;
        int maxTickets = ClientTeamData.maxTickets;

        if (maxTickets <= 0) maxTickets = 1;
        natoSmooth = AnimationHelper.lerp(natoSmooth, natoTickets / (float) maxTickets, 0.08f);
        russiaSmooth = AnimationHelper.lerp(russiaSmooth, russiaTickets / (float) maxTickets, 0.08f);

        int cx = screenWidth / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = 0;

        Font font = Minecraft.getInstance().font;

        // Panel background with subtle border
        RenderHelper.dropShadow(g, panelX, panelY, PANEL_W, PANEL_H, 3, 100);
        RenderHelper.roundedRect(g, panelX, panelY, PANEL_W, PANEL_H, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, UITheme.ALPHA_HUD));

        // Accent line at top
        RenderHelper.gradientRectH(g, panelX + 3, panelY, PANEL_W - 6, 2,
            AnimationHelper.withAlpha(UITheme.TEAM_NATO, 180),
            AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, 180));

        int barGap = 12;

        // === NATO (LEFT) ===
        int natoBarX = panelX + barGap;

        // NATO label
        String natoLabel = "NATO";
        int nlw = font.width(natoLabel);
        g.drawString(font, natoLabel, natoBarX, 3,
            AnimationHelper.withAlpha(UITheme.TEAM_NATO, 255));

        // NATO ticket count
        String natoCount = String.valueOf(natoTickets);
        g.drawString(font, natoCount, natoBarX + nlw + 4, 3,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 220));

        // NATO bar background
        g.fill(natoBarX, BAR_Y, natoBarX + BAR_W, BAR_Y + BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, UITheme.ALPHA_HUD));

        // NATO bar fill with glow effect
        int natoFillW = (int) (BAR_W * natoSmooth);
        if (natoFillW > 0) {
            RenderHelper.gradientRectH(g, natoBarX, BAR_Y, natoFillW, BAR_H,
                0xFF0D3A6B, UITheme.TEAM_NATO);
            // Glow tip
            RenderHelper.glowBar(g, natoBarX + natoFillW - 1, BAR_Y, 1, BAR_H,
                UITheme.TEAM_NATO, 4);
        }

        // === RUSSIA (RIGHT) ===
        int russiaBarX = panelX + PANEL_W - barGap - BAR_W;

        // Russia ticket count (right-aligned before label)
        String rusCount = String.valueOf(russiaTickets);
        int rcw = font.width(rusCount);

        String rusLabel = "RUSSIA";
        int rlw = font.width(rusLabel);

        g.drawString(font, rusCount, russiaBarX + BAR_W - rcw - rlw - 4, 3,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 220));

        // Russia label
        g.drawString(font, rusLabel, russiaBarX + BAR_W - rlw, 3,
            AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, 255));

        // Russia bar background
        g.fill(russiaBarX, BAR_Y, russiaBarX + BAR_W, BAR_Y + BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, UITheme.ALPHA_HUD));

        // Russia bar fill
        int russiaFillW = (int) (BAR_W * russiaSmooth);
        if (russiaFillW > 0) {
            RenderHelper.gradientRectH(g, russiaBarX + BAR_W - russiaFillW, BAR_Y, russiaFillW, BAR_H,
                UITheme.TEAM_RUSSIA, 0xFF6B0D0D);
            // Glow tip
            RenderHelper.glowBar(g, russiaBarX + BAR_W - russiaFillW, BAR_Y, 1, BAR_H,
                UITheme.TEAM_RUSSIA, 4);
        }

        // === TIMER ===
        int mins = timeSeconds / 60;
        int secs = timeSeconds % 60;
        String timerStr = String.format("%02d:%02d", mins, secs);
        int tw = font.width(timerStr);
        int timerX = cx - tw / 2;
        int timerY = 3;

        if (timeSeconds < 60 && timeSeconds > 0) {
            timerGlowPhase = (timerGlowPhase + 0.08f) % 1f;
            float glowIntensity = (float) Math.sin(timerGlowPhase * Math.PI * 2) * 0.5f + 0.5f;
            RenderHelper.glow(g, timerX - 4, timerY - 2, tw + 8, font.lineHeight + 4,
                UITheme.ACCENT, 5, glowIntensity * 0.7f);
        }

        g.drawString(font, timerStr, timerX, timerY,
            AnimationHelper.withAlpha(UITheme.ACCENT, 255));

        // VS indicator between bars
        String vs = "VS";
        int vsw = font.width(vs);
        int vsX = cx - vsw / 2;
        g.drawString(font, vs, vsX, BAR_Y - 1,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, 120));
    }

    private void renderLobbyStatus(GuiGraphics g, int screenWidth) {
        int phase = ClientTeamData.getGamePhase();
        String status = ClientTeamData.getLobbyStatus();
        int cx = screenWidth / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = 0;

        Font font = Minecraft.getInstance().font;

        RenderHelper.dropShadow(g, panelX, panelY, PANEL_W, PANEL_H, 3, 100);
        RenderHelper.roundedRect(g, panelX, panelY, PANEL_W, PANEL_H, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, UITheme.ALPHA_HUD));

        if (phase == 1) {
            RenderHelper.gradientRectH(g, panelX + 3, panelY, PANEL_W - 6, 2,
                AnimationHelper.withAlpha(UITheme.ACCENT, 180),
                AnimationHelper.withAlpha(UITheme.ACCENT_DIM, 80));
        } else {
            RenderHelper.gradientRectH(g, panelX + 3, panelY, PANEL_W - 6, 2,
                AnimationHelper.withAlpha(0xCCCCCC, 120),
                AnimationHelper.withAlpha(0x666666, 120));
        }

        pulsePhase = (pulsePhase + 0.03f) % 1f;

        String line1;
        String line2 = "";
        int color1 = UITheme.TEXT_PRIMARY;
        int color2 = UITheme.TEXT_SECONDARY;
        boolean showPulse = false;

        if (phase == 1) {
            line1 = "\u0413\u041e\u041b\u041e\u0421\u041e\u0412\u0410\u041d\u0418\u0415";
            int remaining = ClientTeamData.getVoteRemainingSeconds();
            line2 = "\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: " + remaining + "\u0441";
            color1 = UITheme.ACCENT;
            color2 = UITheme.TEXT_SECONDARY;
        } else if (status == null || status.isEmpty()) {
            line1 = "\u041b\u041e\u0411\u0411\u0418";
            color1 = UITheme.TEXT_MUTED;
        } else {
            switch (status) {
                case "cleaning":
                    line1 = "\u041e\u0447\u0438\u0441\u0442\u043a\u0430 \u0441\u0442\u0430\u0440\u043e\u0433\u043e \u043c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440\u0430...";
                    color1 = UITheme.STATUS_WARN;
                    showPulse = true;
                    break;
                case "waiting":
                    line1 = "\u0417\u0430\u043f\u0443\u0441\u043a \u043c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440\u0430...";
                    color1 = UITheme.STATUS_WARN;
                    showPulse = true;
                    break;
                case "ready":
                    line1 = "\u041c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440 \u0433\u043e\u0442\u043e\u0432!";
                    line2 = "\u041f\u0435\u0440\u0435\u043c\u0435\u0449\u0435\u043d\u0438\u0435 \u0438\u0433\u0440\u043e\u043a\u043e\u0432...";
                    color1 = UITheme.STATUS_OK;
                    color2 = UITheme.STATUS_OK;
                    break;
                case "failed":
                    line1 = "\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u043f\u0443\u0441\u043a\u0430 \u043c\u0430\u0442\u0447-\u0441\u0435\u0440\u0432\u0435\u0440\u0430";
                    color1 = UITheme.STATUS_DANGER;
                    break;
                default:
                    line1 = status;
                    break;
            }
        }

        if (showPulse) {
            float pulse = (float) Math.sin(pulsePhase * Math.PI * 2) * 0.3f + 0.7f;
            RenderHelper.glow(g, panelX + 4, panelY + PANEL_H / 2 - 4, PANEL_W - 8, 8,
                color1, 6, pulse * 0.3f);
        }

        if (line2.isEmpty()) {
            int tw = font.width(line1);
            g.drawString(font, line1, cx - tw / 2, PANEL_H / 2 - font.lineHeight / 2,
                AnimationHelper.withAlpha(color1, 255));
        } else {
            int tw1 = font.width(line1);
            g.drawString(font, line1, cx - tw1 / 2, 2,
                AnimationHelper.withAlpha(color1, 255));
            int tw2 = font.width(line2);
            g.drawString(font, line2, cx - tw2 / 2, 2 + font.lineHeight + 1,
                AnimationHelper.withAlpha(color2, 220));
        }
    }
}
