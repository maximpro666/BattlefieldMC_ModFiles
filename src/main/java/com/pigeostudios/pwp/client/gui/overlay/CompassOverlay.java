package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class CompassOverlay {

    private static final int COMPASS_W = 260;
    private static final int COMPASS_H = 18;
    private static final int VIEW_RANGE = 180;

    public void render(GuiGraphics g, int screenWidth) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int centerX = screenWidth / 2;
        int compassX = centerX - COMPASS_W / 2;
        int compassY = 33;

        Font font = Minecraft.getInstance().font;
        float playerYaw = (player.getYRot() % 360f + 360f) % 360f;

        // Subtle background pill
        int bgColor = AnimationHelper.withAlpha(UITheme.BG_HUD, 120);
        g.fill(compassX, compassY, compassX + COMPASS_W, compassY + COMPASS_H, bgColor);

        // Subtle accent border bottom
        g.fill(compassX, compassY + COMPASS_H - 1, compassX + COMPASS_W, compassY + COMPASS_H,
            AnimationHelper.withAlpha(UITheme.ACCENT, 60));

        drawCursor(g, centerX, compassY, COMPASS_H);
        drawCompassLabels(g, font, compassX, compassY, playerYaw, centerX);
    }

    private void drawCursor(GuiGraphics g, int cx, int cy, int compassH) {
        int col = AnimationHelper.withAlpha(UITheme.ACCENT, 220);
        // Thin vertical line cursor
        g.fill(cx, cy + 2, cx + 1, cy + compassH - 2, col);
        // Small triangle at bottom
        g.fill(cx - 3, cy + compassH - 5, cx + 4, cy + compassH - 4, col);
        g.fill(cx - 2, cy + compassH - 4, cx + 3, cy + compassH - 3, col);
        g.fill(cx - 1, cy + compassH - 3, cx + 2, cy + compassH - 2, col);
    }

    private void drawCompassLabels(GuiGraphics g, Font font, int compassX, int compassY,
                                    float playerYaw, int centerX) {
        int labelY = compassY + 3;

        // Only cardinal + intercardinal directions
        String[][] directions = {
            {"N", "0"}, {"NE", "45"}, {"E", "90"}, {"SE", "135"},
            {"S", "180"}, {"SW", "225"}, {"W", "270"}, {"NW", "315"}
        };

        for (String[] dir : directions) {
            String label = dir[0];
            int deg = Integer.parseInt(dir[1]);

            float relAngle = deg - playerYaw;
            while (relAngle > 180f) relAngle -= 360f;
            while (relAngle < -180f) relAngle += 360f;
            if (Math.abs(relAngle) > VIEW_RANGE / 2f) continue;

            int tickX = centerX + (int) (relAngle / VIEW_RANGE * COMPASS_W);
            if (tickX < compassX || tickX > compassX + COMPASS_W) continue;

            // Tick mark
            boolean isCardinal = label.length() == 1;
            int tickH = isCardinal ? 5 : 3;
            int tickY = compassY + COMPASS_H - 1 - tickH;

            g.fill(tickX, tickY, tickX + 1, tickY + tickH,
                AnimationHelper.withAlpha(isCardinal ? UITheme.TEXT_PRIMARY : UITheme.COMPASS_TICK,
                    isCardinal ? 200 : 140));

            // Label
            int labelColor;
            if ("N".equals(label)) labelColor = UITheme.ACCENT;
            else if ("S".equals(label)) labelColor = UITheme.COMPASS_TEXT;
            else labelColor = UITheme.TEXT_PRIMARY;

            int lw = font.width(label);
            g.drawString(font, label, tickX - lw / 2, labelY,
                AnimationHelper.withAlpha(labelColor, isCardinal ? 220 : 150));
        }
    }
}
