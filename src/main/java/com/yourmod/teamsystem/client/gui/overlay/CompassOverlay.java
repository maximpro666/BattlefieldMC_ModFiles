package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class CompassOverlay {

    private static final int COLOR_BG       = UITheme.BG_HUD;
    private static final int COLOR_BORDER   = UITheme.BORDER;
    private static final int COLOR_TEXT     = UITheme.TEXT_SECONDARY;
    private static final int COLOR_CARDINAL = UITheme.TEXT_PRIMARY;
    private static final int COLOR_ORANGE   = UITheme.ACCENT;
    private static final int COLOR_TICK     = UITheme.COMPASS_TICK;

    private static final int WIDTH  = 240;
    private static final int HEIGHT = 18;

    public void render(GuiGraphics g, int screenWidth, float yaw) {
        int x = screenWidth / 2 - WIDTH / 2;
        int y = 28;

        g.fill(x, y, x + WIDTH, y + HEIGHT, COLOR_BG);
        g.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, COLOR_BORDER);

        g.fill(x + WIDTH / 2 - 1, y, x + WIDTH / 2 + 1, y + HEIGHT, AnimationHelper.withAlpha(COLOR_ORANGE, 200));

        float norm = ((yaw % 360) + 360) % 360;
        for (int deg = -180; deg <= 180; deg += 5) {
            float drawDeg = (norm + deg + 360) % 360;
            int px = x + WIDTH / 2 + (int)(deg * (WIDTH / 360f));
            if (px < x || px > x + WIDTH) continue;

            boolean isCardinal = (drawDeg % 90 == 0);
            boolean isMajor    = (drawDeg % 45 == 0);

            int tickH = isCardinal ? HEIGHT - 4 : (isMajor ? HEIGHT - 8 : HEIGHT - 12);
            int col   = isCardinal ? COLOR_CARDINAL : COLOR_TICK;
            g.fill(px, y + HEIGHT - tickH, px + 1, y + HEIGHT, col);

            if (isCardinal) {
                String label = degreesToCardinal((int) drawDeg);
                int lw = Minecraft.getInstance().font.width(label);
                g.drawString(Minecraft.getInstance().font, label, px - lw / 2, y + 2,
                    AnimationHelper.withAlpha(COLOR_CARDINAL, 255));
            }
        }

        String degLabel = (int) norm + "\u00B0";
        int dw = Minecraft.getInstance().font.width(degLabel);
        g.drawString(Minecraft.getInstance().font, degLabel,
            x + WIDTH / 2 - dw / 2, y + HEIGHT + 2,
            AnimationHelper.withAlpha(COLOR_TEXT, 200));
    }

    private String degreesToCardinal(int deg) {
        return switch (deg) {
            case 0   -> "N";
            case 90  -> "E";
            case 180 -> "S";
            case 270 -> "W";
            default  -> "";
        };
    }
}
