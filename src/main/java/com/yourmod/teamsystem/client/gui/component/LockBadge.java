package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class LockBadge {

    private static final int OVERLAY_COLOR = 0xCC0A0A0A;
    private static final String LOCK_ICON  = "\uD83D\uDD12";

    public static void draw(GuiGraphics g, int x, int y, int w, int h, String reason) {
        g.fill(x, y, x + w, y + h, OVERLAY_COLOR);

        var font = Minecraft.getInstance().font;
        int midX = x + w / 2;
        int midY = y + h / 2;

        int iconW = font.width(LOCK_ICON);
        g.drawString(font, LOCK_ICON,
            midX - iconW / 2, midY - font.lineHeight - 2,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, 0xCC));

        if (reason != null && !reason.isEmpty()) {
            int tw = font.width(reason);
            g.drawString(font, reason,
                midX - tw / 2, midY + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, 0xAA));
        }
    }
}
