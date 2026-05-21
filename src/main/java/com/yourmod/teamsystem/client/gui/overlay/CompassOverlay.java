package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class CompassOverlay {

    private static final int COMPASS_W = 320;
    private static final int COMPASS_H = 10;
    private static final int TICK_INTERVAL = 30;
    private static final int VIEW_RANGE = 150;

    public void render(GuiGraphics g, int screenWidth) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int centerX = screenWidth / 2;
        int compassX = centerX - COMPASS_W / 2;
        int compassY = 29;

        g.fill(compassX, compassY, compassX + COMPASS_W, compassY + COMPASS_H,
            AnimationHelper.withAlpha(UITheme.BG_HUD, 180));

        g.fill(compassX, compassY + COMPASS_H - 1, compassX + COMPASS_W, compassY + COMPASS_H,
            AnimationHelper.withAlpha(UITheme.BORDER, 200));

        Font font = Minecraft.getInstance().font;
        float playerYaw = (player.getYRot() % 360f + 360f) % 360f;

        drawCursor(g, centerX, compassY);
        drawCompassTicks(g, font, compassX, compassY, playerYaw, centerX);
    }

    private void drawCursor(GuiGraphics g, int cx, int cy) {
        int col = AnimationHelper.withAlpha(UITheme.COMPASS_CURSOR, 255);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 4, col);
        g.fill(cx - 3, cy + 2, cx - 1, cy + 3, col);
        g.fill(cx + 2, cy + 2, cx + 4, cy + 3, col);
    }

    private void drawCompassTicks(GuiGraphics g, Font font, int compassX, int compassY,
                                   float playerYaw, int centerX) {
        int tickY = compassY + 2;

        for (int deg = 0; deg < 360; deg += TICK_INTERVAL) {
            float relAngle = deg - playerYaw;
            while (relAngle > 180f) relAngle -= 360f;
            while (relAngle < -180f) relAngle += 360f;
            if (Math.abs(relAngle) > VIEW_RANGE) continue;

            int tickX = centerX + (int)(relAngle / 180f * (COMPASS_W / 2));
            if (tickX < compassX || tickX > compassX + COMPASS_W) continue;

            boolean isCardinal = (deg % 90) == 0;
            int tickH = isCardinal ? 3 : 2;
            int tickColor = isCardinal ? UITheme.COMPASS_CARDINAL : UITheme.COMPASS_TICK;

            g.fill(tickX, tickY, tickX + 1, tickY + tickH,
                AnimationHelper.withAlpha(tickColor, isCardinal ? 255 : 200));

            String label;
            if (deg == 0) label = "N";
            else if (deg == 90) label = "E";
            else if (deg == 180) label = "S";
            else if (deg == 270) label = "W";
            else label = String.valueOf(deg);

            int lw = font.width(label);
            int labelColor = isCardinal ? UITheme.COMPASS_CARDINAL : UITheme.COMPASS_TEXT;
            g.drawString(font, label, tickX - lw / 2, tickY + tickH + 1,
                AnimationHelper.withAlpha(labelColor, isCardinal ? 240 : 180));
        }
    }

}
