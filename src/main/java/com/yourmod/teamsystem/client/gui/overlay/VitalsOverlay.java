package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class VitalsOverlay {

    private static final int PANEL_W = 180;
    private static final int PANEL_H = 22;
    private static final int HP_BAR_H = 5;
    private static final int ARMOR_BAR_H = 4;

    private float smoothHp = 1f;
    private float smoothArmor = 0f;
    private float displayHp = 100f;
    private float targetDisplayHp = 100f;
    private float regenGlowPhase = 0f;
    private float lastHpFraction = 1f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float maxHp = player.getMaxHealth();
        float hp = player.getHealth();
        float hpFraction = maxHp > 0 ? hp / maxHp : 0f;

        smoothHp = AnimationHelper.lerp(smoothHp, hpFraction, 0.12f);
        targetDisplayHp = hp;
        displayHp = AnimationHelper.lerp(displayHp, targetDisplayHp, 0.08f);
        int hpColor = getHpGradientColor(smoothHp);

        int armorVal = player.getArmorValue();
        float armorFrac = Math.min(1f, armorVal / 20f);
        smoothArmor = AnimationHelper.lerp(smoothArmor, armorFrac, 0.12f);

        int panelX = 6;
        int panelY = screenHeight - 32;

        RenderHelper.dropShadow(g, panelX, panelY, PANEL_W, PANEL_H, 2, 60);
        RenderHelper.roundedRect(g, panelX, panelY, PANEL_W, PANEL_H, 2,
            AnimationHelper.withAlpha(UITheme.BG_HUD, 255));

        Font font = Minecraft.getInstance().font;

        int hpY = panelY + 3;
        g.fill(panelX + 2, hpY - 1, panelX + 12, hpY + 9, AnimationHelper.withAlpha(0xFF000000, 180));
        g.drawString(font, "\u2665", panelX + 4, hpY, AnimationHelper.withAlpha(0xFFE05050, 255));

        int displayHpInt = Math.round(displayHp);
        int maxHpInt = Math.round(maxHp);
        String hpText = displayHpInt + "/" + maxHpInt;
        g.drawString(font, hpText, panelX + 14, hpY, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, 240));

        int hpBarX = panelX + 70;
        int hpBarY = hpY + 1;
        int hpBarW = PANEL_W - 76;
        g.fill(hpBarX, hpBarY, hpBarX + hpBarW, hpBarY + HP_BAR_H,
            AnimationHelper.withAlpha(UITheme.HUD_HP_BG, 255));

        int fillW = (int)(hpBarW * smoothHp);
        if (fillW > 0) {
            RenderHelper.gradientRectH(g, hpBarX, hpBarY, fillW, HP_BAR_H,
                hpColor, brightenColor(hpColor, 1.15f));
        }

        if (smoothHp < hpFraction && smoothHp > 0.85f) {
            regenGlowPhase = (regenGlowPhase + 0.08f) % 1f;
            float glowIntensity = (float)Math.sin(regenGlowPhase * Math.PI) * 0.5f;
            RenderHelper.glow(g, hpBarX, hpBarY, fillW, HP_BAR_H, hpColor, 3, glowIntensity);
        }

        lastHpFraction = hpFraction;

        int armorY = panelY + 12;
        g.fill(panelX + 2, armorY - 1, panelX + 12, armorY + 9, AnimationHelper.withAlpha(0xFF000000, 180));
        g.drawString(font, "\u2666", panelX + 4, armorY, AnimationHelper.withAlpha(0xFF5090E0, 255));

        String armorText = armorVal + "/20";
        g.drawString(font, armorText, panelX + 14, armorY, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, 220));

        int armorBarX = panelX + 70;
        int armorBarY = armorY + 1;
        int armorBarW = PANEL_W - 76;
        g.fill(armorBarX, armorBarY, armorBarX + armorBarW, armorBarY + ARMOR_BAR_H,
            AnimationHelper.withAlpha(0xFF1A1A1A, 255));

        int armorFillW = (int)(armorBarW * smoothArmor);
        if (armorFillW > 0) {
            int armorGradStart = 0xFF607080;
            int armorGradMid = 0xFF8090A0;
            int armorGradEnd = 0xFFB0C0D0;

            if (armorFillW > 2) {
                int halfW = armorFillW / 2;
                RenderHelper.gradientRectH(g, armorBarX, armorBarY, halfW, ARMOR_BAR_H,
                    armorGradStart, armorGradMid);
                RenderHelper.gradientRectH(g, armorBarX + halfW, armorBarY, armorFillW - halfW, ARMOR_BAR_H,
                    armorGradMid, armorGradEnd);
            } else {
                g.fill(armorBarX, armorBarY, armorBarX + armorFillW, armorBarY + ARMOR_BAR_H, armorGradMid);
            }
        }

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, 80));
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
        int a = (int)((1f - t) * aA + t * aB);
        int r = (int)((1f - t) * rA + t * rB);
        int g = (int)((1f - t) * gA + t * gB);
        int b = (int)((1f - t) * bA + t * bB);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int)((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
