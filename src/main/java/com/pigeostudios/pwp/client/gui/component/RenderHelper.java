package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.gui.GuiGraphics;

public class RenderHelper {

    public static void gradientRectH(GuiGraphics g, int x, int y, int w, int h, int colorLeft, int colorRight) {
        if (w <= 0 || h <= 0) return;
        int steps = Math.min(w, 16);
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            int x0 = x + (int)(t0 * w);
            int x1 = x + (int)(t1 * w);
            int c = lerpColor(colorLeft, colorRight, (t0 + t1) * 0.5f);
            g.fill(x0, y, x1, y + h, c);
        }
    }

    public static void gradientRectV(GuiGraphics g, int x, int y, int w, int h, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;
        g.fillGradient(x, y, x + w, y + h, colorTop, colorBottom);
    }

    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w / 2, h / 2));
        g.fill(x + radius, y, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + radius, y + h - radius, color);
        g.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        g.fill(x + radius, y, x + w - radius, y + radius, color);
        g.fill(x + radius, y + h - radius, x + w - radius, y + h, color);
    }

    public static void dropShadow(GuiGraphics g, int x, int y, int w, int h, int offset, int alpha) {
        if (offset <= 0) return;
        int sideAlpha = alpha / 3;
        int cornerAlpha = alpha / 2;
        g.fillGradient(x + 1, y + h, x + w + offset, y + h + offset,
            withAlpha(0, 0), withAlpha(0, sideAlpha));
        g.fillGradient(x + w, y + 1, x + w + offset, y + h + offset,
            withAlpha(0, 0), withAlpha(0, sideAlpha));
        g.fill(x + w, y + h, x + w + offset, y + h + offset,
            withAlpha(0, cornerAlpha));
    }

    public static void glow(GuiGraphics g, int x, int y, int w, int h, int color, int spread, float intensity) {
        if (spread <= 0 || intensity <= 0.01f) return;
        int baseAlpha = Math.min((color >> 24) & 0xFF, 160);
        int a1 = withAlpha(color, (int)(baseAlpha * 0.6f * intensity));
        int a2 = withAlpha(color, (int)(baseAlpha * 0.3f * intensity));
        int a3 = withAlpha(color, (int)(baseAlpha * 0.1f * intensity));

        g.fill(x - spread, y - spread, x + w + spread, y - spread + 1, a3);
        g.fill(x - spread, y + h + spread - 1, x + w + spread, y + h + spread, a3);
        g.fill(x - spread, y - spread, x - spread + 1, y + h + spread, a3);
        g.fill(x + w + spread - 1, y - spread, x + w + spread, y + h + spread, a3);

        int inset = Math.max(1, spread / 2);
        g.fill(x - inset, y - inset, x + w + inset, y - inset + 1, a2);
        g.fill(x - inset, y + h + inset - 1, x + w + inset, y + h + inset, a2);
        g.fill(x - inset, y - inset, x - inset + 1, y + h + inset, a2);
        g.fill(x + w + inset - 1, y - inset, x + w + inset, y + h + inset, a2);

        g.fill(x - 1, y - 1, x + w + 1, y, a1);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, a1);
        g.fill(x - 1, y - 1, x, y + h + 1, a1);
        g.fill(x + w, y - 1, x + w + 1, y + h + 1, a1);
    }

    public static void glowBar(GuiGraphics g, int x, int y, int w, int h, int color, int spread) {
        if (spread <= 0 || w <= 0) return;
        int baseAlpha = Math.min((color >> 24) & 0xFF, 120);
        int a1 = withAlpha(color, (int)(baseAlpha * 0.6f));
        int a2 = withAlpha(color, (int)(baseAlpha * 0.3f));
        int a3 = withAlpha(color, (int)(baseAlpha * 0.1f));
        g.fill(x + w, y, x + w + spread, y + h, a3);
        g.fill(x + w, y, x + w + 2, y + h, a2);
        g.fill(x + w, y, x + w + 1, y + h, a1);
    }

    private static int withAlpha(int color, int alpha) {
        int a = Math.min(0xFF, Math.max(0, alpha));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int)(((c1 >> 24) & 0xFF) * (1 - t) + ((c2 >> 24) & 0xFF) * t);
        int r = (int)(((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int)(((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int)((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
