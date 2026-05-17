package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class BPanel {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected final List<BComponent> children = new ArrayList<>();
    protected int backgroundColor = 0x80000000;
    protected boolean drawBorder;
    protected int borderColor = 0xFFFFFFFF;

    public BPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        if (drawBorder) {
            graphics.fill(x, y, x + 1, y + height, borderColor);
            graphics.fill(x + width - 1, y, x + width, y + height, borderColor);
            graphics.fill(x, y, x + width, y + 1, borderColor);
            graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        }
        for (BComponent child : children) {
            child.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public void addChild(BComponent child) {
        children.add(child);
    }

    public void removeChild(BComponent child) {
        children.remove(child);
    }

    public void clearChildren() {
        children.clear();
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }
}
