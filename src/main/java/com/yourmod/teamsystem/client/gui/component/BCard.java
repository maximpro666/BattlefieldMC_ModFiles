package com.yourmod.teamsystem.client.gui.component;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.yourmod.teamsystem.client.gui.UITheme;

public class BCard {
    private int x, y, width, height;
    private int animOffset = 0;
    private static final int ANIM_OFFSET_START = -20;
    private int borderColor = UITheme.BORDER;
    private int backgroundColor = UITheme.BG_PANEL;
    private int accentColor = UITheme.ACCENT;
    private float animProgress = 0F;
    private boolean isVisible = true;

    public BCard(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setColors(int border, int background, int accent) {
        this.borderColor = border;
        this.backgroundColor = background;
        this.accentColor = accent;
    }

    public void setVisible(boolean visible) {
        if (visible != this.isVisible) {
            this.isVisible = visible;
            if (visible) {
                animOffset = ANIM_OFFSET_START;
                animProgress = 0F;
            }
        }
    }

    public void tick() {
        if (isVisible && animOffset < 0) {
            animProgress += 0.1F;
            if (animProgress > 1.0F) animProgress = 1.0F;
            animOffset = (int)(ANIM_OFFSET_START * (1.0F - AnimationHelper.easeOutCubic(animProgress)));
        }
    }

    public void render(GuiGraphics g) {
        if (!isVisible) return;
        int drawY = y + animOffset;
        g.fill(x, drawY, x + width, drawY + height, backgroundColor);
        g.fill(x, drawY, x + 2, drawY + height, accentColor);
        g.fill(x, drawY, x + width, drawY + 1, borderColor);
        g.fill(x, drawY + height - 1, x + width, drawY + height, borderColor);
        g.fill(x + width - 1, drawY, x + width, drawY + height, borderColor);
    }

    public int getX() { return x; }
    public int getY() { return y + animOffset; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
