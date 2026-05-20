package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BScrollPanel {
    protected int x, y, width, height;
    protected float scrollOffset;
    protected float targetScrollOffset;
    protected int contentHeight;
    protected int borderColor = UITheme.BORDER;
    protected int backgroundColor = UITheme.BG_PANEL;
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
        clampScroll();
    }

    public void ensureVisible(int childY, int childHeight) {
        if (childY < scrollOffset) {
            scrollTo(childY);
        } else if (childY + childHeight > scrollOffset + height) {
            scrollTo(childY + childHeight - height);
        }
    }

    public void scrollTo(float target) {
        float maxScroll = Math.max(0, contentHeight - height);
        targetScrollOffset = Math.max(0, Math.min(maxScroll, target));
    }

    public void scrollToInstantly(float target) {
        float maxScroll = Math.max(0, contentHeight - height);
        scrollOffset = Math.max(0, Math.min(maxScroll, target));
        targetScrollOffset = scrollOffset;
    }

    public void resetScroll() {
        scrollToInstantly(0);
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean onScroll(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        float maxScroll = Math.max(0, contentHeight - height);
        float prev = scrollOffset;
        scrollTo(scrollOffset - (float)(delta * 48));
        return scrollOffset != prev;
    }

    public void tick() {
        scrollOffset = AnimationHelper.lerp(scrollOffset, targetScrollOffset, 0.5f);
        if (Math.abs(scrollOffset - targetScrollOffset) < 0.5f) {
            scrollOffset = targetScrollOffset;
        }
    }

    public void render(GuiGraphics g) {
        g.fill(x, y, x + width, y + height, backgroundColor);

        if (showBorder) {
            g.fill(x, y, x + width, y + 1, borderColor);
            g.fill(x, y + height - 1, x + width, y + height, borderColor);
            g.fill(x, y, x + 1, y + height, borderColor);
            g.fill(x + width - 1, y, x + width, y + height, borderColor);
        }

        renderScrollbar(g);
    }

    protected void renderScrollbar(GuiGraphics g) {
        float maxScroll = Math.max(0, contentHeight - height);
        if (maxScroll <= 0) return;

        int scrollbarH = Math.max(20, height * height / contentHeight);
        int trackW = 4;
        int sx = x + width - trackW - 2;
        float ratio = scrollOffset / maxScroll;
        float availableTrack = height - scrollbarH;
        int sy = y + (int)(ratio * availableTrack);

        g.fill(sx, y, sx + trackW, y + height,
            AnimationHelper.withAlpha(UITheme.BORDER, 0x44));
        g.fill(sx, sy, sx + trackW, sy + scrollbarH,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, 0xAA));
    }

    protected void clampScroll() {
        float maxScroll = Math.max(0, contentHeight - height);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (targetScrollOffset > maxScroll) targetScrollOffset = maxScroll;
    }
}
