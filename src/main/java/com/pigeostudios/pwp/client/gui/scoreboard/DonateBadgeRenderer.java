package com.pigeostudios.pwp.client.gui.scoreboard;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.scoreboard.data.PlayerScoreboardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class DonateBadgeRenderer {

    public static void renderBadge(GuiGraphics g, int x, int y, PlayerScoreboardData.DonateLevel level, int alpha) {
        Font font = Minecraft.getInstance().font;
        switch (level) {
            case VIP:
                g.drawString(font, "\u2605 VIP", x, y, AnimationHelper.withAlpha(UITheme.DONATE_VIP, alpha));
                break;
            case ELITE: {
                float t = (System.currentTimeMillis() / 20) % 100 / 100f;
                int color = AnimationHelper.blendColors(UITheme.DONATE_ELITE_A, UITheme.DONATE_ELITE_B, t);
                g.drawString(font, "\u25C8 ELITE", x, y, AnimationHelper.withAlpha(color, alpha));
                break;
            }
            case GENERAL: {
                double pulse = Math.sin(System.currentTimeMillis() / 300.0) * 0.3 + 0.7;
                int glowAlpha = (int) (pulse * alpha);
                g.fill(x - 1, y - 1, x + font.width("\u2B21 GENERAL") + 1, y + font.lineHeight + 1,
                    AnimationHelper.withAlpha(UITheme.DONATE_GENERAL & 0x00FFFFFF, glowAlpha / 4));
                g.drawString(font, "\u2B21 GENERAL", x, y, AnimationHelper.withAlpha(UITheme.DONATE_GENERAL, alpha));
                break;
            }
            default:
                break;
        }
    }

    public static void renderBar(GuiGraphics g, int x, int y, int width, int height,
                                  PlayerScoreboardData.DonateLevel level, float progress, int alpha) {
        if (width <= 0 || height <= 0) return;

        g.fill(x, y, x + width, y + height, AnimationHelper.withAlpha(UITheme.NAMETAG_BAR_BG, alpha));

        int fillColor;
        switch (level) {
            case VIP:
                fillColor = UITheme.DONATE_VIP;
                break;
            case ELITE: {
                float t = (System.currentTimeMillis() / 20) % 100 / 100f;
                fillColor = AnimationHelper.blendColors(UITheme.DONATE_ELITE_A, UITheme.DONATE_ELITE_B, t);
                break;
            }
            case GENERAL: {
                fillColor = UITheme.DONATE_GENERAL;
                break;
            }
            default:
                fillColor = UITheme.NAMETAG_BAR_BASE;
                break;
        }

        int fillW = (int) (width * Math.min(1f, Math.max(0f, progress)));
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + height, AnimationHelper.withAlpha(fillColor, alpha));
        }
    }
}
