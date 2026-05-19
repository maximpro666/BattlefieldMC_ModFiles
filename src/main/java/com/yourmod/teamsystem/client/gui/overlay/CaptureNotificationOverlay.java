package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class CaptureNotificationOverlay {

    private static final int COLOR_BG       = UITheme.BG_HUD;
    private static final int COLOR_PROGRESS = UITheme.ACCENT;
    private static final int COLOR_TEXT     = UITheme.TEXT_PRIMARY;
    private static final int BAR_W          = 220;
    private static final int BAR_H          = 8;

    private String pointName      = "";
    private int capturingTeam     = -1;
    private int ownerTeam         = -1;
    private float captureFraction = 0f;
    private float smoothFraction  = 0f;
    private boolean active       = false;
    private float  fadeAlpha     = 0f;
    private BProgressBar bar;

    public void startCapture(String pointName, int capturingTeamOrdinal, int ownerTeamOrdinal) {
        this.pointName = pointName;
        this.capturingTeam = capturingTeamOrdinal;
        this.ownerTeam = ownerTeamOrdinal;
        this.active = true;
    }

    public void updateProgress(float fraction) {
        captureFraction = fraction;
    }

    public void endCapture() {
        active = false;
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        fadeAlpha   = AnimationHelper.lerp(fadeAlpha, active ? 1f : 0f, 0.12f);
        smoothFraction = AnimationHelper.lerp(smoothFraction, captureFraction, 0.08f);
        if (fadeAlpha < 0.02f) return;

        int x = screenWidth / 2 - BAR_W / 2;
        int y = screenHeight / 2 + 40;

        // Background panel
        g.fill(x - 8, y - 8, x + BAR_W + 8, y + BAR_H + 24,
            AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 200)));

        Font font = Minecraft.getInstance().font;

        // Team color accent bar
        int teamColor;
        String teamName;
        if (capturingTeam == 0) {
            teamColor = UITheme.TEAM_NATO;
            teamName = "\u041D\u0410\u0422\u041E";
        } else if (capturingTeam == 1) {
            teamColor = UITheme.TEAM_RUSSIA;
            teamName = "\u0420\u041E\u0421\u0421\u0418\u042F";
        } else {
            teamColor = UITheme.ACCENT;
            teamName = "\u0417\u0430\u0445\u0432\u0430\u0442";
        }
        g.fill(x - 8, y - 8, x - 4, y + BAR_H + 24,
            AnimationHelper.withAlpha(teamColor, (int)(fadeAlpha * 255)));

        // Main label: "Захват: Название"
        String label = "\u0417\u0430\u0445\u0432\u0430\u0442: " + teamName + " \u00BB " + pointName;
        int tw = font.width(label);
        g.drawString(font, label, x + BAR_W / 2 - tw / 2, y - 2,
            AnimationHelper.withAlpha(teamColor, (int)(fadeAlpha * 255)));

        // Progress bar
        if (bar == null) bar = new BProgressBar(x, y + 12, BAR_W, BAR_H, teamColor);
        bar.setFraction(smoothFraction);
        bar.render(g);

        // Percentage text with icon
        String pct = (int)(smoothFraction * 100) + "%";
        String statusIcon = smoothFraction < 1.0f ? "\u25B6" : "\u2713";
        String statusText = statusIcon + " " + pct;
        int sw = font.width(statusText);
        g.drawString(font, statusText, x + BAR_W / 2 - sw / 2, y + 22,
            AnimationHelper.withAlpha(teamColor, (int)(fadeAlpha * 200)));
    }
}
