package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BProgressBar {
    protected int x, y, width, height;
    protected float fraction;
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
        this.fraction = Math.max(0F, Math.min(1F, f));
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public void setShowLabel(boolean v) { this.showLabel = v; }

    public void tick() {
    }

    public void render(GuiGraphics g) {
        int drawW = (int)(width * fraction);
        // Background
        g.fill(x, y, x + width, y + height, backgroundColor);
        // Fill
        if (drawW > 0) {
            g.fill(x, y, x + drawW, y + height, fillColor);
        }
        // Border
        if (showBorder) {
            g.fill(x, y, x + width, y + 1, borderColor);
            g.fill(x, y + height - 1, x + width, y + height, borderColor);
            g.fill(x, y, x + 1, y + height, borderColor);
            g.fill(x + width - 1, y, x + width, y + height, borderColor);
        }
        // Label
        if (showLabel) {
            String pct = Math.round(fraction * 100) + "%";
            var font = Minecraft.getInstance().font;
            g.drawString(font, pct, x + width + 4, y + (height - font.lineHeight) / 2, UITheme.TEXT_MUTED);
        }
    }

    public float getFraction() { return fraction; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
