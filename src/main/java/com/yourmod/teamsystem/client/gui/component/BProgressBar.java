package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BProgressBar {
    protected int x, y, width, height;
    protected float targetFraction;
    protected float currentFraction;
    protected boolean pulse;
    protected float pulsePhase;
    protected int fillColor = UITheme.ACCENT;
    protected int backgroundColor = UITheme.BG_SLOT;
    protected int borderColor = UITheme.BORDER;
    protected boolean showBorder = true;
    protected boolean showLabel = false;

    public BProgressBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public BProgressBar(int x, int y, int width, int height, int fillColor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
    }

    public void setFraction(float f) {
        this.targetFraction = Math.max(0F, Math.min(1F, f));
    }

    public void setPulse(boolean pulse) {
        this.pulse = pulse;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public void setFillColor(int color) { this.fillColor = color; }
    public void setShowBorder(boolean v) { this.showBorder = v; }
    public void setShowLabel(boolean v) { this.showLabel = v; }

    public void tick() {
        currentFraction = AnimationHelper.lerp(currentFraction, targetFraction, 0.12f);
        if (Math.abs(currentFraction - targetFraction) < 0.001f) {
            currentFraction = targetFraction;
        }
        if (pulse) {
            pulsePhase = (pulsePhase + 0.025f) % 1f;
        }
    }

    public void render(GuiGraphics g) {
        int drawW = (int)(width * targetFraction);
        int pulseEdge = pulse ? (int)(width * pulsePhase) : -1;

        g.fill(x, y, x + width, y + height, backgroundColor);

        if (drawW > 0) {
            int useW = Math.min(drawW, x + width - this.x);
            g.fill(x, y, x + useW, y + height,
                pulse ? AnimationHelper.withAlpha(fillColor, (int)(0xCC + 0x33 * Math.sin(pulsePhase * Math.PI)))
                      : fillColor);
        }

        if (drawW > 3) {
            g.fill(x + drawW - 3, y, x + drawW, y + height,
                AnimationHelper.withAlpha(fillColor, 0x60));
        }

        if (pulse && pulseEdge >= 1) {
            int gw = Math.min(6, width - pulseEdge);
            g.fill(x + pulseEdge, y, x + pulseEdge + gw, y + height,
                AnimationHelper.withAlpha(0xFFFFFFFF, 0x33));
        }

        if (showBorder) {
            g.fill(x, y, x + width, y + 1, borderColor);
            g.fill(x, y + height - 1, x + width, y + height, borderColor);
            g.fill(x, y, x + 1, y + height, borderColor);
            g.fill(x + width - 1, y, x + width, y + height, borderColor);
        }

        if (showLabel) {
            String pct = Math.round(targetFraction * 100) + "%";
            var font = Minecraft.getInstance().font;
            g.drawString(font, pct, x + width + 4, y + (height - font.lineHeight) / 2, UITheme.TEXT_MUTED);
        }
    }

    public float getFraction() { return targetFraction; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
