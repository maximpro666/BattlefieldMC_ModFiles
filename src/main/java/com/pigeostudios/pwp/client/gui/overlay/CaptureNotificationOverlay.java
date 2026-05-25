package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.CapturePointData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BProgressBar;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class CaptureNotificationOverlay {

    private static final int BAR_W = 260;
    private static final int BAR_H = 10;
    private static final int PANEL_H = 64;
    private static final int TEXT_Y = 24;

    private String pointName = "";
    private String pointType = "small";
    private int capturingTeam = -1;
    private int ownerTeam = -1;
    private float captureFraction = 0f;
    private float smoothFraction = 0f;
    private boolean active = false;
    private float fadeAlpha = 0f;
    private float glowPhase = 0f;
    private BProgressBar bar = new BProgressBar(0, 0, BAR_W, BAR_H, UITheme.TEAM_NATO);

    public void startCapture(String pointName, int capturingTeamOrdinal, int ownerTeamOrdinal, String pointType) {
        this.pointName = pointName != null ? pointName : "";
        this.capturingTeam = capturingTeamOrdinal;
        this.ownerTeam = ownerTeamOrdinal;
        this.pointType = pointType != null ? pointType : "small";
        this.active = true;
        this.glowPhase = 0f;
    }

    public void updateProgress(float fraction) { captureFraction = fraction; }
    public void endCapture() { active = false; }

    public void tickFromCapturePoints(List<CapturePointData> points) {
        if (points == null || points.isEmpty()) {
            endCapture();
            return;
        }

        CapturePointData nearestActive = null;
        for (CapturePointData cp : points) {
            float prog = (float) cp.progress();
            int capturer = cp.capturingTeamOrdinal();
            int owner = cp.ownerTeamOrdinal();
            if (prog > 0.01f && prog < 1.0f) {
                nearestActive = cp;
                break;
            }
        }

        if (nearestActive != null) {
            float prog = (float) nearestActive.progress();
            if (!active || !nearestActive.name().equals(pointName)) {
                startCapture(nearestActive.name(), nearestActive.capturingTeamOrdinal(),
                    nearestActive.ownerTeamOrdinal(), nearestActive.pointType());
            }
            updateProgress(prog);
        } else {
            endCapture();
        }
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        fadeAlpha = AnimationHelper.lerp(fadeAlpha, active ? 1f : 0f, 0.15f);
        smoothFraction = AnimationHelper.lerp(smoothFraction, captureFraction, 0.10f);

        if (active) {
            glowPhase = (glowPhase + 0.08f) % 1f;
        }

        if (fadeAlpha < 0.02f) return;

        int x = screenWidth / 2 - BAR_W / 2;
        int y = screenHeight / 2 + 50;

        int teamColor;
        String teamName;
        int team2Color;
        if (capturingTeam == 0) {
            teamColor = UITheme.TEAM_NATO;
            team2Color = UITheme.TEAM_RUSSIA;
            teamName = "\u041D\u0410\u0422\u041E";
        } else if (capturingTeam == 1) {
            teamColor = UITheme.TEAM_RUSSIA;
            team2Color = UITheme.TEAM_NATO;
            teamName = "\u0420\u0424";
        } else {
            teamColor = UITheme.ACCENT;
            team2Color = UITheme.ACCENT_DIM;
            teamName = "\u0417\u0430\u0445\u0432\u0430\u0442";
        }

        Font font = Minecraft.getInstance().font;
        int barY = y + 8;

        // ===== PASS 1: Frame =====
        RenderHelper.dropShadow(g, x - 10, y - 14, BAR_W + 20, PANEL_H, 4, (int) (fadeAlpha * 120));
        RenderHelper.roundedRect(g, x - 10, y - 14, BAR_W + 20, PANEL_H, 4,
            AnimationHelper.withAlpha(UITheme.BG_HUD, (int) (fadeAlpha * UITheme.ALPHA_HUD)));

        g.fill(x - 10, y - 14, x - 6, y + PANEL_H - 14,
            AnimationHelper.withAlpha(teamColor, (int) (fadeAlpha * 255)));

        g.fill(x - 10, y - 14, x + BAR_W + 10, y - 11,
            AnimationHelper.withAlpha(teamColor, (int) (fadeAlpha * 120)));

        // ===== PASS 2: Progress bar =====
        bar.setGradient(true);
        bar.setShowBorder(false);
        bar.setPosition(x, barY);
        bar.setFillColor(teamColor);
        bar.setFraction(smoothFraction);

        RenderHelper.roundedRect(g, x, barY, BAR_W, BAR_H, 3,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, (int) (fadeAlpha * UITheme.ALPHA_HUD)));

        bar.render(g);

        if (smoothFraction > 0.1f) {
            int fillW = (int) (BAR_W * smoothFraction);
            RenderHelper.glowBar(g, x, barY, fillW, BAR_H, teamColor, 6);
        }

        int remainingW = (int) (BAR_W * (1f - smoothFraction));
        if (remainingW > 0 && ownerTeam >= 0) {
            g.fill(x + (int) (BAR_W * smoothFraction), barY,
                x + (int) (BAR_W * smoothFraction) + remainingW, barY + BAR_H,
                AnimationHelper.withAlpha(team2Color, (int) (fadeAlpha * 60)));
        }

        // ===== PASS 3: Text =====
        String label;
        if ("major".equals(pointType)) {
            label = "\u2694 " + teamName + " \u00BB " + "\u0413\u043B\u0430\u0432\u043D\u0430\u044F \u0442\u043E\u0447\u043A\u0430";
        } else {
            label = "\u2694 " + teamName + " \u00BB " + pointName;
        }
        int tw = font.width(label);

        float glowIntensity = (float) Math.sin(glowPhase * Math.PI * 2) * 0.3f + 0.5f;
        RenderHelper.glow(g, x + BAR_W / 2 - tw / 2 - 4, y - 10, tw + 8, font.lineHeight + 6,
            0xFFFFFFFF, 3, fadeAlpha * glowIntensity * 0.3f);

        g.drawString(font, label, x + BAR_W / 2 - tw / 2, y - 6,
            AnimationHelper.withAlpha(teamColor, (int) (fadeAlpha * 255)));

        String pct = (int) (smoothFraction * 100) + "%";
        String statusIcon = smoothFraction < 1.0f ? "\u25B6" : "\u2713";
        String statusText = statusIcon + " " + pct;
        int sw = font.width(statusText);

        g.drawString(font, statusText, x + BAR_W / 2 - sw / 2, y + TEXT_Y,
            AnimationHelper.withAlpha(teamColor, (int) (fadeAlpha * 230)));

        if (smoothFraction >= 0.98f) {
            float completePulse = (float) Math.sin(glowPhase * Math.PI * 4) * 0.5f + 0.5f;
            RenderHelper.glow(g, x - 10, y - 14, BAR_W + 20, PANEL_H,
                teamColor, 8, fadeAlpha * completePulse * 0.6f);
        }
    }
}
