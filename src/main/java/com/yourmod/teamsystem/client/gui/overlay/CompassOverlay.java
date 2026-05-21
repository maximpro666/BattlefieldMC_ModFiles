package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class CompassOverlay {

    private static final int COMPASS_W = 340;
    private static final int COMPASS_H = 16;
    private static final int TICK_INTERVAL = 30;
    private static final int VIEW_RANGE = 170;

    public void render(GuiGraphics g, int screenWidth) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int centerX = screenWidth / 2;
        int compassX = centerX - COMPASS_W / 2;
        int compassY = 29;

        RenderHelper.roundedRect(g, compassX, compassY, COMPASS_W, COMPASS_H, 2,
            AnimationHelper.withAlpha(UITheme.BG_HUD, 200));

        RenderHelper.gradientRectH(g, compassX, compassY + COMPASS_H - 2, COMPASS_W, 2,
            AnimationHelper.withAlpha(UITheme.ACCENT, 60),
            AnimationHelper.withAlpha(UITheme.ACCENT, 20));

        Font font = Minecraft.getInstance().font;
        float playerYaw = (player.getYRot() % 360f + 360f) % 360f;

        drawCursor(g, centerX, compassY);
        drawCompassTicks(g, font, compassX, compassY, playerYaw, centerX);
    }

    private void drawCursor(GuiGraphics g, int cx, int cy) {
        int col = AnimationHelper.withAlpha(UITheme.ACCENT, 255);
        g.fill(cx - 2, cy + 1, cx + 3, cy + 6, col);
        g.fill(cx - 5, cy + 3, cx - 2, cy + 5, col);
        g.fill(cx + 3, cy + 3, cx + 6, cy + 5, col);
        g.fill(cx - 1, cy, cx + 2, cy + 2, col);
    }

    private void drawCompassTicks(GuiGraphics g, Font font, int compassX, int compassY,
                                   float playerYaw, int centerX) {
        int tickY = compassY + 4;

        for (int deg = 0; deg < 360; deg += TICK_INTERVAL) {
            float relAngle = deg - playerYaw;
            while (relAngle > 180f) relAngle -= 360f;
            while (relAngle < -180f) relAngle += 360f;
            if (Math.abs(relAngle) > VIEW_RANGE) continue;

            int tickX = centerX + (int) (relAngle / 180f * (COMPASS_W / 2));
            if (tickX < compassX || tickX > compassX + COMPASS_W) continue;

            boolean isCardinal = (deg % 90) == 0;
            int tickH = isCardinal ? 5 : 2;

            int tickColor = isCardinal ? UITheme.TEXT_PRIMARY : UITheme.COMPASS_TICK;
            g.fill(tickX, tickY, tickX + 1, tickY + tickH,
                AnimationHelper.withAlpha(tickColor, isCardinal ? 255 : 180));

            String label;
            int labelColor;
            if (deg == 0) { label = "N"; labelColor = UITheme.ACCENT; }
            else if (deg == 90) { label = "E"; labelColor = UITheme.TEXT_PRIMARY; }
            else if (deg == 180) { label = "S"; labelColor = UITheme.COMPASS_TEXT; }
            else if (deg == 270) { label = "W"; labelColor = UITheme.TEXT_PRIMARY; }
            else { label = String.valueOf(deg); labelColor = UITheme.COMPASS_TEXT; }

            int lw = font.width(label);
            g.drawString(font, label, tickX - lw / 2, tickY + tickH + 1,
                AnimationHelper.withAlpha(labelColor, isCardinal ? 255 : 160));
        }
    }
}
