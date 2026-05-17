package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;

public class BProgressBar {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected float fraction;
    protected int color = 0xFF00FF00;
    protected int backgroundColor = 0x80000000;

    public BProgressBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        int fillWidth = (int) (width * fraction);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, color);
        }
    }

    public void setFraction(float fraction) {
        this.fraction = Math.max(0.0F, Math.min(1.0F, fraction));
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float getFraction() {
        return fraction;
    }
}
