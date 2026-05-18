package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class CaptureNotificationOverlay {

    private static final int COLOR_BG       = UITheme.BG_HUD;
    private static final int COLOR_PROGRESS = UITheme.ACCENT;
    private static final int COLOR_TEXT     = UITheme.TEXT_PRIMARY;
    private static final int BAR_W          = 200;
    private static final int BAR_H          = 8;

    private String captureLabel    = "";
    private float  captureFraction = 0f;
    private float  smoothFraction  = 0f;
    private boolean active         = false;
    private float  fadeAlpha       = 0f;
    private BProgressBar bar;

    public void startCapture(String pointLabel) {
        captureLabel = pointLabel;
        active       = true;
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

        g.fill(x - 8, y - 6, x + BAR_W + 8, y + BAR_H + 20,
            AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 200)));

        String label = "Capturing: " + captureLabel;
        int tw = Minecraft.getInstance().font.width(label);
        g.drawString(Minecraft.getInstance().font, label, x + BAR_W / 2 - tw / 2, y - 2,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(fadeAlpha * 255)));

        if (bar == null) bar = new BProgressBar(x, y + 10, BAR_W, BAR_H, COLOR_PROGRESS);
        bar.setFraction(smoothFraction);
        bar.render(g);

        String pct = (int)(smoothFraction * 100) + "%";
        int pw = Minecraft.getInstance().font.width(pct);
        g.drawString(Minecraft.getInstance().font, pct, x + BAR_W / 2 - pw / 2, y + 20,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(fadeAlpha * 200)));
    }
}
