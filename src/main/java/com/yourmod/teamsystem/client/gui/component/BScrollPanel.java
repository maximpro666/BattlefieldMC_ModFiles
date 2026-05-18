package com.yourmod.teamsystem.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BScrollPanel {
    protected int x, y, width, height;
    protected int scrollOffset;
    protected int contentHeight;
    protected int borderColor = 0xFF2E2E2E;
    protected int backgroundColor = 0x80000000;
    protected boolean showBorder = true;

    public BScrollPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public boolean onScroll(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, contentHeight - height);
        int prev = scrollOffset;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 10));
        return scrollOffset != prev;
    }

    public void tick() {
    }

    public void render(GuiGraphics g) {
        g.fill(x, y, x + width, y + height, backgroundColor);
        if (showBorder) {
            g.fill(x, y, x + width, y + 1, borderColor);
            g.fill(x, y + height - 1, x + width, y + height, borderColor);
            g.fill(x, y, x + 1, y + height, borderColor);
            g.fill(x + width - 1, y, x + width, y + height, borderColor);
        }
    }
}
