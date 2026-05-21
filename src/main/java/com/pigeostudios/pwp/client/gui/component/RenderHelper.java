package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class RenderHelper {

    public static void gradientRectH(GuiGraphics g, int x, int y, int w, int h, int colorLeft, int colorRight) {
        if (w <= 0 || h <= 0) return;
        
        int aL = (colorLeft >> 24) & 0xFF;
        int rL = (colorLeft >> 16) & 0xFF;
        int gL = (colorLeft >> 8) & 0xFF;
        int bL = colorLeft & 0xFF;
        
        int aR = (colorRight >> 24) & 0xFF;
        int rR = (colorRight >> 16) & 0xFF;
        int gR = (colorRight >> 8) & 0xFF;
        int bR = colorRight & 0xFF;
        
        for (int i = 0; i < w; i++) {
            float t = i / (float) w;
            int a = (int) Mth.lerp(t, aL, aR);
            int r = (int) Mth.lerp(t, rL, rR);
            int g_ = (int) Mth.lerp(t, gL, gR);
            int b = (int) Mth.lerp(t, bL, bR);
            int color = (a << 24) | (r << 16) | (g_ << 8) | b;
            g.fill(x + i, y, x + i + 1, y + h, color);
        }
    }

    public static void gradientRectV(GuiGraphics g, int x, int y, int w, int h, int colorTop, int colorBottom) {
        if (w <= 0 || h <= 0) return;
        
        int aT = (colorTop >> 24) & 0xFF;
        int rT = (colorTop >> 16) & 0xFF;
        int gT = (colorTop >> 8) & 0xFF;
        int bT = colorTop & 0xFF;
        
        int aB = (colorBottom >> 24) & 0xFF;
        int rB = (colorBottom >> 16) & 0xFF;
        int gB = (colorBottom >> 8) & 0xFF;
        int bB = colorBottom & 0xFF;
        
        for (int i = 0; i < h; i++) {
            float t = i / (float) h;
            int a = (int) Mth.lerp(t, aT, aB);
            int r = (int) Mth.lerp(t, rT, rB);
            int g_ = (int) Mth.lerp(t, gT, gB);
            int b = (int) Mth.lerp(t, bT, bB);
            int color = (a << 24) | (r << 16) | (g_ << 8) | b;
            g.fill(x, y + i, x + w, y + i + 1, color);
        }
    }

    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w / 2, h / 2));
        
        g.fill(x + radius, y, x + w - radius, y + h, color);
        
        g.fill(x, y + radius, x + radius, y + h - radius, color);
        g.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        
        g.fill(x + radius, y, x + w - radius, y + radius, color);
        g.fill(x + radius, y + h - radius, x + w - radius, y + h, color);
        
        int r2 = radius / 2;
        g.fill(x + r2, y + r2, x + radius, y + radius, color);
        g.fill(x + w - radius, y + r2, x + w - r2, y + radius, color);
        g.fill(x + r2, y + h - radius, x + radius, y + h - r2, color);
        g.fill(x + w - radius, y + h - radius, x + w - r2, y + h - r2, color);
    }

    public static void dropShadow(GuiGraphics g, int x, int y, int w, int h, int offset, int alpha) {
        if (offset <= 0) return;
        int shadowColor = AnimationHelper.withAlpha(0xFF000000, alpha);
        
        for (int i = 0; i < offset; i++) {
            float t = 1f - (i / (float) offset);
            int a = (int) (alpha * t);
            int color = AnimationHelper.withAlpha(0xFF000000, a);
            g.fill(x + i, y + h + i, x + w + i, y + h + i + 1, color);
            g.fill(x + w + i, y + i, x + w + i + 1, y + h + i, color);
        }
    }

    public static void glow(GuiGraphics g, int x, int y, int w, int h, int color, int spread, float intensity) {
        if (spread <= 0 || intensity <= 0.01f) return;
        
        int baseAlpha = (color >> 24) & 0xFF;
        for (int i = 1; i <= spread; i++) {
            float t = 1f - (i / (float) spread);
            int glowAlpha = (int) (baseAlpha * t * intensity);
            if (glowAlpha < 1) continue;
            
            int glowColor = AnimationHelper.withAlpha(color, glowAlpha);
            
            g.fill(x - i, y - i, x + w + i, y - i + 1, glowColor);
            g.fill(x - i, y + h + i - 1, x + w + i, y + h + i, glowColor);
            
            g.fill(x - i, y - i, x - i + 1, y + h + i, glowColor);
            g.fill(x + w + i - 1, y - i, x + w + i, y + h + i, glowColor);
        }
    }

    public static void glowBar(GuiGraphics g, int x, int y, int w, int h, int color, int spread) {
        if (spread <= 0 || w <= 0) return;
        
        int baseAlpha = (color >> 24) & 0xFF;
        for (int i = 1; i <= spread; i++) {
            float t = 1f - (i / (float) spread);
            int glowAlpha = (int) (baseAlpha * t * 0.5f);
            if (glowAlpha < 1) continue;
            
            int glowColor = AnimationHelper.withAlpha(color, glowAlpha);
            g.fill(x + w, y, x + w + i, y + h, glowColor);
        }
    }
}
