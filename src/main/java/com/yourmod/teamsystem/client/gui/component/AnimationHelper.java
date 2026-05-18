package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.util.Mth;

public class AnimationHelper {
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

    public static float smoothStep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float pingPong(float value, float max) {
        float mod = value % (max * 2.0F);
        return mod <= max ? mod : max * 2.0F - mod;
    }
}
