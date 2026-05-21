package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SortControl {

    public enum SortMode {
        DEFAULT("Default"),
        NAME("Name A-Z"),
        AVAILABILITY("Unlocked");

        public final String label;
        SortMode(String label) { this.label = label; }

        public SortMode next() {
            SortMode[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private SortMode currentMode = SortMode.DEFAULT;
    private float hoverAlpha = 0f;

    public SortMode getMode() { return currentMode; }
    public void setMode(SortMode mode) { this.currentMode = mode; }

    public int render(GuiGraphics g, int bx, int by, int mouseX, int mouseY, float fade) {
        var font = Minecraft.getInstance().font;
        String text = "\u2195 " + currentMode.label;
        int tw = font.width(text);
        int pw = tw + 16;
        int ph = font.lineHeight + 4;

        boolean hov = mouseX >= bx && mouseX <= bx + pw && mouseY >= by && mouseY <= by + ph;
        hoverAlpha = AnimationHelper.lerp(hoverAlpha, hov ? 1f : 0f, 0.15f);

        g.fill(bx, by, bx + pw, by + ph,
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * (0x88 + hoverAlpha * 0x77))));
        g.fill(bx, by, bx + pw, by + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * (0x66 + hoverAlpha * 0x99))));
        g.fill(bx, by + ph - 1, bx + pw, by + ph,
            AnimationHelper.withAlpha(hov ? UITheme.ACCENT_DIM : UITheme.BORDER, (int)(fade * 0x88)));

        g.drawString(font, text, bx + 8, by + 2,
            AnimationHelper.withAlpha(hov ? UITheme.ACCENT : UITheme.TEXT_SECONDARY, (int)(fade * 200)));

        return pw;
    }

    public boolean handleClick(double mx, double my, int bx, int by) {
        var font = Minecraft.getInstance().font;
        String text = "\u2195 " + currentMode.label;
        int pw = font.width(text) + 16;
        int ph = font.lineHeight + 4;
        return mx >= bx && mx <= bx + pw && my >= by && my <= by + ph;
    }

    public SortMode cycleMode() {
        currentMode = currentMode.next();
        return currentMode;
    }
}