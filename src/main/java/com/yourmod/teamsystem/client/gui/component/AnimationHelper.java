package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.ClientTeamData;
import net.minecraft.util.Mth;

public class AnimationHelper {
    private static float cachedOpacity = 1.0f;

    public static void updateOpacityCache() {
        cachedOpacity = ClientTeamData.guiOpacity;
    }
    public static float easeInOutCubic(float t) {
        return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0F) / 2.0F;
    }

    public static float easeOutCubic(float t) {
        return 1.0F - (float)Math.pow(1.0F - t, 3.0F);
    }

    public static float easeOutElastic(float t) {
        if (t == 0.0F || t == 1.0F) return t;
        return (float)(Math.pow(-2.0, -10.0 * t) * Math.sin((t - 0.075) * (2.0 * Math.PI) / 0.3) + 1.0);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float lerpAngle(float a, float b, float t) {
        float diff = b - a;
        while (diff > 180f) diff -= 360f;
        while (diff < -180f) diff += 360f;
        return a + diff * t;
    }

    public static float smoothStep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float pingPong(float value, float max) {
        float mod = value % (max * 2.0F);
        return mod <= max ? mod : max * 2.0F - mod;
    }

    public static int withAlpha(int color, int alpha) {
        int a = Math.min(255, Math.max(0, (int)(alpha * cachedOpacity)));
        return (a & 0xFF) << 24 | (color & 0xFFFFFF);
    }

    public static int blendColors(int base, int overlay, float overlayAlpha) {
        int ba = (base >> 24) & 0xFF;
        int br = (base >> 16) & 0xFF;
        int bg = (base >>  8) & 0xFF;
        int bb =  base        & 0xFF;

        int oa = (overlay >> 24) & 0xFF;
        int or = (overlay >> 16) & 0xFF;
        int og = (overlay >>  8) & 0xFF;
        int ob =  overlay        & 0xFF;

        float a = Mth.clamp(overlayAlpha, 0f, 1f) * (oa / 255f);
        int r = (int)(br * (1f - a) + or * a);
        int g = (int)(bg * (1f - a) + og * a);
        int b_val = (int)(bb * (1f - a) + ob * a);

        return (ba << 24) | (r << 16) | (g << 8) | b_val;
    }

    public static int hpColor(float fraction) {
        if (fraction > 0.6f) return 0xFF50B050;
        if (fraction > 0.3f) return 0xFFCCA030;
        return 0xFFCC3030;
    }
}
