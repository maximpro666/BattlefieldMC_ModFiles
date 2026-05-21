package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class VitalsOverlay {

    private static final int PANEL_W = 190;
    private static final int PANEL_H = 28;
    private static final int HP_BAR_H = 6;
    private static final int ARMOR_BAR_H = 4;

    private float smoothHp = 1f;
    private float smoothArmor = 0f;
    private float displayHp = 100f;
    private float targetDisplayHp = 100f;
    private float regenGlowPhase = 0f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float maxHp = player.getMaxHealth();
        float hp = player.getHealth();
        float hpFraction = maxHp > 0 ? hp / maxHp : 0f;

        smoothHp = AnimationHelper.lerp(smoothHp, hpFraction, 0.12f);
        targetDisplayHp = hp;
        displayHp = AnimationHelper.lerp(displayHp, targetDisplayHp, 0.08f);

        int armorVal = player.getArmorValue();
        float armorFrac = Math.min(1f, armorVal / 20f);
        smoothArmor = AnimationHelper.lerp(smoothArmor, armorFrac, 0.12f);

        int panelX = 6;
        int panelY = screenHeight - 38;

        RenderHelper.dropShadow(g, panelX, panelY, PANEL_W, PANEL_H, 3, 80);
        RenderHelper.roundedRect(g, panelX, panelY, PANEL_W, PANEL_H, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, UITheme.ALPHA_HUD));

        Font font = Minecraft.getInstance().font;

        int hpY = panelY + 3;
        g.fill(panelX + 2, hpY, panelX + 14, hpY + 10,
            AnimationHelper.withAlpha(0xFF000000, 180));
        g.drawString(font, "\u2665", panelX + 4, hpY + 1,
            AnimationHelper.withAlpha(0xFFE05050, 255));

        int displayHpInt = Math.round(displayHp);
        String hpText = displayHpInt + " HP";
        g.drawString(font, hpText, panelX + 16, hpY,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 240));

        int hpBarX = panelX + 72;
        int hpBarY = hpY + 1;
        int hpBarW = PANEL_W - 78;
        g.fill(hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + HP_BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, UITheme.ALPHA_HUD));

        int fillW = (int) (hpBarW * smoothHp);
        if (fillW > 0) {
            int hpColor = getHpGradientColor(smoothHp);
            RenderHelper.gradientRectH(g, hpBarX, hpBarY, fillW, HP_BAR_H,
                hpColor, brightenColor(hpColor, 1.2f));

            if (smoothHp < 0.3f) {
                regenGlowPhase = (regenGlowPhase + 0.1f) % 1f;
                float pulse = (float) Math.sin(regenGlowPhase * Math.PI) * 0.4f;
                RenderHelper.glow(g, hpBarX, hpBarY, fillW, HP_BAR_H,
                    hpColor, 4, pulse);
            }
        }

        int armorY = panelY + 14;
        g.fill(panelX + 2, armorY, panelX + 14, armorY + 10,
            AnimationHelper.withAlpha(0xFF000000, 180));
        g.drawString(font, "\u2666", panelX + 4, armorY + 1,
            AnimationHelper.withAlpha(0xFF5090E0, 255));

        String armorText = armorVal + " ARM";
        g.drawString(font, armorText, panelX + 16, armorY,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, 220));

        int armorBarX = panelX + 72;
        int armorBarY = armorY + 1;
        int armorBarW = PANEL_W - 78;
        g.fill(armorBarX, armorBarY, armorBarX + armorBarW, armorBarY + ARMOR_BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, UITheme.ALPHA_HUD));

        int armorFillW = (int) (armorBarW * smoothArmor);
        if (armorFillW > 0) {
            RenderHelper.gradientRectH(g, armorBarX, armorBarY, armorFillW, ARMOR_BAR_H,
                0xFF506880, 0xFF90A8C0);
        }
    }

    private int getHpGradientColor(float fraction) {
        if (fraction > 0.6f) {
            float t = (fraction - 0.6f) / 0.4f;
            return lerpColor(0xFFCCA030, 0xFF50B050, t);
        } else if (fraction > 0.3f) {
            float t = (fraction - 0.3f) / 0.3f;
            return lerpColor(0xFFCC3030, 0xFFCCA030, t);
        } else {
            return 0xFFCC3030;
        }
    }

    private int lerpColor(int colorA, int colorB, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;
        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;
        int a = (int) ((1f - t) * aA + t * aB);
        int r = (int) ((1f - t) * rA + t * rB);
        int g = (int) ((1f - t) * gA + t * gB);
        int b = (int) ((1f - t) * bA + t * bB);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
