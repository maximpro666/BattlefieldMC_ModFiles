package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;

public class BProgressBar {
    protected int x, y, width, height;
    protected float fraction;
    protected int fillColor = 0xFFE07B00;
    protected int backgroundColor = 0x80000000;
    protected int borderColor = 0xFF2E2E2E;
    protected boolean showBorder = true;

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

    public void tick() {
    }

    public void render(GuiGraphics g) {
        int drawW = (int)(width * fraction);
        g.fill(x, y, x + width, y + height, backgroundColor);
        if (drawW > 0) {
            g.fill(x, y, x + drawW, y + height, fillColor);
        }
        if (showBorder) {
            g.fill(x, y, x + width, y + 1, borderColor);
            g.fill(x, y + height - 1, x + width, y + height, borderColor);
            g.fill(x, y, x + 1, y + height, borderColor);
            g.fill(x + width - 1, y, x + width, y + height, borderColor);
        }
    }

    public float getFraction() { return fraction; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
